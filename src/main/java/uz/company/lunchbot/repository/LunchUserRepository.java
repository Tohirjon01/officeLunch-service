package uz.company.lunchbot.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.enums.UserRole;
import uz.company.lunchbot.enums.UserStatus;

public interface LunchUserRepository extends JpaRepository<LunchUser, Long> {

    Optional<LunchUser> findByTelegramUserId(Long telegramUserId);

    Optional<LunchUser> findByPrivateChatId(Long privateChatId);

    List<LunchUser> findAllByStatusOrderByCreatedAtAsc(UserStatus status);

    List<LunchUser> findAllByStatus(UserStatus status);

    List<LunchUser> findAllByStatusAndRoleIn(UserStatus status, Collection<UserRole> roles);
}
