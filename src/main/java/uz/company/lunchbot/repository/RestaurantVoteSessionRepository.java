package uz.company.lunchbot.repository;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.company.lunchbot.entity.RestaurantVoteSession;
import uz.company.lunchbot.enums.RestaurantVoteSessionStatus;

public interface RestaurantVoteSessionRepository extends JpaRepository<RestaurantVoteSession, Long> {

    Optional<RestaurantVoteSession> findFirstByVoteDateOrderByCreatedAtDesc(LocalDate voteDate);

    Optional<RestaurantVoteSession> findFirstByVoteDateAndStatusOrderByCreatedAtDesc(LocalDate voteDate, RestaurantVoteSessionStatus status);
}
