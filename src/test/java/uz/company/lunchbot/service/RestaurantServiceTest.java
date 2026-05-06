package uz.company.lunchbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uz.company.lunchbot.dto.request.CreateRestaurantRequest;
import uz.company.lunchbot.dto.request.UpdateStatusRequest;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.exception.BadRequestException;
import uz.company.lunchbot.repository.RestaurantRepository;
import uz.company.lunchbot.security.AdminAccessService;
import uz.company.lunchbot.service.impl.RestaurantServiceImpl;

class RestaurantServiceTest {

    @Test
    void shouldCreateRestaurant() {
        RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        RestaurantServiceImpl service = new RestaurantServiceImpl(restaurantRepository, adminAccessService, auditService);

        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> {
            Restaurant restaurant = invocation.getArgument(0);
            ReflectionTestUtils.setField(restaurant, "id", 1L);
            return restaurant;
        });

        Restaurant restaurant = service.create(new CreateRestaurantRequest(
                "Osh Posh",
                "Traditional lunch restaurant",
                "+998900001122",
                "Tashkent",
                true,
                true,
                true,
                new BigDecimal("2000"),
                true,
                new BigDecimal("20000")
        ), 7L);

        assertThat(restaurant.getId()).isEqualTo(1L);
        assertThat(restaurant.getName()).isEqualTo("Osh Posh");
        assertThat(restaurant.getDescription()).isEqualTo("Traditional lunch restaurant");
        assertThat(restaurant.getPhoneNumber()).isEqualTo("+998900001122");
        assertThat(restaurant.getAddress()).isEqualTo("Tashkent");
        assertThat(restaurant.isDefault()).isTrue();
        verify(restaurantRepository).clearDefaultFlagExcept(-1L);
    }

    @Test
    void shouldSetDefaultRestaurantAndClearOthers() {
        RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        RestaurantServiceImpl service = new RestaurantServiceImpl(restaurantRepository, adminAccessService, auditService);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(5L);
        restaurant.setActive(true);

        when(restaurantRepository.findById(5L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Restaurant updated = service.setDefault(5L, 10L);

        assertThat(updated.isDefault()).isTrue();
        verify(restaurantRepository).clearDefaultFlagExcept(5L);
    }

    @Test
    void shouldDeactivateRestaurant() {
        RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        RestaurantServiceImpl service = new RestaurantServiceImpl(restaurantRepository, adminAccessService, auditService);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(3L);
        restaurant.setActive(true);
        restaurant.setDefault(false);
        restaurant.setName("Tarnov");
        restaurant.setDefaultContainerPrice(BigDecimal.ZERO);
        restaurant.setDefaultDeliveryPrice(BigDecimal.ZERO);

        when(restaurantRepository.findById(3L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Restaurant updated = service.updateStatus(3L, new UpdateStatusRequest(false), 9L);

        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void shouldRejectInactiveDefaultRestaurant() {
        RestaurantRepository restaurantRepository = mock(RestaurantRepository.class);
        AdminAccessService adminAccessService = mock(AdminAccessService.class);
        AuditService auditService = mock(AuditService.class);

        RestaurantServiceImpl service = new RestaurantServiceImpl(restaurantRepository, adminAccessService, auditService);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(4L);
        restaurant.setActive(true);
        restaurant.setDefault(true);
        restaurant.setName("KFC");
        restaurant.setDefaultContainerPrice(BigDecimal.ZERO);
        restaurant.setDefaultDeliveryPrice(BigDecimal.ZERO);

        when(restaurantRepository.findById(4L)).thenReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> service.updateStatus(4L, new UpdateStatusRequest(false), 9L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Default restaurant must be active");
    }
}
