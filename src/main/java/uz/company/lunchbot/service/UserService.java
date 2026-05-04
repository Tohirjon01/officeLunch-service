package uz.company.lunchbot.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.company.lunchbot.dto.request.TelegramRegistrationRequest;
import uz.company.lunchbot.dto.response.RegistrationResultResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.enums.AuditAction;
import uz.company.lunchbot.enums.UserRole;
import uz.company.lunchbot.enums.UserStatus;
import uz.company.lunchbot.exception.ForbiddenException;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.repository.LunchUserRepository;
import uz.company.lunchbot.security.AdminAccessService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final LunchUserRepository lunchUserRepository;
    private final AdminAccessService adminAccessService;
    private final AuditService auditService;

    public List<LunchUser> getAll() {
        return lunchUserRepository.findAll();
    }

    public List<LunchUser> getPendingUsers() {
        return lunchUserRepository.findAllByStatusOrderByCreatedAtAsc(UserStatus.PENDING);
    }

    public List<LunchUser> getApprovedUsers() {
        return lunchUserRepository.findAllByStatus(UserStatus.APPROVED);
    }

    public List<LunchUser> getApprovedAdmins() {
        return lunchUserRepository.findAllByStatusAndRoleIn(UserStatus.APPROVED, List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN));
    }

    public LunchUser getRequired(Long id) {
        return lunchUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    public LunchUser getRequiredByTelegramUserId(Long telegramUserId) {
        return lunchUserRepository.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new NotFoundException("Telegram user not registered: " + telegramUserId));
    }

    public LunchUser getApprovedUserByTelegramUserId(Long telegramUserId) {
        LunchUser user = getRequiredByTelegramUserId(telegramUserId);
        if (user.getStatus() != UserStatus.APPROVED) {
            throw new ForbiddenException("User is not approved for ordering");
        }
        return user;
    }

    @Transactional
    public RegistrationResultResponse registerTelegramUser(TelegramRegistrationRequest request) {
        return lunchUserRepository.findByTelegramUserId(request.telegramUserId())
                .map(existing -> new RegistrationResultResponse(updateTelegramProfile(existing, request), false))
                .orElseGet(() -> new RegistrationResultResponse(createPendingUser(request), true));
    }

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

    @Transactional
    public LunchUser upsertSuperAdmin(Long telegramUserId, String username, String firstName, String lastName, Long privateChatId) {
        LunchUser user = lunchUserRepository.findByTelegramUserId(telegramUserId).orElseGet(LunchUser::new);
        user.setTelegramUserId(telegramUserId);
        user.setUsername(blankToNull(username));
        user.setFirstName(blankToNull(firstName));
        user.setLastName(blankToNull(lastName));
        user.setPrivateChatId(privateChatId != null && privateChatId > 0 ? privateChatId : null);
        user.setStatus(UserStatus.APPROVED);
        user.setRole(UserRole.SUPER_ADMIN);
        return lunchUserRepository.save(user);
    }

    private LunchUser updateTelegramProfile(LunchUser user, TelegramRegistrationRequest request) {
        user.setUsername(blankToNull(request.username()));
        user.setFirstName(blankToNull(request.firstName()));
        user.setLastName(blankToNull(request.lastName()));
        user.setPrivateChatId(request.privateChatId());
        return lunchUserRepository.save(user);
    }

    private LunchUser createPendingUser(TelegramRegistrationRequest request) {
        LunchUser user = new LunchUser();
        user.setTelegramUserId(request.telegramUserId());
        user.setUsername(blankToNull(request.username()));
        user.setFirstName(blankToNull(request.firstName()));
        user.setLastName(blankToNull(request.lastName()));
        user.setPrivateChatId(request.privateChatId());
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
}
