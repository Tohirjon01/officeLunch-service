package uz.company.lunchbot.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import uz.company.lunchbot.dto.request.CreateMenuItemRequest;
import uz.company.lunchbot.dto.request.CreateRestaurantRequest;
import uz.company.lunchbot.dto.request.MenuItemContainerSettingsRequest;
import uz.company.lunchbot.dto.request.RestaurantContainerSettingsRequest;
import uz.company.lunchbot.dto.request.RestaurantDeliverySettingsRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemRequest;
import uz.company.lunchbot.dto.request.UpdateRestaurantRequest;
import uz.company.lunchbot.dto.request.UpdateStatusRequest;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.exception.BadRequestException;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.util.MoneyUtils;

@Service
public class TelegramAdminCommandService {

    private final RestaurantService restaurantService;
    private final MenuItemService menuItemService;
    private final OrderSessionService orderSessionService;

    public TelegramAdminCommandService(RestaurantService restaurantService,
                                       MenuItemService menuItemService,
                                       OrderSessionService orderSessionService) {
        this.restaurantService = restaurantService;
        this.menuItemService = menuItemService;
        this.orderSessionService = orderSessionService;
    }

    public boolean supports(String text) {
        return switch (command(text)) {
            case "/restaurants", "/restaurant_create", "/restaurant_update", "/restaurant_activate", "/restaurant_deactivate",
                    "/restaurant_default", "/restaurant_container", "/restaurant_delivery", "/menu", "/menu_add",
                    "/menu_update", "/menu_activate", "/menu_deactivate", "/menu_container",
                    "/menu_container_override_remove", "/session_delivery", "/session_recalculate" -> true;
            default -> false;
        };
    }

    public String handle(LunchUser actor, String text) {
        return switch (command(text)) {
            case "/restaurants" -> formatRestaurants();
            case "/restaurant_create" -> createRestaurant(actor, args(text));
            case "/restaurant_update" -> updateRestaurant(actor, args(text));
            case "/restaurant_activate" -> updateRestaurantStatus(actor, args(text), true);
            case "/restaurant_deactivate" -> updateRestaurantStatus(actor, args(text), false);
            case "/restaurant_default" -> setDefaultRestaurant(actor, args(text));
            case "/restaurant_container" -> updateRestaurantContainer(actor, args(text));
            case "/restaurant_delivery" -> updateRestaurantDelivery(actor, args(text));
            case "/menu" -> formatMenu(args(text));
            case "/menu_add" -> createMenuItem(actor, args(text));
            case "/menu_update" -> updateMenuItem(actor, args(text));
            case "/menu_activate" -> updateMenuItemStatus(actor, args(text), true);
            case "/menu_deactivate" -> updateMenuItemStatus(actor, args(text), false);
            case "/menu_container" -> updateMenuItemContainer(actor, args(text));
            case "/menu_container_override_remove" -> removeMenuItemContainerOverride(actor, args(text));
            case "/session_delivery" -> updateCurrentSessionDelivery(actor, args(text));
            case "/session_recalculate" -> recalculateCurrentSession(actor, args(text));
            default -> throw new BadRequestException("Unsupported admin command");
        };
    }

    private String formatRestaurants() {
        List<Restaurant> restaurants = restaurantService.getAll();
        if (restaurants.isEmpty()) {
            return "No restaurants found.";
        }

        return restaurants.stream()
                .map(this::formatRestaurant)
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("No restaurants found.");
    }

    private String createRestaurant(LunchUser actor, String rawArgs) {
        List<String> args = split(rawArgs, 9);
        Restaurant restaurant = restaurantService.create(new CreateRestaurantRequest(
                args.get(0),
                nullableText(args.get(1)),
                nullableText(args.get(2)),
                parseBoolean(args.get(3)),
                parseBoolean(args.get(4)),
                parseBoolean(args.get(5)),
                parseMoney(args.get(6)),
                parseBoolean(args.get(7)),
                parseMoney(args.get(8))), actor.getId());
        return "Restaurant created:\n" + formatRestaurant(restaurant);
    }

    private String updateRestaurant(LunchUser actor, String rawArgs) {
        List<String> args = split(rawArgs, 4);
        Restaurant restaurant = restaurantService.update(parseLong(args.get(0)), new UpdateRestaurantRequest(
                args.get(1),
                nullableText(args.get(2)),
                nullableText(args.get(3))), actor.getId());
        return "Restaurant updated:\n" + formatRestaurant(restaurant);
    }

