package uz.company.lunchbot.mapper;

import org.springframework.stereotype.Component;
import uz.company.lunchbot.dto.response.OrderSessionResponse;
import uz.company.lunchbot.entity.OrderSession;

@Component
public class OrderSessionMapper {

    public OrderSessionResponse toResponse(OrderSession session) {
        return new OrderSessionResponse(
                session.getId(),
                session.getRestaurant().getId(),
                session.getRestaurant().getName(),
                session.getOrderDate(),
                session.getStatus(),
                session.getDeliveryPrice(),
                session.getContainerPrice(),
                session.getOpenedAt(),
                session.getDeadlineAt(),
                session.getClosedAt(),
                session.getConfirmedAt(),
                session.getCreatedBy() == null ? null : session.getCreatedBy().getId(),
                session.getCreatedBy() == null ? null : session.getCreatedBy().getDisplayName(),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }
}
