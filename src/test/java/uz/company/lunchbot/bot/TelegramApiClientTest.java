package uz.company.lunchbot.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uz.company.lunchbot.config.TelegramBotProperties;
import uz.company.lunchbot.exception.TelegramMessageException;
import uz.company.lunchbot.service.UserService;

class TelegramApiClientTest {

    @Test
    void shouldIgnoreBlockedUserForbiddenResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.telegram.org");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        UserService userService = mock(UserService.class);
        TelegramApiClient client = new TelegramApiClient(new TelegramBotProperties(true, "123:token", "bot"), builder.build(), userService);

        Logger logger = (Logger) LoggerFactory.getLogger(TelegramApiClient.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        server.expect(requestTo("https://api.telegram.org/bot123%3Atoken/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":false,\"error_code\":403,\"description\":\"Forbidden: bot was blocked by the user\"}"));

        assertThatCode(() -> client.sendMessage(10L, "hello", null)).doesNotThrowAnyException();
        server.verify();

        assertThat(appender.list)
                .anyMatch(event -> event.getLevel() == Level.WARN && event.getFormattedMessage().contains("telegram_send_blocked"));
        assertThat(appender.list)
                .noneMatch(event -> event.getLevel() == Level.ERROR);
        verify(userService).markBotBlocked(10L);

        logger.detachAppender(appender);
    }

    @Test
    void shouldStillThrowForOtherTelegramFailures() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.telegram.org");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramApiClient client = new TelegramApiClient(new TelegramBotProperties(true, "123:token", "bot"), builder.build());

        server.expect(requestTo("https://api.telegram.org/bot123%3Atoken/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":false,\"error_code\":403,\"description\":\"Forbidden: user is deactivated\"}"));

        assertThatThrownBy(() -> client.sendMessage(10L, "hello", null))
                .isInstanceOf(TelegramMessageException.class);

        server.verify();
    }

    @Test
    void shouldReturnFalseWhenPhotoSendFails() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.telegram.org");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramApiClient client = new TelegramApiClient(new TelegramBotProperties(true, "123:token", "bot"), builder.build());

        server.expect(requestTo("https://api.telegram.org/bot123%3Atoken/sendPhoto"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":false,\"error_code\":400,\"description\":\"Bad Request: wrong file identifier/http url specified\"}"));

        assertThat(client.sendPhoto(10L, "https://example.com/food.jpg", "caption", null)).isFalse();

        server.verify();
    }

    @Test
    void shouldIgnoreExpiredCallbackQueryResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.telegram.org");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramApiClient client = new TelegramApiClient(new TelegramBotProperties(true, "123:token", "bot"), builder.build());

        server.expect(requestTo("https://api.telegram.org/bot123%3Atoken/answerCallbackQuery"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":false,\"error_code\":400,\"description\":\"Bad Request: query is too old and response timeout expired or query ID is invalid\"}"));

        assertThatCode(() -> client.answerCallbackQuery("cb-1", "saved")).doesNotThrowAnyException();

        server.verify();
    }
}
