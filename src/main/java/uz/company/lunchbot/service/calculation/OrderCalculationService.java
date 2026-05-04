package uz.company.lunchbot.service.calculation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.RoundingStrategy;
import uz.company.lunchbot.enums.UserOrderStatus;

@Service
public class OrderCalculationService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal FIVE_HUNDRED = BigDecimal.valueOf(500);
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    private final LunchProperties lunchProperties;

    public OrderCalculationService(LunchProperties lunchProperties) {
        this.lunchProperties = lunchProperties;
    }

    public void recalculate(OrderSession session, List<UserOrder> orders) {
        List<UserOrder> orderedUsers = orders.stream()
                .filter(order -> order.getStatus() == UserOrderStatus.ORDERED)
                .toList();

        BigDecimal deliveryShare = orderedUsers.isEmpty()
                ? BigDecimal.ZERO
                : session.getDeliveryPrice().divide(BigDecimal.valueOf(orderedUsers.size()), 2, RoundingMode.HALF_UP);

        for (UserOrder order : orders) {
            if (order.getStatus() != UserOrderStatus.ORDERED) {
                order.setContainerPrice(BigDecimal.ZERO);
                order.setDeliveryShare(BigDecimal.ZERO);
                order.setFinalPrice(BigDecimal.ZERO);
                continue;
            }

            BigDecimal quantity = BigDecimal.valueOf(order.getQuantity());
            BigDecimal base = order.getFoodPrice().multiply(quantity)
                    .add(session.getContainerPrice().multiply(quantity))
                    .add(deliveryShare);

            order.setContainerPrice(session.getContainerPrice());
            order.setDeliveryShare(deliveryShare);
            order.setFinalPrice(applyRounding(base));
        }
    }

    private BigDecimal applyRounding(BigDecimal value) {
        RoundingStrategy strategy = lunchProperties.roundingStrategy();
        return switch (strategy) {
            case NONE -> value;
            case HALF_UP -> value.setScale(0, RoundingMode.HALF_UP);
            case CEIL_TO_100 -> ceilToStep(value, HUNDRED);
            case CEIL_TO_500 -> ceilToStep(value, FIVE_HUNDRED);
            case CEIL_TO_1000 -> ceilToStep(value, THOUSAND);
        };
    }

    private BigDecimal ceilToStep(BigDecimal value, BigDecimal step) {
        if (value.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(step, 0, RoundingMode.CEILING).multiply(step);
    }
}
