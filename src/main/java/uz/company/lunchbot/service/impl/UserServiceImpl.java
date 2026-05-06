package uz.company.lunchbot.service.impl;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.company.lunchbot.dto.request.TelegramRegistrationRequest;
import uz.company.lunchbot.dto.response.RegistrationResultResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.enums.AuditAction;
import uz.company.lunchbot.enums.UserLanguage;
import uz.company.lunchbot.enums.UserRole;
import uz.company.lunchbot.enums.UserStatus;
import uz.company.lunchbot.exception.ForbiddenException;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.repository.LunchUserRepository;
import uz.company.lunchbot.security.AdminAccessService;
import uz.company.lunchbot.service.AuditService;
import uz.company.lunchbot.service.UserService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final LunchUserRepository lunchUserRepository;
    private final AdminAccessService adminAccessService;
    private final AuditService auditService;

    @Override
    public List<LunchUser> getAll() {
        return lunchUserRepository.findAll();
    }

    @Override
    public List<LunchUser> getPendingUsers() {
        return lunchUserRepository.findAllByStatusOrderByCreatedAtAsc(UserStatus.PENDING);
    }

    @Override
    public List<LunchUser> getApprovedUsers() {
        return lunchUserRepository.findAllByStatus(UserStatus.APPROVED);
    }

    @Override
    public List<LunchUser> getApprovedAdmins() {
        return lunchUserRepository.findAllByStatusAndRoleIn(UserStatus.APPROVED, List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN));
    }

    @Override
    public LunchUser getRequired(Long id) {
        return lunchUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    @Override
    public LunchUser getRequiredByTelegramUserId(Long telegramUserId) {
        return lunchUserRepository.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new NotFoundException("Telegram user not registered: " + telegramUserId));
    }

    @Override
    public Optional<LunchUser> findByTelegramUserId(Long telegramUserId) {
        return lunchUserRepository.findByTelegramUserId(telegramUserId);
    }

    @Override
    public Optional<LunchUser> findByPrivateChatId(Long privateChatId) {
        return lunchUserRepository.findByPrivateChatId(privateChatId);
    }

    @Override
    public LunchUser getApprovedUserByTelegramUserId(Long telegramUserId) {
        LunchUser user = getRequiredByTelegramUserId(telegramUserId);
        if (user.getStatus() != UserStatus.APPROVED) {
            throw new ForbiddenException("User is not approved for ordering");
        }
        return user;
    }

    @Override
    @Transactional
    public RegistrationResultResponse registerTelegramUser(TelegramRegistrationRequest request) {
        return lunchUserRepository.findByTelegramUserId(request.telegramUserId())
                .map(existing -> new RegistrationResultResponse(updateTelegramProfile(existing, request), false))
                .orElseGet(() -> new RegistrationResultResponse(createPendingUser(request), true));
    }

    @Override
    @Transactional
    public LunchUser completeTelegramRegistration(TelegramRegistrationRequest request, UserLanguage language) {
        LunchUser user = lunchUserRepository.findByTelegramUserId(request.telegramUserId()).orElseGet(LunchUser::new);
        boolean isNew = user.getId() == null;
        user.setTelegramUserId(request.telegramUserId());
        user.setUsername(blankToNull(request.username()));
        user.setFirstName(blankToNull(request.firstName()));
        user.setLastName(blankToNull(request.lastName()));
        user.setPrivateChatId(request.privateChatId());
        user.setPhoneNumber(normalizePhoneNumber(request.phoneNumber()));
        user.setLanguage(language == null ? UserLanguage.UZ : language);
        user.setBotBlocked(false);
        if (user.getRole() == null) {
            user.setRole(UserRole.USER);
        }
        if (user.getStatus() == null || user.getStatus() == UserStatus.REJECTED || user.getStatus() == UserStatus.BLOCKED) {
            user.setStatus(UserStatus.PENDING);
        }
        LunchUser saved = lunchUserRepository.save(user);
        if (isNew) {
            auditService.log(AuditAction.USER_REGISTERED, null, saved.getId(), null, saved.getTelegramUserId());
            log.info("user_registered userId={} telegramUserId={}", saved.getId(), saved.getTelegramUserId());
        }
        return saved;
    }

    @Override
    @Transactional
    public LunchUser updateLanguage(Long telegramUserId, UserLanguage language) {
        LunchUser user = getRequiredByTelegramUserId(telegramUserId);
        user.setLanguage(language == null ? UserLanguage.UZ : language);
        return lunchUserRepository.save(user);
    }

    @Override
    @Transactional
    public LunchUser approve(Long userId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        LunchUser user = getRequired(userId);
        UserStatus oldStatus = user.getStatus();
        user.setStatus(UserStatus.APPROVED);
        LunchUser saved = lunchUserRepository.save(user);
        auditService.log(AuditAction.USER_APPROVED, null, userId, oldStatus, saved.getStatus());
        log.info("user_approved userId={} actorUserId={}", userId, actorUserId);
        return saved;
    }

    @Override
    @Transactional
    public LunchUser reject(Long userId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        LunchUser user = getRequired(userId);
        UserStatus oldStatus = user.getStatus();
        user.setStatus(UserStatus.REJECTED);
        LunchUser saved = lunchUserRepository.save(user);
        auditService.log(AuditAction.USER_REJECTED, null, userId, oldStatus, saved.getStatus());
        return saved;
    }

    @Override
    @Transactional
    public LunchUser block(Long userId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        LunchUser user = getRequired(userId);
        UserStatus oldStatus = user.getStatus();
        user.setStatus(UserStatus.BLOCKED);
        LunchUser saved = lunchUserRepository.save(user);
        auditService.log(AuditAction.USER_BLOCKED, null, userId, oldStatus, saved.getStatus());
        return saved;
    }

    @Override
    @Transactional
    public LunchUser changeRole(Long userId, UserRole role, Long actorUserId) {
        adminAccessService.ensureSuperAdmin(actorUserId);
        LunchUser user = getRequired(userId);
        UserRole oldRole = user.getRole();
        user.setRole(role);
        LunchUser saved = lunchUserRepository.save(user);
        auditService.log(AuditAction.USER_ROLE_CHANGED, null, userId, oldRole, saved.getRole());
        return saved;
    }

    @Override
    @Transactional
    public LunchUser upsertSuperAdmin(Long telegramUserId, String username, String firstName, String lastName, Long privateChatId) {
        LunchUser user = lunchUserRepository.findByTelegramUserId(telegramUserId).orElseGet(LunchUser::new);
        user.setTelegramUserId(telegramUserId);
        user.setUsername(blankToNull(username));
        user.setFirstName(blankToNull(firstName));
        user.setLastName(blankToNull(lastName));
        user.setPrivateChatId(privateChatId != null && privateChatId > 0 ? privateChatId : null);
        user.setLanguage(user.getLanguage() == null ? UserLanguage.UZ : user.getLanguage());
        user.setStatus(UserStatus.APPROVED);
        user.setRole(UserRole.SUPER_ADMIN);
        return lunchUserRepository.save(user);
    }

    @Override
    @Transactional
    public void markBotBlocked(Long privateChatId) {
        if (privateChatId == null) {
            return;
        }

        lunchUserRepository.findByPrivateChatId(privateChatId).ifPresent(user -> {
            if (!user.isBotBlocked()) {
                user.setBotBlocked(true);
                lunchUserRepository.save(user);
                log.warn("telegram_user_marked_blocked userId={} privateChatId={}", user.getId(), privateChatId);
            }
        });
    }

    private LunchUser updateTelegramProfile(LunchUser user, TelegramRegistrationRequest request) {
        user.setUsername(blankToNull(request.username()));
        user.setFirstName(blankToNull(request.firstName()));
        user.setLastName(blankToNull(request.lastName()));
        user.setPrivateChatId(request.privateChatId());
        if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
            user.setPhoneNumber(normalizePhoneNumber(request.phoneNumber()));
        }
        return lunchUserRepository.save(user);
    }

    private LunchUser createPendingUser(TelegramRegistrationRequest request) {
        LunchUser user = new LunchUser();
        user.setTelegramUserId(request.telegramUserId());
        user.setUsername(blankToNull(request.username()));
        user.setFirstName(blankToNull(request.firstName()));
        user.setLastName(blankToNull(request.lastName()));
        user.setPrivateChatId(request.privateChatId());
        user.setPhoneNumber(normalizePhoneNumber(request.phoneNumber()));
        user.setLanguage(UserLanguage.UZ);
        user.setStatus(UserStatus.PENDING);
        user.setRole(UserRole.USER);
        LunchUser saved = lunchUserRepository.save(user);
        auditService.log(AuditAction.USER_REGISTERED, null, saved.getId(), null, saved.getTelegramUserId());
        log.info("user_registered userId={} telegramUserId={}", saved.getId(), saved.getTelegramUserId());
        return saved;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizePhoneNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new ForbiddenException("Phone number is required");
        }
        return value.trim();
    }
}
