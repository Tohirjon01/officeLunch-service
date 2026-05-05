package uz.company.lunchbot.service.calculation;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.Restaurant;

@Service
public class ContainerPricingService {

    public BigDecimal resolve(MenuItem menuItem) {
        Restaurant restaurant = menuItem.getRestaurant();

        if (Boolean.FALSE.equals(menuItem.getContainerRequired())) {
            return BigDecimal.ZERO;
        }
        if (menuItem.getContainerPriceOverride() != null) {
            return menuItem.getContainerPriceOverride();
        }
        if (restaurant.isContainerEnabled()) {
            return defaultIfNull(restaurant.getDefaultContainerPrice());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
