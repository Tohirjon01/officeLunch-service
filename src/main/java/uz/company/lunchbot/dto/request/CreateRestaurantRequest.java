package uz.company.lunchbot.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record CreateRestaurantRequest(
        @NotBlank String name,
        String description,
        @JsonAlias("phone") String phoneNumber,
        String address,
        Boolean defaultRestaurant,
        Boolean active,
        Boolean containerEnabled,
        @DecimalMin(value = "0.00") BigDecimal defaultContainerPrice,
        Boolean deliveryEnabled,
        @DecimalMin(value = "0.00") BigDecimal defaultDeliveryPrice
) {
}
