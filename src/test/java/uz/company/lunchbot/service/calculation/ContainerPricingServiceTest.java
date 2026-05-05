package uz.company.lunchbot.service.calculation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.Restaurant;

class ContainerPricingServiceTest {

    private final ContainerPricingService service = new ContainerPricingService();

    @Test
    void shouldReturnZeroWhenMenuItemExplicitlyDoesNotRequireContainer() {
        MenuItem menuItem = menuItem(restaurant(true, "2000"), Boolean.FALSE, new BigDecimal("3000"));

        assertThat(service.resolve(menuItem)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldUseItemOverrideWhenPresent() {
        MenuItem menuItem = menuItem(restaurant(true, "2000"), null, new BigDecimal("3000"));

        assertThat(service.resolve(menuItem)).isEqualByComparingTo("3000");
    }

    @Test
    void shouldUseRestaurantDefaultWhenEnabledAndNoOverrideExists() {
        MenuItem menuItem = menuItem(restaurant(true, "2000"), Boolean.TRUE, null);

        assertThat(service.resolve(menuItem)).isEqualByComparingTo("2000");
    }

    @Test
    void shouldReturnZeroWhenRestaurantContainerPricingIsDisabledWithoutOverride() {
        MenuItem menuItem = menuItem(restaurant(false, "2000"), null, null);

        assertThat(service.resolve(menuItem)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private static MenuItem menuItem(Restaurant restaurant, Boolean containerRequired, BigDecimal override) {
        MenuItem menuItem = new MenuItem();
        menuItem.setRestaurant(restaurant);
        menuItem.setContainerRequired(containerRequired);
        menuItem.setContainerPriceOverride(override);
        return menuItem;
    }

    private static Restaurant restaurant(boolean containerEnabled, String defaultContainerPrice) {
        Restaurant restaurant = new Restaurant();
        restaurant.setContainerEnabled(containerEnabled);
        restaurant.setDefaultContainerPrice(new BigDecimal(defaultContainerPrice));
        return restaurant;
    }
}
