package uz.company.lunchbot.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import uz.company.lunchbot.dto.response.MenuItemResponse;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.Restaurant;

class MenuItemMapperTest {

    @Test
    void shouldIncludeImageFieldsInResponse() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(2L);
        restaurant.setName("Tarnov");

        MenuItem item = new MenuItem();
        item.setId(5L);
        item.setRestaurant(restaurant);
        item.setName("Bon file");
        item.setPrice(new BigDecimal("106500"));
        item.setActive(true);
        item.setSortOrder(1);
        item.setCategory("Grill");
        item.setImageUrl("https://example.com/bon-file.jpg");
        item.setTelegramImageFileId("telegram-file-id");

        MenuItemResponse response = new MenuItemMapper().toResponse(item);

        assertThat(response.category()).isEqualTo("Grill");
        assertThat(response.imageUrl()).isEqualTo("https://example.com/bon-file.jpg");
        assertThat(response.telegramImageFileId()).isEqualTo("telegram-file-id");
    }
}
