package uz.company.lunchbot.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.company.lunchbot.entity.RestaurantVote;

public interface RestaurantVoteRepository extends JpaRepository<RestaurantVote, Long> {

    Optional<RestaurantVote> findByVoteSessionIdAndUserId(Long voteSessionId, Long userId);

    List<RestaurantVote> findAllByVoteSessionId(Long voteSessionId);
}