    private String updateRestaurantStatus(LunchUser actor, String rawArgs, boolean active) {
        Restaurant restaurant = restaurantService.updateStatus(parseLong(requireSingle(rawArgs)), new UpdateStatusRequest(active), actor.getId());
        return "Restaurant updated:\n" + formatRestaurant(restaurant);
    }

    private String setDefaultRestaurant(LunchUser actor, String rawArgs) {
        Restaurant restaurant = restaurantService.setDefault(parseLong(requireSingle(rawArgs)), actor.getId());
        return "Default restaurant changed:\n" + formatRestaurant(restaurant);
    }

    private String updateRestaurantContainer(LunchUser actor, String rawArgs) {
        List<String> args = split(rawArgs, 3);
        Restaurant restaurant = restaurantService.updateContainerSettings(
                parseLong(args.get(0)),
                new RestaurantContainerSettingsRequest(parseBoolean(args.get(1)), parseMoney(args.get(2))),
                actor.getId());
        return "Restaurant container settings updated:\n" + formatRestaurant(restaurant);
    }

    private String updateRestaurantDelivery(LunchUser actor, String rawArgs) {
        List<String> args = split(rawArgs, 3);
        Restaurant restaurant = restaurantService.updateDeliverySettings(
                parseLong(args.get(0)),
                new RestaurantDeliverySettingsRequest(parseBoolean(args.get(1)), parseMoney(args.get(2))),
                actor.getId());
        return "Restaurant delivery settings updated:\n" + formatRestaurant(restaurant);
    }

    private String formatMenu(String rawArgs) {
        Long restaurantId = parseLong(requireSingle(rawArgs));
        List<MenuItem> items = menuItemService.getAllByRestaurant(restaurantId);
        if (items.isEmpty()) {
            return "No menu items found for restaurant " + restaurantId + ".";
        }

        return items.stream()
                .map(this::formatMenuItem)
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("No menu items found.");
    }

    private String createMenuItem(LunchUser actor, String rawArgs) {
        List<String> args = split(rawArgs, 6);
        MenuItem menuItem = menuItemService.create(new CreateMenuItemRequest(
                parseLong(args.get(0)),
                args.get(1),
                parseMoney(args.get(2)),
                parseBoolean(args.get(3)),
                parseNullableBoolean(args.get(4)),
                parseNullableMoney(args.get(5))), actor.getId());
        return "Menu item created:\n" + formatMenuItem(menuItem);
    }

    private String updateMenuItem(LunchUser actor, String rawArgs) {
        List<String> args = split(rawArgs, 3);
        MenuItem menuItem = menuItemService.update(parseLong(args.get(0)), new UpdateMenuItemRequest(
                args.get(1),
                parseMoney(args.get(2))), actor.getId());
        return "Menu item updated:\n" + formatMenuItem(menuItem);
    }

    private String updateMenuItemStatus(LunchUser actor, String rawArgs, boolean active) {
        MenuItem menuItem = menuItemService.updateStatus(parseLong(requireSingle(rawArgs)), new UpdateStatusRequest(active), actor.getId());
        return "Menu item updated:\n" + formatMenuItem(menuItem);
    }

    private String updateMenuItemContainer(LunchUser actor, String rawArgs) {
        List<String> args = split(rawArgs, 3);
        MenuItem menuItem = menuItemService.updateContainerSettings(
                parseLong(args.get(0)),
                new MenuItemContainerSettingsRequest(parseNullableBoolean(args.get(1)), parseNullableMoney(args.get(2))),
                actor.getId());
        return "Menu item container settings updated:\n" + formatMenuItem(menuItem);
    }

    private String removeMenuItemContainerOverride(LunchUser actor, String rawArgs) {
        MenuItem menuItem = menuItemService.removeContainerOverride(parseLong(requireSingle(rawArgs)), actor.getId());
        return "Menu item container override removed:\n" + formatMenuItem(menuItem);
    }

    private String updateCurrentSessionDelivery(LunchUser actor, String rawArgs) {
        OrderSession session = currentSession();
        OrderSession updated = orderSessionService.updateDeliveryPrice(session.getId(), parseMoney(requireSingle(rawArgs)), actor.getId());
        return "Current session delivery updated:\nSession ID: " + updated.getId() + "\nDelivery: " + MoneyUtils.formatUzs(updated.getDeliveryPrice());
    }

