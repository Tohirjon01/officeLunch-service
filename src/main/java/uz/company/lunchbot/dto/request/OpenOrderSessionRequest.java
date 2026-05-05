package uz.company.lunchbot.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

public record OpenOrderSessionRequest(
        Long restaurantId,
        LocalDate orderDate,
        LocalTime deadlineTime
) {
}
