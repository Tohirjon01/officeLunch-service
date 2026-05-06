package uz.company.lunchbot.service.calculation;

import java.util.List;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.RecalculationMode;
public interface OrderCalculationService {

    CalculationResult recalculate(OrderSession session, List<UserOrder> orders, RecalculationMode mode);

    void capturePriceSnapshot(UserOrder order);
}