    private String recalculateCurrentSession(LunchUser actor, String rawArgs) {
        OrderSession session = currentSession();
        boolean allowConfirmed = !rawArgs.isBlank() && "allow_confirmed".equalsIgnoreCase(rawArgs.trim());
        OrderSession recalculated = orderSessionService.recalculateSession(session.getId(), actor.getId(), allowConfirmed);
        return "Current session recalculated:\nSession ID: " + recalculated.getId() + "\nStatus: " + recalculated.getStatus();
    }

    private OrderSession currentSession() {
        return orderSessionService.getTodaySession().orElseThrow(() -> new NotFoundException("Today's order session not found"));
    }

    private String formatRestaurant(Restaurant restaurant) {
        return """
                #%d %s
                active=%s, default=%s
                phone=%s
                address=%s
                containerEnabled=%s, defaultContainerPrice=%s
                deliveryEnabled=%s, defaultDeliveryPrice=%s
                """.formatted(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.isActive(),
                restaurant.isDefault(),
                fallback(restaurant.getPhone()),
                fallback(restaurant.getAddress()),
                restaurant.isContainerEnabled(),
                MoneyUtils.formatUzs(restaurant.getDefaultContainerPrice()),
                restaurant.isDeliveryEnabled(),
                MoneyUtils.formatUzs(restaurant.getDefaultDeliveryPrice()));
    }

    private String formatMenuItem(MenuItem menuItem) {
        return """
                #%d %s
                restaurantId=%d, active=%s
                price=%s
                containerRequired=%s
                containerPriceOverride=%s
                """.formatted(
                menuItem.getId(),
                menuItem.getName(),
                menuItem.getRestaurant().getId(),
                menuItem.isActive(),
                MoneyUtils.formatUzs(menuItem.getPrice()),
                menuItem.getContainerRequired(),
                menuItem.getContainerPriceOverride() == null ? "-" : MoneyUtils.formatUzs(menuItem.getContainerPriceOverride()));
    }

    private List<String> split(String rawArgs, int expectedSize) {
        List<String> args = Arrays.stream(rawArgs.split("\\|", -1))
                .map(String::trim)
                .toList();
        if (args.size() != expectedSize) {
            throw new BadRequestException("Expected " + expectedSize + " arguments separated by '|'");
        }
        return args;
    }

    private String requireSingle(String rawArgs) {
        if (rawArgs == null || rawArgs.isBlank()) {
            throw new BadRequestException("Command argument is required");
        }
        return rawArgs.trim();
    }

    private String command(String text) {
        String firstToken = text.trim().split("\\s+", 2)[0];
        int mentionIndex = firstToken.indexOf('@');
        return mentionIndex >= 0 ? firstToken.substring(0, mentionIndex) : firstToken;
    }

    private String args(String text) {
        String trimmed = text.trim();
        int firstSpace = trimmed.indexOf(' ');
        return firstSpace < 0 ? "" : trimmed.substring(firstSpace + 1).trim();
    }

    private Long parseLong(String token) {
        try {
            return Long.parseLong(token.trim());
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Invalid numeric value: " + token);
        }
    }

    private BigDecimal parseMoney(String token) {
        try {
            return new BigDecimal(token.replace(",", "").trim());
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Invalid money value: " + token);
        }
    }

    private BigDecimal parseNullableMoney(String token) {
        return isNullToken(token) ? null : parseMoney(token);
    }

    private Boolean parseBoolean(String token) {
        return switch (token.trim().toLowerCase()) {
            case "true", "yes", "1", "on" -> true;
            case "false", "no", "0", "off" -> false;
            default -> throw new BadRequestException("Invalid boolean value: " + token);
        };
    }

    private Boolean parseNullableBoolean(String token) {
        return isNullToken(token) ? null : parseBoolean(token);
    }

    private String nullableText(String token) {
        return isNullToken(token) ? null : token.trim();
    }

    private boolean isNullToken(String token) {
        String normalized = token == null ? "" : token.trim();
        return normalized.isEmpty() || normalized.equals("-") || normalized.equals("--") || normalized.equalsIgnoreCase("null");
    }

    private String fallback(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
