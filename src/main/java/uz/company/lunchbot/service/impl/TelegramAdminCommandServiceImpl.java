package uz.company.lunchbot.service.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.company.lunchbot.dto.request.CreateMenuItemRequest;
import uz.company.lunchbot.dto.request.CreateRestaurantRequest;
import uz.company.lunchbot.dto.request.MenuItemContainerSettingsRequest;
import uz.company.lunchbot.dto.request.RecalculateSessionRequest;
import uz.company.lunchbot.dto.request.RestaurantContainerSettingsRequest;
import uz.company.lunchbot.dto.request.RestaurantDeliverySettingsRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemCategoryRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemImageRequest;
import uz.company.lunchbot.dto.request.UpdateMenuItemRequest;
import uz.company.lunchbot.dto.request.UpdateRestaurantRequest;
import uz.company.lunchbot.dto.request.UpdateStatusRequest;
import uz.company.lunchbot.dto.response.RestaurantVoteSessionResponse;
import uz.company.lunchbot.dto.response.SessionRecalculationResponse;
import uz.company.lunchbot.dto.response.SessionSummaryResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.MenuItem;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.RestaurantVoteSession;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.enums.OrderSessionStatus;
import uz.company.lunchbot.enums.RecalculationMode;
import uz.company.lunchbot.enums.RestaurantVoteSessionStatus;
import uz.company.lunchbot.exception.BadRequestException;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.security.AdminAccessService;
import uz.company.lunchbot.service.MenuPhotoUploadStateService;
import uz.company.lunchbot.service.MenuItemService;
import uz.company.lunchbot.service.OrderSessionService;
import uz.company.lunchbot.service.RestaurantService;
import uz.company.lunchbot.service.RestaurantVoteSessionService;
import uz.company.lunchbot.service.SummaryService;
import uz.company.lunchbot.service.TelegramAdminCommandService;
import uz.company.lunchbot.service.UserOrderService;
import uz.company.lunchbot.bot.keyboard.TelegramKeyboards;
import uz.company.lunchbot.util.MoneyUtils;

@Slf4j
@Service
public class TelegramAdminCommandServiceImpl implements TelegramAdminCommandService {

    private static final int MENU_PAGE_SIZE = 10;

    private final RestaurantService restaurantService;
    private final MenuItemService menuItemService;
    private final OrderSessionService orderSessionService;
    private final RestaurantVoteSessionService restaurantVoteSessionService;
    private final SummaryService summaryService;
    private final UserOrderService userOrderService;
    private final AdminAccessService adminAccessService;
    private final TelegramKeyboards telegramKeyboards;
    private final MenuPhotoUploadStateService menuPhotoUploadStateService;

    public TelegramAdminCommandServiceImpl(RestaurantService restaurantService,
                                           MenuItemService menuItemService,
                                           OrderSessionService orderSessionService,
                                           RestaurantVoteSessionService restaurantVoteSessionService,
                                           SummaryService summaryService,
                                           UserOrderService userOrderService,
                                           AdminAccessService adminAccessService,
                                           TelegramKeyboards telegramKeyboards,
                                           MenuPhotoUploadStateService menuPhotoUploadStateService) {
        this.restaurantService = restaurantService;
        this.menuItemService = menuItemService;
        this.orderSessionService = orderSessionService;
        this.restaurantVoteSessionService = restaurantVoteSessionService;
        this.summaryService = summaryService;
        this.userOrderService = userOrderService;
        this.adminAccessService = adminAccessService;
        this.telegramKeyboards = telegramKeyboards;
        this.menuPhotoUploadStateService = menuPhotoUploadStateService;
    }

