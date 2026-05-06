package uz.company.lunchbot.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import uz.company.lunchbot.enums.RestaurantVoteSessionStatus;

public record RestaurantVoteSessionResponse(
        Long id,
        LocalDate voteDate,
        RestaurantVoteSessionStatus status,
        LocalDateTime startedAt,
        LocalDateTime deadlineAt,
        Long winnerRestaurantId,
        String winnerRestaurantName,
        Long groupMessageId,
        List<RestaurantVoteCountResponse> voteCounts,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
