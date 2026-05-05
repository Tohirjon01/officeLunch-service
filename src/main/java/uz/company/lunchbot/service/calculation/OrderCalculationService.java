package uz.company.lunchbot.service.calculation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.RecalculationMode;
import uz.company.lunchbot.enums.RoundingStrategy;
import uz.company.lunchbot.enums.UserOrderStatus;
import uz.company.lunchbot.exception.CalculationException;

@Service
public class OrderCalculationService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal FIVE_HUNDRED = BigDecimal.valueOf(500);
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    private final LunchProperties lunchProperties;
    private final ContainerPricingService containerPricingService;

    public OrderCalculationService(LunchProperties lunchProperties, ContainerPricingService containerPricingService) {
        this.lunchProperties = lunchProperties;
        this.containerPricingService = containerPricingService;
    }

    public CalculationResult recalculate(OrderSession session, List<UserOrder> orders, RecalculationMode mode) {
        List<UserOrder> orderedUsers = orders.stream()
                .filter(order -> order.getStatus() == UserOrderStatus.ORDERED)
                .toList();

        BigDecimal configuredDeliveryPrice = defaultIfNull(session.getDeliveryPrice());
        BigDecimal appliedDeliveryPrice = orderedUsers.isEmpty() ? BigDecimal.ZERO : configuredDeliveryPrice;
        BigDecimal deliveryShare = orderedUsers.isEmpty()
                ? BigDecimal.ZERO
                : appliedDeliveryPrice.divide(BigDecimal.valueOf(orderedUsers.size()), 2, RoundingMode.HALF_UP);

        BigDecimal totalFoodAmount = BigDecimal.ZERO;
        BigDecimal totalContainerAmount = BigDecimal.ZERO;
        BigDecimal totalFinalAmount = BigDecimal.ZERO;

        for (UserOrder order : orders) {
            if (order.getStatus() != UserOrderStatus.ORDERED) {
                order.setDeliveryShare(BigDecimal.ZERO);
                order.setFinalPrice(BigDecimal.ZERO);
                continue;
            }

            validateOrderedOrder(order);
            refreshSnapshotsForMode(order, mode);

            BigDecimal quantity = BigDecimal.valueOf(order.getQuantity());
            BigDecimal foodTotal = defaultIfNull(order.getFoodPrice()).multiply(quantity);
            BigDecimal containerTotal = defaultIfNull(order.getContainerPrice()).multiply(quantity);
            BigDecimal rawFinal = foodTotal.add(containerTotal).add(deliveryShare);

            order.setDeliveryShare(deliveryShare);
            order.setFinalPrice(applyRounding(rawFinal));

            totalFoodAmount = totalFoodAmount.add(foodTotal);
            totalContainerAmount = totalContainerAmount.add(containerTotal);
            totalFinalAmount = totalFinalAmount.add(defaultIfNull(order.getFinalPrice()));
        }

        BigDecimal exactTotal = totalFoodAmount.add(totalContainerAmount).add(appliedDeliveryPrice);
        BigDecimal roundingDifference = totalFinalAmount.subtract(exactTotal);

        return new CalculationResult(
                orderedUsers.size(),
                totalFoodAmount,
                totalContainerAmount,
                appliedDeliveryPrice,
                totalFinalAmount,
                roundingDifference);
    }

    public void capturePriceSnapshot(UserOrder order) {
        validateOrderedOrder(order);
        MenuItem menuItem = order.getMenuItem();
        order.setFoodPrice(defaultIfNull(menuItem.getPrice()));
        order.setContainerPrice(containerPricingService.resolve(menuItem));
    }

    private void refreshSnapshotsForMode(UserOrder order, RecalculationMode mode) {
        switch (mode) {
            case DELIVERY_ONLY -> {
                // Keep stored food and container snapshots unchanged.
            }
            case DELIVERY_AND_CONTAINER -> order.setContainerPrice(containerPricingService.resolve(order.getMenuItem()));
            case FULL_PRICE_REBUILD -> {
                order.setFoodPrice(defaultIfNull(order.getMenuItem().getPrice()));
                order.setContainerPrice(containerPricingService.resolve(order.getMenuItem()));
            }
        }
    }

    private void validateOrderedOrder(UserOrder order) {
        MenuItem menuItem = order.getMenuItem();
        if (menuItem == null) {
            throw new CalculationException("Ordered user order must reference a menu item");
        }
        if (order.getQuantity() == null || order.getQuantity() <= 0) {
            throw new CalculationException("Ordered user order must have quantity greater than zero");
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

    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
