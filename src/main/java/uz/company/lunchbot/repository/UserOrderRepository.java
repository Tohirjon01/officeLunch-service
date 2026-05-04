package uz.company.lunchbot.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.UserOrderStatus;

public interface UserOrderRepository extends JpaRepository<UserOrder, Long> {

    Optional<UserOrder> findByOrderSessionIdAndUserId(Long orderSessionId, Long userId);

    List<UserOrder> findAllByOrderSessionId(Long orderSessionId);

    List<UserOrder> findAllByOrderSessionIdAndStatus(Long orderSessionId, UserOrderStatus status);

    long countByOrderSessionIdAndStatus(Long orderSessionId, UserOrderStatus status);
}
