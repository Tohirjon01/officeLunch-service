package uz.company.lunchbot.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.company.lunchbot.entity.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByIsDefaultTrueAndIsActiveTrue();

    List<Restaurant> findAllByOrderByCreatedAtDesc();
}
