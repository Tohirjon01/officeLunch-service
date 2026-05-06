package uz.company.lunchbot.service.calculation;

import java.math.BigDecimal;
import uz.company.lunchbot.entity.MenuItem;

public interface ContainerPricingService {

    BigDecimal resolve(MenuItem menuItem);
}
