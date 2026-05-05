package uz.company.lunchbot.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.company.lunchbot.entity.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findAllByOrderByCreatedAtDesc();

    List<MenuItem> findAllByRestaurantIdOrderByNameAsc(Long restaurantId);

    List<MenuItem> findAllByRestaurantIdAndIsActiveTrueOrderByNameAsc(Long restaurantId);
}
