package uz.company.lunchbot.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.company.lunchbot.entity.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findAllByOrderByCreatedAtDesc();

    List<MenuItem> findAllByRestaurantIdOrderBySortOrderAscNameAsc(Long restaurantId);

    List<MenuItem> findAllByRestaurantIdAndIsActiveTrueOrderBySortOrderAscNameAsc(Long restaurantId);

    @Query("select coalesce(max(item.sortOrder), 0) from MenuItem item where item.restaurant.id = :restaurantId")
    Integer findMaxSortOrderByRestaurantId(@Param("restaurantId") Long restaurantId);
}
