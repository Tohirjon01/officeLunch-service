package uz.company.lunchbot.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;
import io.swagger.v3.oas.annotations.media.Schema;

public record OpenOrderSessionRequest(
        Long restaurantId,
        LocalDate orderDate,
        @Schema(type = "string", example = "11:30:00")
        LocalTime deadlineTime
) {
}
