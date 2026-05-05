package uz.company.lunchbot.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.company.lunchbot.entity.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByIsDefaultTrueAndIsActiveTrue();

    Optional<Restaurant> findByIsDefaultTrue();

    List<Restaurant> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("update Restaurant restaurant set restaurant.isDefault = false where restaurant.isDefault = true and restaurant.id <> :restaurantId")
    void clearDefaultFlagExcept(@Param("restaurantId") Long restaurantId);
}
