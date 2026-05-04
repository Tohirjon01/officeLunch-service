package uz.company.lunchbot.mapper;

import org.springframework.stereotype.Component;
import uz.company.lunchbot.dto.response.UserOrderResponse;
import uz.company.lunchbot.entity.UserOrder;

@Component
public class UserOrderMapper {

    public UserOrderResponse toResponse(UserOrder order) {
        return new UserOrderResponse(
                order.getId(),
                order.getOrderSession().getId(),
                order.getUser().getId(),
                order.getUser().getDisplayName(),
                order.getMenuItem() == null ? null : order.getMenuItem().getId(),
                order.getMenuItem() == null ? null : order.getMenuItem().getName(),
                order.getStatus(),
                order.getQuantity(),
                order.getFoodPrice(),
                order.getContainerPrice(),
                order.getDeliveryShare(),
                order.getFinalPrice(),
                order.getPaymentStatus(),
                order.getOrderedAt(),
                order.getUpdatedAt());
    }
}