    @Override
    public boolean supports(String text) {
        return switch (command(text)) {
            case "/restaurants", "/restaurant_create", "/restaurant_update", "/restaurant_activate", "/restaurant_deactivate",
                    "/restaurant_default", "/restaurant_container", "/restaurant_delivery", "/menu", "/menu_add",
                    "/menu_update", "/menu_activate", "/menu_deactivate", "/menu_container",
                    "/menu_container_override_remove", "/menu_category", "/menu_image", "/menu_photo", "/menu_photo_cancel",
                    "/session_delivery", "/session_recalculate", "/vote_open", "/vote_close", "/order_open_default",
                    "/order_close_now", "/order_control", "/vote_control", "/status" -> true;
            default -> false;
        };
    }

    @Override
    public AdminCommandResponse handle(LunchUser actor, String text) {
        adminAccessService.ensureAdmin(actor.getId());

        return switch (command(text)) {
            case "/restaurants" -> response(formatRestaurants());
            case "/restaurant_create" -> response(createRestaurant(actor, args(text)));
            case "/restaurant_update" -> response(updateRestaurant(actor, args(text)));
            case "/restaurant_activate" -> response(updateRestaurantStatus(actor, args(text), true));
            case "/restaurant_deactivate" -> response(updateRestaurantStatus(actor, args(text), false));
            case "/restaurant_default" -> response(setDefaultRestaurant(actor, args(text)));
            case "/restaurant_container" -> response(updateRestaurantContainer(actor, args(text)));
            case "/restaurant_delivery" -> response(updateRestaurantDelivery(actor, args(text)));
            case "/menu" -> buildMenuPage(actor, parseLong(requireSingle(args(text))), 0);
            case "/menu_add" -> response(createMenuItem(actor, args(text)));
            case "/menu_update" -> response(updateMenuItem(actor, args(text)));
            case "/menu_activate" -> response(updateMenuItemStatus(actor, args(text), true));
            case "/menu_deactivate" -> response(updateMenuItemStatus(actor, args(text), false));
            case "/menu_container" -> response(updateMenuItemContainer(actor, args(text)));
            case "/menu_container_override_remove" -> response(removeMenuItemContainerOverride(actor, args(text)));
            case "/menu_category" -> response(updateMenuItemCategory(actor, args(text)));
            case "/menu_image" -> response(updateMenuItemImage(actor, args(text)));
            case "/menu_photo" -> response(startMenuPhotoUpload(actor, args(text)));
            case "/menu_photo_cancel" -> response(cancelMenuPhotoUpload(actor));
            case "/session_delivery" -> response(updateCurrentSessionDelivery(actor, args(text)));
            case "/session_recalculate" -> response(recalculateCurrentSession(actor, args(text)));
            case "/vote_open" -> response(openVote(actor));
            case "/vote_close" -> response(closeVote(actor));
            case "/order_open_default" -> response(openDefaultOrder(actor));
            case "/order_close_now" -> response(closeOrderNow(actor));
            case "/order_control" -> buildOrderControl(actor, null);
            case "/vote_control" -> buildVoteControl(actor, null);
            case "/status" -> response(buildAdminStatus(actor));
            default -> throw new BadRequestException("Unsupported admin command");
        };
    }

    @Override
    public AdminCommandResponse buildMenuPage(LunchUser actor, Long restaurantId, int page) {
        adminAccessService.ensureAdmin(actor.getId());

        Restaurant restaurant = restaurantService.getRequired(restaurantId);
        List<MenuItem> items = menuItemService.getAllByRestaurant(restaurantId);
        int totalPages = Math.max(1, (items.size() + MENU_PAGE_SIZE - 1) / MENU_PAGE_SIZE);
        int boundedPage = Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = Math.min(boundedPage * MENU_PAGE_SIZE, items.size());
        int toIndex = Math.min(fromIndex + MENU_PAGE_SIZE, items.size());

        String lines = items.subList(fromIndex, toIndex).stream()
                .map(this::formatMenuPageItem)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No menu items found.");

        String text = "%s menu — page %d/%d\n\n%s".formatted(
                restaurant.getName(),
                boundedPage + 1,
                totalPages,
                lines
        );

        return new AdminCommandResponse(text, telegramKeyboards.menuPagination(restaurantId, boundedPage, totalPages));
    }

