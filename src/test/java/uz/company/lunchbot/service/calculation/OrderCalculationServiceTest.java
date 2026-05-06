package uz.company.lunchbot.service.calculation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.RecalculationMode;
import uz.company.lunchbot.enums.RoundingStrategy;
import uz.company.lunchbot.enums.UserOrderStatus;
import uz.company.lunchbot.service.calculation.impl.ContainerPricingServiceImpl;
import uz.company.lunchbot.service.calculation.impl.OrderCalculationServiceImpl;

class OrderCalculationServiceTest {

    @Test
    void shouldApplyConfiguredCeilToHundredRounding() {
        OrderCalculationService service = new OrderCalculationServiceImpl(
                properties(RoundingStrategy.CEIL_TO_100),
                new ContainerPricingServiceImpl());

        OrderSession session = new OrderSession();
        session.setDeliveryPrice(new BigDecimal("3076"));

        Restaurant restaurant = restaurant(true, "2000");
        UserOrder first = ordered(menuItem(restaurant, "Toy Oshi", "33000"), 1);
        UserOrder second = ordered(menuItem(restaurant, "Choyxona", "35000"), 1);

        service.recalculate(session, List.of(first, second), RecalculationMode.FULL_PRICE_REBUILD);

        assertThat(first.getFoodPrice()).isEqualByComparingTo("33000");
        assertThat(first.getContainerPrice()).isEqualByComparingTo("2000");
        assertThat(first.getDeliveryShare()).isEqualByComparingTo("1538.00");
        assertThat(first.getFinalPrice()).isEqualByComparingTo("36600");
        assertThat(second.getFinalPrice()).isEqualByComparingTo("38600");
    }

    @Test
    void shouldZeroOutSkippedOrders() {
        OrderCalculationService service = new OrderCalculationServiceImpl(
                properties(RoundingStrategy.HALF_UP),
                new ContainerPricingServiceImpl());

        OrderSession session = new OrderSession();
        session.setDeliveryPrice(new BigDecimal("30000"));

        UserOrder skipped = new UserOrder();
        skipped.setStatus(UserOrderStatus.SKIPPED);
        skipped.setQuantity(1);
        skipped.setFoodPrice(new BigDecimal("33000"));
        skipped.setContainerPrice(new BigDecimal("2000"));

        service.recalculate(session, List.of(skipped), RecalculationMode.DELIVERY_ONLY);

        assertThat(skipped.getFoodPrice()).isEqualByComparingTo("33000");
        assertThat(skipped.getDeliveryShare()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(skipped.getContainerPrice()).isEqualByComparingTo("2000");
        assertThat(skipped.getFinalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private static UserOrder ordered(MenuItem menuItem, int quantity) {
        UserOrder order = new UserOrder();
        order.setMenuItem(menuItem);
        order.setStatus(UserOrderStatus.ORDERED);
        order.setQuantity(quantity);
        return order;
    }

    private static MenuItem menuItem(Restaurant restaurant, String name, String price) {
        MenuItem menuItem = new MenuItem();
        menuItem.setRestaurant(restaurant);
        menuItem.setName(name);
        menuItem.setPrice(new BigDecimal(price));
        return menuItem;
    }

    private static Restaurant restaurant(boolean containerEnabled, String defaultContainerPrice) {
        Restaurant restaurant = new Restaurant();
        restaurant.setContainerEnabled(containerEnabled);
        restaurant.setDefaultContainerPrice(new BigDecimal(defaultContainerPrice));
        return restaurant;
    }

    private static LunchProperties properties(RoundingStrategy strategy) {
        return new LunchProperties(
                1L,
                "Asia/Tashkent",
                LocalTime.NOON,
                strategy,
                new LunchProperties.Scheduler(true, "", "", ""),
                new LunchProperties.RestaurantVoting(false, "", "", 5),
                new LunchProperties.Payment(true, "8600", "Owner", true),
                new LunchProperties.Bootstrap(0L, "", "", "", 0L));
    }
}
