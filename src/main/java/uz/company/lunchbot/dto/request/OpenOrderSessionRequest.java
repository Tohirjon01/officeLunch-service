package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record OpenOrderSessionRequest(
        Long restaurantId,
        LocalDate orderDate,
        LocalTime deadlineTime,
        @DecimalMin(value = "0.00") BigDecimal deliveryPrice,
        @DecimalMin(value = "0.00") BigDecimal containerPrice
) {
}
