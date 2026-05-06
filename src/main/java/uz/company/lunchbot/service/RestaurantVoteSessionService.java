package uz.company.lunchbot.service;

import java.util.Optional;
import uz.company.lunchbot.dto.request.OpenRestaurantVoteSessionRequest;
import uz.company.lunchbot.dto.response.RestaurantVoteSessionResponse;
import uz.company.lunchbot.entity.RestaurantVoteSession;

public interface RestaurantVoteSessionService {

    RestaurantVoteSession getRequired(Long id);

    Optional<RestaurantVoteSession> getTodaySession();

    RestaurantVoteSessionResponse getTodayResponse(Long actorUserId);

    RestaurantVoteSessionResponse openSession(OpenRestaurantVoteSessionRequest request, Long actorUserId);

    RestaurantVoteSessionResponse closeSession(Long id, Long actorUserId);

    RestaurantVoteSessionResponse chooseWinner(Long id, Long restaurantId, Long actorUserId);

    RestaurantVoteSessionResponse extendDeadline(Long id, int minutes, Long actorUserId);

    RestaurantVoteSessionResponse reduceDeadline(Long id, int minutes, Long actorUserId);

    void castVote(Long telegramUserId, Long voteSessionId, Long restaurantId);
}
