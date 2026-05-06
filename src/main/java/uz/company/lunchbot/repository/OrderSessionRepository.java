package uz.company.lunchbot.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.enums.OrderSessionStatus;

public interface OrderSessionRepository extends JpaRepository<OrderSession, Long> {

    Optional<OrderSession> findFirstByOrderDateOrderByCreatedAtDesc(LocalDate orderDate);

    Optional<OrderSession> findFirstByOrderDateAndStatusOrderByCreatedAtDesc(LocalDate orderDate, OrderSessionStatus status);

    Optional<OrderSession> findByRestaurantIdAndOrderDate(Long restaurantId, LocalDate orderDate);

    Optional<OrderSession> findFirstByRestaurantIdAndStatusOrderByOrderDateDesc(Long restaurantId, OrderSessionStatus status);

    Optional<OrderSession> findByRestaurantIdAndOrderDateAndStatus(Long restaurantId, LocalDate orderDate, OrderSessionStatus status);

    List<OrderSession> findAllByStatusOrderByOrderDateAscCreatedAtAsc(OrderSessionStatus status);
}
