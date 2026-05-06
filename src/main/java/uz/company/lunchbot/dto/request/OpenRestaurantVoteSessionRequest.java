package uz.company.lunchbot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;

public record OpenRestaurantVoteSessionRequest(
        LocalDate voteDate,
        @Schema(type = "string", example = "10:00:00")
        LocalTime deadlineTime
) {
}
