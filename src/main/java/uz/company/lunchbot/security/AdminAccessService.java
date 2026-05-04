package uz.company.lunchbot.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.enums.UserRole;
import uz.company.lunchbot.enums.UserStatus;
import uz.company.lunchbot.exception.ForbiddenException;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.repository.LunchUserRepository;

@Service
@RequiredArgsConstructor
public class AdminAccessService {

    private final LunchUserRepository lunchUserRepository;

    public LunchUser getActor(Long actorUserId) {
        if (actorUserId == null) {
            return null;
        }
        return lunchUserRepository.findById(actorUserId)
                .orElseThrow(() -> new NotFoundException("Actor user not found: " + actorUserId));
    }

    public void ensureAdmin(Long actorUserId) {
        if (actorUserId == null) {
            return;
        }
        LunchUser actor = getActor(actorUserId);
        if (actor.getStatus() != UserStatus.APPROVED) {
            throw new ForbiddenException("Only approved admins can perform this action");
        }
        if (actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.SUPER_ADMIN) {
            throw new ForbiddenException("Admin role is required for this action");
        }
    }

    public void ensureSuperAdmin(Long actorUserId) {
        if (actorUserId == null) {
            return;
        }
        LunchUser actor = getActor(actorUserId);
        if (actor.getStatus() != UserStatus.APPROVED || actor.getRole() != UserRole.SUPER_ADMIN) {
            throw new ForbiddenException("Super admin role is required for this action");
        }
    }
}
