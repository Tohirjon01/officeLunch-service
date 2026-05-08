package uz.company.lunchbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uz.company.lunchbot.bot.keyboard.TelegramKeyboards;
import uz.company.lunchbot.bot.message.TelegramMessages;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.config.TelegramBotProperties;
import uz.company.lunchbot.dto.response.RestaurantVoteSessionResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.entity.RestaurantVote;
import uz.company.lunchbot.entity.RestaurantVoteSession;
import uz.company.lunchbot.enums.OrderSessionStatus;
import uz.company.lunchbot.enums.RestaurantVoteSessionStatus;
import uz.company.lunchbot.repository.RestaurantRepository;
import uz.company.lunchbot.repository.RestaurantVoteRepository;
import uz.company.lunchbot.repository.RestaurantVoteSessionRepository;
import uz.company.lunchbot.security.AdminAccessService;
import uz.company.lunchbot.service.impl.RestaurantVoteSessionServiceImpl;

class RestaurantVoteSessionServiceTest {

    @Test
    void shouldOpenRestaurantVoteSession() {
        RestaurantVoteSessionRepository sessionRepository = mock(RestaurantVoteSessionRepository.class);
        RestaurantVoteRepository voteRepository = mock(RestaurantVoteRepository.class);
        RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        UserService userService = mock(UserService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        NotificationService notificationService = mock(NotificationService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        RestaurantVoteSessionServiceImpl service = new RestaurantVoteSessionServiceImpl(
                sessionRepository,
                voteRepository,
                restaurantRepository,
                restaurantService,
                userService,
                orderSessionService,
                menuItemService,
                notificationService,
                new TelegramMessages(),
                new TelegramKeyboards(),
                adminAccessService,
                auditService,
                properties(),
                telegramBotProperties(),
                clock()
        );

        Restaurant osh = restaurant(1L, "Osh Posh");
        Restaurant tarnov = restaurant(2L, "Tarnov");
        when(restaurantRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(osh, tarnov));
        when(sessionRepository.findFirstByVoteDateAndStatusOrderByCreatedAtDesc(LocalDate.of(2026, 5, 5), RestaurantVoteSessionStatus.OPEN))
                .thenReturn(Optional.empty());
        when(sessionRepository.saveAndFlush(any(RestaurantVoteSession.class))).thenAnswer(invocation -> {
            RestaurantVoteSession session = invocation.getArgument(0);
            ReflectionTestUtils.setField(session, "id", 10L);
            return session;
        });
        when(sessionRepository.save(any(RestaurantVoteSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationService.sendGroupText(any(String.class), any())).thenReturn(99L);
        when(voteRepository.findAllByVoteSessionId(10L)).thenReturn(List.of());

        RestaurantVoteSessionResponse response = service.openSession(null, 7L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.groupMessageId()).isEqualTo(99L);
        assertThat(response.voteCounts()).hasSize(2);
    }

    @Test
    void shouldUpdateExistingVoteWhenUserVotesAgain() {
        RestaurantVoteSessionRepository sessionRepository = mock(RestaurantVoteSessionRepository.class);
        RestaurantVoteRepository voteRepository = mock(RestaurantVoteRepository.class);
        RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        UserService userService = mock(UserService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        NotificationService notificationService = mock(NotificationService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        RestaurantVoteSessionServiceImpl service = new RestaurantVoteSessionServiceImpl(
                sessionRepository,
                voteRepository,
                restaurantRepository,
                restaurantService,
                userService,
                orderSessionService,
                menuItemService,
                notificationService,
                new TelegramMessages(),
                new TelegramKeyboards(),
                adminAccessService,
                auditService,
                properties(),
                telegramBotProperties(),
                clock()
        );

        LunchUser user = new LunchUser();
        user.setId(11L);
        user.setTelegramUserId(1001L);
        when(userService.getApprovedUserByTelegramUserId(1001L)).thenReturn(user);

        RestaurantVoteSession session = new RestaurantVoteSession();
        session.setId(10L);
        session.setStatus(RestaurantVoteSessionStatus.OPEN);
        session.setDeadlineAt(LocalDateTime.of(2026, 5, 5, 10, 0));
        session.setGroupMessageId(88L);
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));

        Restaurant osh = restaurant(1L, "Osh Posh");
        Restaurant tarnov = restaurant(2L, "Tarnov");
        when(restaurantService.getRequired(1L)).thenReturn(osh);
        when(restaurantService.getRequired(2L)).thenReturn(tarnov);
        when(restaurantRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(osh, tarnov));

        AtomicReference<RestaurantVote> storedVote = new AtomicReference<>();
        when(voteRepository.findByVoteSessionIdAndUserId(10L, 11L)).thenAnswer(invocation -> Optional.ofNullable(storedVote.get()));
        when(voteRepository.save(any(RestaurantVote.class))).thenAnswer(invocation -> {
            RestaurantVote vote = invocation.getArgument(0);
            ReflectionTestUtils.setField(vote, "id", 1L);
            storedVote.set(vote);
            return vote;
        });
        when(voteRepository.findAllByVoteSessionId(10L)).thenAnswer(invocation -> storedVote.get() == null ? List.of() : List.of(storedVote.get()));

        service.castVote(1001L, 10L, 1L);
        service.castVote(1001L, 10L, 2L);

        assertThat(storedVote.get().getRestaurant().getId()).isEqualTo(2L);
        verify(notificationService, org.mockito.Mockito.times(2)).editGroupText(anyLong(), any(String.class), any());
    }

    @Test
    void shouldOpenOrderSessionForWinnerRestaurant() {
        RestaurantVoteSessionRepository sessionRepository = mock(RestaurantVoteSessionRepository.class);
        RestaurantVoteRepository voteRepository = mock(RestaurantVoteRepository.class);
        RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        UserService userService = mock(UserService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        NotificationService notificationService = mock(NotificationService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        RestaurantVoteSessionServiceImpl service = new RestaurantVoteSessionServiceImpl(
                sessionRepository,
                voteRepository,
                restaurantRepository,
                restaurantService,
                userService,
                orderSessionService,
                menuItemService,
                notificationService,
                new TelegramMessages(),
                new TelegramKeyboards(),
                adminAccessService,
                auditService,
                properties(),
                telegramBotProperties(),
                clock()
        );

        RestaurantVoteSession session = new RestaurantVoteSession();
        session.setId(10L);
        session.setVoteDate(LocalDate.of(2026, 5, 5));
        session.setStatus(RestaurantVoteSessionStatus.OPEN);
        session.setDeadlineAt(LocalDateTime.of(2026, 5, 5, 10, 0));
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(RestaurantVoteSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Restaurant osh = restaurant(1L, "Osh Posh");
        Restaurant tarnov = restaurant(2L, "Tarnov");
        when(restaurantRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(osh, tarnov));
        when(restaurantService.getRequired(1L)).thenReturn(osh);

        when(voteRepository.findAllByVoteSessionId(10L)).thenReturn(List.of(
                vote(session, 101L, osh),
                vote(session, 102L, osh),
                vote(session, 103L, tarnov)
        ));

        OrderSession orderSession = new OrderSession();
        orderSession.setId(44L);
        orderSession.setRestaurant(osh);
        orderSession.setStatus(OrderSessionStatus.OPEN);
        orderSession.setDeadlineAt(LocalDateTime.of(2026, 5, 5, 11, 30));
        when(orderSessionService.getTodayOpenSession()).thenReturn(Optional.empty());
        when(orderSessionService.openTodaySessionForRestaurantIfAbsent(1L, 7L)).thenReturn(orderSession);
        when(menuItemService.getActiveMenu(1L)).thenReturn(List.of(menuItem(1L, osh, "To'y oshi", "33000")));

        RestaurantVoteSessionResponse response = service.closeSession(10L, 7L);

        assertThat(response.winnerRestaurantId()).isEqualTo(1L);
        verify(orderSessionService).openTodaySessionForRestaurantIfAbsent(1L, 7L);
        verify(notificationService).sendGroupText(org.mockito.ArgumentMatchers.contains("G'olib restoran: Osh Posh"), any());
    }

    @Test
    void shouldDetectTieAndAllowManualWinnerSelection() {
        RestaurantVoteSessionRepository sessionRepository = mock(RestaurantVoteSessionRepository.class);
        RestaurantVoteRepository voteRepository = mock(RestaurantVoteRepository.class);
        RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        UserService userService = mock(UserService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        NotificationService notificationService = mock(NotificationService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        LunchUser admin = new LunchUser();
        admin.setPrivateChatId(700L);
        when(userService.getApprovedAdmins()).thenReturn(List.of(admin));

        RestaurantVoteSessionServiceImpl service = new RestaurantVoteSessionServiceImpl(
                sessionRepository,
                voteRepository,
                restaurantRepository,
                restaurantService,
                userService,
                orderSessionService,
                menuItemService,
                notificationService,
                new TelegramMessages(),
                new TelegramKeyboards(),
                adminAccessService,
                auditService,
                properties(),
                telegramBotProperties(),
                clock()
        );

        RestaurantVoteSession session = new RestaurantVoteSession();
        session.setId(20L);
        session.setVoteDate(LocalDate.of(2026, 5, 5));
        session.setStatus(RestaurantVoteSessionStatus.OPEN);
        session.setDeadlineAt(LocalDateTime.of(2026, 5, 5, 10, 0));
        when(sessionRepository.findById(20L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(RestaurantVoteSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Restaurant osh = restaurant(1L, "Osh Posh");
        Restaurant tarnov = restaurant(2L, "Tarnov");
        when(restaurantRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(osh, tarnov));
        when(restaurantService.getRequired(1L)).thenReturn(osh);
        when(restaurantService.getRequired(2L)).thenReturn(tarnov);

        when(voteRepository.findAllByVoteSessionId(20L)).thenReturn(List.of(
                vote(session, 101L, osh),
                vote(session, 102L, tarnov)
        ));

        RestaurantVoteSessionResponse tied = service.closeSession(20L, 7L);

        assertThat(tied.status()).isEqualTo(RestaurantVoteSessionStatus.TIE_WAITING_ADMIN);
        verify(orderSessionService, never()).openTodaySessionForRestaurantIfAbsent(anyLong(), any());

        OrderSession orderSession = new OrderSession();
        orderSession.setId(55L);
        orderSession.setRestaurant(tarnov);
        orderSession.setStatus(OrderSessionStatus.OPEN);
        orderSession.setDeadlineAt(LocalDateTime.of(2026, 5, 5, 11, 30));
        when(orderSessionService.getTodayOpenSession()).thenReturn(Optional.empty());
        when(orderSessionService.openTodaySessionForRestaurantIfAbsent(2L, 7L)).thenReturn(orderSession);
        when(menuItemService.getActiveMenu(2L)).thenReturn(List.of(menuItem(2L, tarnov, "Burger", "45000")));

        RestaurantVoteSessionResponse chosen = service.chooseWinner(20L, 2L, 7L);

        assertThat(chosen.status()).isEqualTo(RestaurantVoteSessionStatus.CLOSED);
        assertThat(chosen.winnerRestaurantId()).isEqualTo(2L);
        verify(orderSessionService).openTodaySessionForRestaurantIfAbsent(2L, 7L);
    }

    @Test
    void shouldNotDuplicateAnnouncementsForAlreadyClosedVoteSession() {
        RestaurantVoteSessionRepository sessionRepository = mock(RestaurantVoteSessionRepository.class);
        RestaurantVoteRepository voteRepository = mock(RestaurantVoteRepository.class);
        RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
        RestaurantService restaurantService = mock(RestaurantService.class);
        UserService userService = mock(UserService.class);
        OrderSessionService orderSessionService = mock(OrderSessionService.class);
        MenuItemService menuItemService = mock(MenuItemService.class);
        NotificationService notificationService = mock(NotificationService.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        RestaurantVoteSessionServiceImpl service = new RestaurantVoteSessionServiceImpl(
                sessionRepository,
                voteRepository,
                restaurantRepository,
                restaurantService,
                userService,
                orderSessionService,
                menuItemService,
                notificationService,
                new TelegramMessages(),
                new TelegramKeyboards(),
                adminAccessService,
                auditService,
                properties(),
                telegramBotProperties(),
                clock()
        );

        Restaurant osh = restaurant(1L, "Osh Posh");
        RestaurantVoteSession session = new RestaurantVoteSession();
        session.setId(30L);
        session.setVoteDate(LocalDate.of(2026, 5, 5));
        session.setStatus(RestaurantVoteSessionStatus.CLOSED);
        session.setWinnerRestaurant(osh);
        when(sessionRepository.findById(30L)).thenReturn(Optional.of(session));
        when(voteRepository.findAllByVoteSessionId(30L)).thenReturn(List.of());
        when(restaurantRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(osh));

        RestaurantVoteSessionResponse response = service.closeSession(30L, 7L);

        assertThat(response.winnerRestaurantId()).isEqualTo(1L);
        verify(notificationService, never()).sendGroupText(any(String.class), any());
    }

    private static RestaurantVote vote(RestaurantVoteSession session, Long userId, Restaurant restaurant) {
        RestaurantVote vote = new RestaurantVote();
        vote.setVoteSession(session);
        LunchUser user = new LunchUser();
        user.setId(userId);
        vote.setUser(user);
        vote.setRestaurant(restaurant);
        return vote;
    }

    private static MenuItem menuItem(Long id, Restaurant restaurant, String name, String price) {
        MenuItem item = new MenuItem();
        item.setId(id);
        item.setRestaurant(restaurant);
        item.setName(name);
        item.setPrice(new java.math.BigDecimal(price));
        return item;
    }

    private static Restaurant restaurant(Long id, String name) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(id);
        restaurant.setName(name);
        restaurant.setActive(true);
        return restaurant;
    }

    private static Clock clock() {
        return Clock.fixed(Instant.parse("2026-05-05T04:30:00Z"), ZoneId.of("UTC"));
    }

    private static LunchProperties properties() {
        return new LunchProperties(
                1L,
                "Asia/Tashkent",
                LocalTime.of(11, 30),
                uz.company.lunchbot.enums.RoundingStrategy.CEIL_TO_100,
                new LunchProperties.Scheduler(true, "", "", ""),
                new LunchProperties.RestaurantVoting(true, "", "", 5),
                new LunchProperties.Payment(true, "8600", "Owner", true),
                new LunchProperties.Bootstrap(0L, "", "", "", 0L)
        );
    }

    private static TelegramBotProperties telegramBotProperties() {
        return new TelegramBotProperties(true, "token", "your_bot_name");
    }
}
