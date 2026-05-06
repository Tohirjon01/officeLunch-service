package uz.company.lunchbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uz.company.lunchbot.dto.request.TelegramRegistrationRequest;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.enums.UserLanguage;
import uz.company.lunchbot.enums.UserRole;
import uz.company.lunchbot.enums.UserStatus;
import uz.company.lunchbot.repository.LunchUserRepository;
import uz.company.lunchbot.security.AdminAccessService;
import uz.company.lunchbot.service.impl.UserServiceImpl;

class UserServiceRegistrationTest {

    @Test
    void shouldCompleteTelegramRegistrationAsPendingUser() {
        LunchUserRepository lunchUserRepository = mock(LunchUserRepository.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        UserServiceImpl service = new UserServiceImpl(lunchUserRepository, adminAccessService, auditService);

        when(lunchUserRepository.findByTelegramUserId(1001L)).thenReturn(java.util.Optional.empty());
        when(lunchUserRepository.save(any(LunchUser.class))).thenAnswer(invocation -> {
            LunchUser user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });

        LunchUser user = service.completeTelegramRegistration(new TelegramRegistrationRequest(
                1001L,
                "user_name",
                "Ali",
                "Valiyev",
                2002L,
                "+998901112233"
        ), UserLanguage.RU);

        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getPhoneNumber()).isEqualTo("+998901112233");
        assertThat(user.getLanguage()).isEqualTo(UserLanguage.RU);
    }
}
