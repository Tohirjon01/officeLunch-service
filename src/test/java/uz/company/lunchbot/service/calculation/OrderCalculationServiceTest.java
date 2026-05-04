package uz.company.lunchbot.service.calculation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.RoundingStrategy;
import uz.company.lunchbot.enums.UserOrderStatus;

class OrderCalculationServiceTest {

    @Test
    void shouldApplyConfiguredCeilToHundredRounding() {
        LunchProperties properties = properties(RoundingStrategy.CEIL_TO_100);
        OrderCalculationService service = new OrderCalculationService(properties);

        OrderSession session = new OrderSession();
        session.setDeliveryPrice(new BigDecimal("3076"));
        session.setContainerPrice(new BigDecimal("2000"));

        UserOrder first = ordered("33000", 1);
        UserOrder second = ordered("35000", 1);

        service.recalculate(session, List.of(first, second));

        assertThat(first.getDeliveryShare()).isEqualByComparingTo("1538.00");
        assertThat(first.getFinalPrice()).isEqualByComparingTo("36600");
        assertThat(second.getFinalPrice()).isEqualByComparingTo("38600");
    }

    @Test
    void shouldZeroOutSkippedOrders() {
        LunchProperties properties = properties(RoundingStrategy.HALF_UP);
        OrderCalculationService service = new OrderCalculationService(properties);

        OrderSession session = new OrderSession();
        session.setDeliveryPrice(new BigDecimal("30000"));
        session.setContainerPrice(new BigDecimal("2000"));

        UserOrder skipped = new UserOrder();
        skipped.setStatus(UserOrderStatus.SKIPPED);
        skipped.setQuantity(1);
        skipped.setFoodPrice(BigDecimal.ZERO);

        service.recalculate(session, List.of(skipped));

        assertThat(skipped.getDeliveryShare()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(skipped.getContainerPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(skipped.getFinalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private static UserOrder ordered(String foodPrice, int quantity) {
        UserOrder order = new UserOrder();
        order.setStatus(UserOrderStatus.ORDERED);
        order.setQuantity(quantity);
        order.setFoodPrice(new BigDecimal(foodPrice));
        return order;
    }

    private static LunchProperties properties(RoundingStrategy strategy) {
        return new LunchProperties(
                1L,
                "Asia/Tashkent",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalTime.NOON,
                strategy,
                new LunchProperties.Scheduler(true, "", "", ""),
                new LunchProperties.Bootstrap(0L, "", "", "", 0L));
    }
}