    @Override
    public AdminCommandResponse buildOrderControl(LunchUser actor, Long sessionId) {
        adminAccessService.ensureAdmin(actor.getId());

        OrderSession session = sessionId == null
                ? orderSessionService.getTodaySession().orElse(null)
                : orderSessionService.getRequired(sessionId);
        if (session == null) {
            return response("Bugungi order session topilmadi.");
        }

        SessionSummaryResponse summary = summaryService.buildSummary(session.getId());
        String text = """
                🍽 Bugungi order control

                Restoran: %s
                Status: %s
                Deadline: %s
                Buyurtmalar soni: %d
                Skip qilganlar: %d
                Javob bermaganlar: %d
                """.formatted(
                session.getRestaurant().getName(),
                session.getStatus(),
                session.getDeadlineAt().toLocalTime(),
                summary.orderedCount(),
                summary.skippedCount(),
                summary.noResponseCount()
        );

        log.info("order_control_opened actorUserId={} sessionId={}", actor.getId(), session.getId());
        return new AdminCommandResponse(text, telegramKeyboards.orderControl(session.getId()));
    }

    @Override
    public AdminCommandResponse buildVoteControl(LunchUser actor, Long voteSessionId) {
        adminAccessService.ensureAdmin(actor.getId());

        RestaurantVoteSession session = voteSessionId == null
                ? restaurantVoteSessionService.getTodaySession().orElse(null)
                : restaurantVoteSessionService.getRequired(voteSessionId);
        if (session == null) {
            return response("Bugungi voting topilmadi.");
        }

        RestaurantVoteSessionResponse response = session.getVoteDate() == null
                ? restaurantVoteSessionService.getTodayResponse(actor.getId())
                : toResponse(session, actor.getId());

        String restaurants = response.voteCounts().stream()
                .map(count -> "- " + count.restaurantName() + " — " + count.voteCount() + " ovoz")
                .reduce((left, right) -> left + "\n" + right)
                .orElse("-");

        String text = """
                🗳 Bugungi voting control

                Status: %s
                Deadline: %s
                Restoranlar:
                %s
                """.formatted(
                response.status(),
                response.deadlineAt().toLocalTime(),
                restaurants
        );

        return new AdminCommandResponse(text, telegramKeyboards.voteControl(session.getId()));
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
                null,
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
                null,
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

    private String createMenuItem(LunchUser actor, String rawArgs) {
        List<String> args = split(rawArgs, 6);
        MenuItem menuItem = menuItemService.create(new CreateMenuItemRequest(
                parseLong(args.get(0)),
                args.get(1),
                null,
                parseMoney(args.get(2)),
                parseBoolean(args.get(3)),
                null,
                parseNullableBoolean(args.get(4)),
                parseNullableMoney(args.get(5)),
                null,
                null), actor.getId());
        return "Menu item created:\n" + formatMenuItem(menuItem);
    }

    private String updateMenuItem(LunchUser actor, String rawArgs) {
        List<String> args = split(rawArgs, 3);
        MenuItem menuItem = menuItemService.update(parseLong(args.get(0)), new UpdateMenuItemRequest(
                args.get(1),
                null,
                parseMoney(args.get(2)),
                null,
                null,
                null), actor.getId());
        return "Menu item updated:\n" + formatMenuItem(menuItem);
    }

    private String updateMenuItemImage(LunchUser actor, String rawArgs) {
        List<String> args = split(rawArgs, 2);
        boolean remove = isNullToken(args.get(1));
        MenuItem menuItem = menuItemService.updateImage(
                parseLong(args.get(0)),
                new UpdateMenuItemImageRequest(remove ? null : args.get(1).trim(), null),
                actor.getId()
        );
        return "Menu item image updated:\n" + formatMenuItem(menuItem);
    }

    private String updateMenuItemCategory(LunchUser actor, String rawArgs) {
        List<String> args = split(rawArgs, 2);
        boolean remove = isNullToken(args.get(1));
        MenuItem menuItem = menuItemService.updateCategory(
                parseLong(args.get(0)),
                new UpdateMenuItemCategoryRequest(remove ? null : args.get(1).trim()),
                actor.getId()
        );
        return "Menu item category updated:\n" + formatMenuItem(menuItem);
    }

    private String startMenuPhotoUpload(LunchUser actor, String rawArgs) {
        Long menuItemId = parseLong(requireSingle(rawArgs));
        MenuItem menuItem = menuItemService.getRequired(menuItemId);
        if (actor.getTelegramUserId() == null) {
            throw new BadRequestException("Actor telegram user id is required");
        }

        menuPhotoUploadStateService.remember(actor.getTelegramUserId(), menuItem.getId());
        return "Send photo for menu item #" + menuItem.getId();
    }

    private String cancelMenuPhotoUpload(LunchUser actor) {
        menuPhotoUploadStateService.clear(actor.getTelegramUserId());
        return "Menu photo upload cancelled";
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

    private String openVote(LunchUser actor) {
        RestaurantVoteSessionResponse session = restaurantVoteSessionService.openSession(null, actor.getId());
        return "Voting opened. Session ID: " + session.id();
    }

    private String closeVote(LunchUser actor) {
        RestaurantVoteSession session = restaurantVoteSessionService.getTodaySession()
                .orElseThrow(() -> new NotFoundException("Today's restaurant vote session not found"));
        if (session.getStatus() != RestaurantVoteSessionStatus.OPEN) {
            return "Bugungi voting allaqachon yopilgan.";
        }

        RestaurantVoteSessionResponse response = restaurantVoteSessionService.closeSession(session.getId(), actor.getId());
        return "Voting closed. Status: " + response.status();
    }

    private String openDefaultOrder(LunchUser actor) {
        OrderSession existing = orderSessionService.getTodaySession().orElse(null);
        if (existing != null) {
            return "Today's order session already exists. Session ID: " + existing.getId();
        }

        OrderSession session = orderSessionService.openSession(null, actor.getId());
        return "Order opened. Session ID: " + session.getId();
    }

    private String closeOrderNow(LunchUser actor) {
        OrderSession session = orderSessionService.getTodayOpenSession().orElse(null);
        if (session == null) {
            return "Bu order allaqachon yopilgan.";
        }

        orderSessionService.closeSession(session.getId(), actor.getId());
        return "Order closed. Session ID: " + session.getId();
    }

    private String buildAdminStatus(LunchUser actor) {
        RestaurantVoteSession voteSession = restaurantVoteSessionService.getTodaySession().orElse(null);
        OrderSession orderSession = orderSessionService.getTodaySession().orElse(null);

        String votingSection;
        if (voteSession == null) {
            votingSection = "Voting:\nStatus: NOT_OPENED";
        } else {
            RestaurantVoteSessionResponse response = toResponse(voteSession, actor.getId());
            String votes = response.voteCounts().stream()
                    .map(count -> "- " + count.restaurantName() + " — " + count.voteCount())
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse("-");
            votingSection = """
                    Voting:
                    Status: %s
                    Winner: %s
                    Votes:
                    %s
                    """.formatted(
                    response.status(),
                    response.winnerRestaurantName() == null ? "-" : response.winnerRestaurantName(),
                    votes
            );
        }

        String orderSection;
        if (orderSession == null) {
            orderSection = "Order:\nStatus: NOT_OPENED";
        } else {
            SessionSummaryResponse summary = summaryService.buildSummary(orderSession.getId());
            orderSection = """
                    Order:
                    Status: %s
                    Restoran: %s
                    Deadline: %s
                    Orders: %d
                    Skipped: %d
                    Not responded: %d
                    """.formatted(
                    orderSession.getStatus(),
                    orderSession.getRestaurant().getName(),
                    orderSession.getDeadlineAt().toLocalTime(),
                    summary.orderedCount(),
                    summary.skippedCount(),
                    summary.noResponseCount()
            );
        }

        return """
                📌 Bugungi status

                %s

                %s

                Admin commands:
                - /vote_control
                - /order_control
                """.formatted(votingSection, orderSection);
    }

    private String recalculateCurrentSession(LunchUser actor, String rawArgs) {
        OrderSession session = currentSession();
        boolean allowConfirmed = !rawArgs.isBlank() && "allow_confirmed".equalsIgnoreCase(rawArgs.trim());
        SessionRecalculationResponse recalculated = orderSessionService.recalculateSession(
                session.getId(),
                new RecalculateSessionRequest(RecalculationMode.FULL_PRICE_REBUILD, allowConfirmed),
                actor.getId());
        return "Current session recalculated:\nSession ID: " + recalculated.sessionId() + "\nStatus: " + session.getStatus();
    }

    private OrderSession currentSession() {
        return orderSessionService.getTodaySession().orElseThrow(() -> new NotFoundException("Today's order session not found"));
    }

    private RestaurantVoteSessionResponse toResponse(RestaurantVoteSession session, Long actorUserId) {
        if (session.getVoteDate() != null && restaurantVoteSessionService.getTodaySession().map(RestaurantVoteSession::getId).filter(session.getId()::equals).isPresent()) {
            return restaurantVoteSessionService.getTodayResponse(actorUserId);
        }

        return new RestaurantVoteSessionResponse(
                session.getId(),
                session.getVoteDate(),
                session.getStatus(),
                session.getStartedAt(),
                session.getDeadlineAt(),
                session.getWinnerRestaurant() == null ? null : session.getWinnerRestaurant().getId(),
                session.getWinnerRestaurant() == null ? null : session.getWinnerRestaurant().getName(),
                session.getGroupMessageId(),
                List.of(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
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
                fallback(restaurant.getPhoneNumber()),
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
                category=%s
                imageUrl=%s
                telegramImageFileId=%s
                """.formatted(
                menuItem.getId(),
                menuItem.getName(),
                menuItem.getRestaurant().getId(),
                menuItem.isActive(),
                MoneyUtils.formatUzs(menuItem.getPrice()),
                menuItem.getContainerRequired(),
                menuItem.getContainerPriceOverride() == null ? "-" : MoneyUtils.formatUzs(menuItem.getContainerPriceOverride()),
                fallback(menuItem.getCategory()),
                fallback(menuItem.getImageUrl()),
                fallback(menuItem.getTelegramImageFileId()));
    }

    private String formatMenuPageItem(MenuItem menuItem) {
        String imageIndicator = menuItem.getImageUrl() != null || menuItem.getTelegramImageFileId() != null ? " 📷" : "";
        String activeState = menuItem.isActive() ? "active" : "inactive";
        return "#%d%s %s — %s | %s | %s | category=%s".formatted(
                menuItem.getId(),
                imageIndicator,
                menuItem.getName(),
                MoneyUtils.formatUzs(menuItem.getPrice()),
                activeState,
                containerSummary(menuItem),
                fallback(menuItem.getCategory())
        );
    }

    private String containerSummary(MenuItem menuItem) {
        if (Boolean.FALSE.equals(menuItem.getContainerRequired())) {
            return "container=no";
        }
        if (menuItem.getContainerPriceOverride() != null) {
            return "container=" + MoneyUtils.formatUzs(menuItem.getContainerPriceOverride());
        }
        if (Boolean.TRUE.equals(menuItem.getContainerRequired()) || menuItem.getRestaurant().isContainerEnabled()) {
            return "container=restaurant-default";
        }
        return "container=no";
    }

    private AdminCommandResponse response(String text) {
        return new AdminCommandResponse(text, null);
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
