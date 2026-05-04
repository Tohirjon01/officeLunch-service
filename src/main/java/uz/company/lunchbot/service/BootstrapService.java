package uz.company.lunchbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.enums.AuditAction;

@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapService {

    private final LunchProperties lunchProperties;
    private final UserService userService;
    private final AuditService auditService;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        Long telegramUserId = lunchProperties.bootstrap().superAdminTelegramUserId();
        if (telegramUserId == null || telegramUserId <= 0) {
            return;
        }

        var user = userService.upsertSuperAdmin(
                telegramUserId,
                lunchProperties.bootstrap().superAdminUsername(),
                lunchProperties.bootstrap().superAdminFirstName(),
                lunchProperties.bootstrap().superAdminLastName(),
                lunchProperties.bootstrap().superAdminPrivateChatId());

        auditService.log(AuditAction.USER_ROLE_CHANGED, null, user.getId(), null, "BOOTSTRAP_SUPER_ADMIN");
        log.info("bootstrap_super_admin_ready userId={} telegramUserId={}", user.getId(), user.getTelegramUserId());
    }
}
