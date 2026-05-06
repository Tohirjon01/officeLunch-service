package uz.company.lunchbot.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.company.lunchbot.bot.keyboard.TelegramKeyboards;
import uz.company.lunchbot.bot.message.TelegramMessages;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.dto.request.OpenRestaurantVoteSessionRequest;
import uz.company.lunchbot.dto.response.RestaurantVoteCountResponse;
import uz.company.lunchbot.dto.response.RestaurantVoteSessionResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.Restaurant;
import uz.company.lunchbot.entity.RestaurantVote;
import uz.company.lunchbot.entity.RestaurantVoteSession;
import uz.company.lunchbot.enums.AuditAction;
import uz.company.lunchbot.enums.RestaurantVoteSessionStatus;
import uz.company.lunchbot.exception.BadRequestException;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.repository.RestaurantRepository;
import uz.company.lunchbot.repository.RestaurantVoteRepository;
import uz.company.lunchbot.repository.RestaurantVoteSessionRepository;
import uz.company.lunchbot.security.AdminAccessService;
import uz.company.lunchbot.service.AuditService;
import uz.company.lunchbot.service.MenuItemService;
import uz.company.lunchbot.service.NotificationService;
import uz.company.lunchbot.service.OrderSessionService;
import uz.company.lunchbot.service.RestaurantService;
import uz.company.lunchbot.service.RestaurantVoteSessionService;
import uz.company.lunchbot.service.UserService;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantVoteSessionServiceImpl implements RestaurantVoteSessionService {

    private final RestaurantVoteSessionRepository restaurantVoteSessionRepository;
    private final RestaurantVoteRepository restaurantVoteRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantService restaurantService;
    private final UserService userService;
    private final OrderSessionService orderSessionService;
    private final MenuItemService menuItemService;
    private final NotificationService notificationService;
    private final TelegramMessages telegramMessages;
    private final TelegramKeyboards telegramKeyboards;
    private final AdminAccessService adminAccessService;
    private final AuditService auditService;
    private final LunchProperties lunchProperties;
    private final Clock clock;

    @Override
    public RestaurantVoteSession getRequired(Long id) {
        return restaurantVoteSessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Restaurant vote session not found: " + id));
    }

    @Override
    public Optional<RestaurantVoteSession> getTodaySession() {
        return restaurantVoteSessionRepository.findFirstByVoteDateOrderByCreatedAtDesc(LocalDate.now(clock));
    }

    @Override
    public RestaurantVoteSessionResponse getTodayResponse(Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        RestaurantVoteSession session = getTodaySession()
                .orElseThrow(() -> new NotFoundException("Today's restaurant vote session not found"));
        return toResponse(session);
    }

    @Override
    @Transactional
    public RestaurantVoteSessionResponse openSession(OpenRestaurantVoteSessionRequest request, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        LocalDate voteDate = request != null && request.voteDate() != null ? request.voteDate() : LocalDate.now(clock);
        LocalDateTime startedAt = LocalDateTime.now(clock);
        LocalTime deadlineTime = request != null && request.deadlineTime() != null
                ? request.deadlineTime()
                : startedAt.toLocalTime().plusMinutes(votingDurationMinutes());

        restaurantVoteSessionRepository.findFirstByVoteDateAndStatusOrderByCreatedAtDesc(voteDate, RestaurantVoteSessionStatus.OPEN)
                .ifPresent(existing -> {
                    throw new BadRequestException("An open restaurant vote session already exists for date " + voteDate);
                });

        List<Restaurant> activeRestaurants = activeRestaurants();
        if (activeRestaurants.isEmpty()) {
            throw new BadRequestException("No active restaurants are available for voting");
        }

        LunchUser actor = adminAccessService.getActor(actorUserId);
        RestaurantVoteSession session = new RestaurantVoteSession();
        session.setVoteDate(voteDate);
        session.setStatus(RestaurantVoteSessionStatus.OPEN);
        session.setStartedAt(startedAt);
        session.setDeadlineAt(LocalDateTime.of(voteDate, deadlineTime));
        session.setCreatedBy(actor);

        RestaurantVoteSession saved = restaurantVoteSessionRepository.saveAndFlush(session);

        log.info("restaurant_voting_send_target groupChatId={}", lunchProperties.groupChatId());

        Long groupMessageId = notificationService.sendGroupText(
                buildVotingMessage(saved, zeroCounts(activeRestaurants)),
                telegramKeyboards.restaurantVoting(activeRestaurants, saved.getId())
        );
        saved.setGroupMessageId(groupMessageId);
        saved = restaurantVoteSessionRepository.save(saved);

        auditService.log(AuditAction.RESTAURANT_VOTE_SESSION_OPENED, null, actorUserId, null, voteSessionState(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public RestaurantVoteSessionResponse closeSession(Long id, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        RestaurantVoteSession session = getRequired(id);
        if (session.getStatus() == RestaurantVoteSessionStatus.CLOSED && session.getWinnerRestaurant() != null) {
            return toResponse(session);
        }
        if (session.getStatus() != RestaurantVoteSessionStatus.OPEN) {
            throw new BadRequestException("Only open restaurant vote sessions can be closed");
        }

        List<RestaurantVoteCountResponse> counts = voteCounts(session);
        updateGroupMessageSafely(session, counts);

        session.setStatus(RestaurantVoteSessionStatus.CLOSED);
        restaurantVoteSessionRepository.save(session);
        auditService.log(AuditAction.RESTAURANT_VOTE_SESSION_CLOSED, null, actorUserId, null, voteSessionState(session));

        List<RestaurantVoteCountResponse> winners = winningCounts(counts);
        if (winners.size() == 1) {
            Restaurant winner = restaurantService.getRequired(winners.getFirst().restaurantId());
            return completeWinnerSelection(session, winner, actorUserId);
        }

        session.setStatus(RestaurantVoteSessionStatus.TIE_WAITING_ADMIN);
        restaurantVoteSessionRepository.save(session);
        auditService.log(AuditAction.RESTAURANT_VOTE_TIE_DETECTED, null, actorUserId, null, counts);
        notifyAdminsAboutTie(session, winners.isEmpty() ? counts : winners);
        return toResponse(session);
    }

    @Override
    @Transactional
    public RestaurantVoteSessionResponse chooseWinner(Long id, Long restaurantId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        RestaurantVoteSession session = getRequired(id);
        if (session.getStatus() != RestaurantVoteSessionStatus.TIE_WAITING_ADMIN && session.getStatus() != RestaurantVoteSessionStatus.OPEN) {
            throw new BadRequestException("Winner can only be chosen for an open or tie-waiting restaurant vote session");
        }

        Restaurant restaurant = restaurantService.getRequired(restaurantId);
        if (!restaurant.isActive()) {
            throw new BadRequestException("Inactive restaurant cannot be selected as winner");
        }

        return completeWinnerSelection(session, restaurant, actorUserId);
    }

    @Override
    @Transactional
    public RestaurantVoteSessionResponse extendDeadline(Long id, int minutes, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        RestaurantVoteSession session = getRequired(id);
        if (session.getStatus() != RestaurantVoteSessionStatus.OPEN) {
            throw new BadRequestException("Bu voting allaqachon yopilgan.");
        }
        if (minutes <= 0) {
            throw new BadRequestException("Minutes must be greater than zero");
        }

        session.setDeadlineAt(session.getDeadlineAt().plusMinutes(minutes));
        RestaurantVoteSession saved = restaurantVoteSessionRepository.save(session);
        log.info("vote_deadline_extended voteSessionId={} minutes={} newDeadline={}", saved.getId(), minutes, saved.getDeadlineAt());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public RestaurantVoteSessionResponse reduceDeadline(Long id, int minutes, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        RestaurantVoteSession session = getRequired(id);
        if (session.getStatus() != RestaurantVoteSessionStatus.OPEN) {
            throw new BadRequestException("Bu voting allaqachon yopilgan.");
        }
        if (minutes <= 0) {
            throw new BadRequestException("Minutes must be greater than zero");
        }

        LocalDateTime newDeadline = session.getDeadlineAt().minusMinutes(minutes);
        if (!newDeadline.isAfter(LocalDateTime.now(clock))) {
            throw new BadRequestException("Deadline hozirgi vaqtdan oldin bo'lishi mumkin emas.");
        }

        session.setDeadlineAt(newDeadline);
        RestaurantVoteSession saved = restaurantVoteSessionRepository.save(session);
        log.info("vote_deadline_reduced voteSessionId={} minutes={} newDeadline={}", saved.getId(), minutes, saved.getDeadlineAt());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void castVote(Long telegramUserId, Long voteSessionId, Long restaurantId) {
        LunchUser user = userService.getApprovedUserByTelegramUserId(telegramUserId);
        RestaurantVoteSession session = getRequired(voteSessionId);
        if (session.getStatus() != RestaurantVoteSessionStatus.OPEN) {
            throw new BadRequestException("Ovoz berish yopilgan.");
        }
        if (session.getDeadlineAt().isBefore(LocalDateTime.now(clock))) {
            throw new BadRequestException("Ovoz berish yopilgan.");
        }

        Restaurant restaurant = restaurantService.getRequired(restaurantId);
        if (!restaurant.isActive()) {
            throw new BadRequestException("Inactive restaurant cannot be voted for");
        }

        Optional<RestaurantVote> existing = restaurantVoteRepository.findByVoteSessionIdAndUserId(session.getId(), user.getId());
        if (existing.isPresent()) {
            RestaurantVote vote = existing.get();
            if (vote.getRestaurant().getId().equals(restaurant.getId())) {
                updateGroupMessageSafely(session, voteCounts(session));
                return;
            }
            Restaurant oldRestaurant = vote.getRestaurant();
            vote.setRestaurant(restaurant);
            restaurantVoteRepository.save(vote);
            auditService.log(
                    AuditAction.RESTAURANT_VOTE_CHANGED,
                    null,
                    user.getId(),
                    Map.of("from", oldRestaurant.getId(), "to", restaurant.getId()),
                    null
            );
        } else {
            RestaurantVote vote = new RestaurantVote();
            vote.setVoteSession(session);
            vote.setUser(user);
            vote.setRestaurant(restaurant);
            restaurantVoteRepository.save(vote);
            auditService.log(AuditAction.RESTAURANT_VOTE_CAST, null, user.getId(), null, Map.of("restaurantId", restaurant.getId()));
        }

        updateGroupMessageSafely(session, voteCounts(session));
    }

    private RestaurantVoteSessionResponse completeWinnerSelection(RestaurantVoteSession session, Restaurant winner, Long actorUserId) {
        if (session.getStatus() == RestaurantVoteSessionStatus.CLOSED
                && session.getWinnerRestaurant() != null
                && session.getWinnerRestaurant().getId().equals(winner.getId())) {
            return toResponse(session);
        }

        session.setWinnerRestaurant(winner);
        session.setStatus(RestaurantVoteSessionStatus.CLOSED);
        RestaurantVoteSession saved = restaurantVoteSessionRepository.save(session);

        log.info("voting_winner restaurantId={} restaurantName={}", winner.getId(), winner.getName());

        auditService.log(
                AuditAction.RESTAURANT_VOTE_WINNER_SELECTED,
                null,
                actorUserId,
                null,
                Map.of("voteSessionId", saved.getId(), "winnerRestaurantId", winner.getId())
        );

        boolean reusedOpenSession = orderSessionService.getTodayOpenSession()
                .filter(existing -> existing.getRestaurant().getId().equals(winner.getId()))
                .isPresent();
        OrderSession orderSession = orderSessionService.openTodaySessionForRestaurantIfAbsent(winner.getId(), actorUserId);

        log.info(
                "order_session_opened_from_vote sessionId={} restaurantId={} restaurantName={}",
                orderSession.getId(),
                orderSession.getRestaurant().getId(),
                orderSession.getRestaurant().getName()
        );

        notificationService.sendGroupText("✅ Bugungi restoran: " + orderSession.getRestaurant().getName(), null);
        if (!reusedOpenSession) {
            notificationService.sendGroupText(
                    telegramMessages.todayMenu(orderSession, menuItemService.getActiveMenu(orderSession.getRestaurant().getId())),
                    null
            );
            notificationService.sendGroupOpenAnnouncement(orderSession);
        }

        return toResponse(saved);
    }

    private void notifyAdminsAboutTie(RestaurantVoteSession session, List<RestaurantVoteCountResponse> tiedCounts) {
        List<Restaurant> tiedRestaurants = tiedCounts.stream()
                .map(count -> restaurantService.getRequired(count.restaurantId()))
                .toList();
        String message = buildTieMessage(tiedCounts);
        userService.getApprovedAdmins().stream()
                .filter(admin -> admin.getPrivateChatId() != null)
                .forEach(admin -> notificationService.sendPrivateText(
                        admin.getPrivateChatId(),
                        message,
                        telegramKeyboards.restaurantWinnerSelection(session.getId(), tiedRestaurants)
                ));
    }

    private void updateGroupMessageSafely(RestaurantVoteSession session, List<RestaurantVoteCountResponse> counts) {
        try {
            notificationService.editGroupText(
                    session.getGroupMessageId(),
                    buildVotingMessage(session, counts),
                    telegramKeyboards.restaurantVoting(activeRestaurants(), session.getId())
            );
        } catch (Exception exception) {
            log.warn("restaurant_vote_message_update_failed sessionId={} reason={}", session.getId(), exception.getMessage());
        }
    }

    private List<Restaurant> activeRestaurants() {
        return restaurantRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(Restaurant::isActive)
                .sorted(Comparator.comparing(Restaurant::getName))
                .toList();
    }

    private List<RestaurantVoteCountResponse> zeroCounts(List<Restaurant> restaurants) {
        return restaurants.stream()
                .map(restaurant -> new RestaurantVoteCountResponse(restaurant.getId(), restaurant.getName(), 0))
                .toList();
    }

    private List<RestaurantVoteCountResponse> voteCounts(RestaurantVoteSession session) {
        Map<Long, Long> counts = new LinkedHashMap<>();
        restaurantVoteRepository.findAllByVoteSessionId(session.getId()).forEach(vote ->
                counts.merge(vote.getRestaurant().getId(), 1L, Long::sum)
        );

        return activeRestaurants().stream()
                .map(restaurant -> new RestaurantVoteCountResponse(
                        restaurant.getId(),
                        restaurant.getName(),
                        counts.getOrDefault(restaurant.getId(), 0L)
                ))
                .toList();
    }

    private List<RestaurantVoteCountResponse> winningCounts(List<RestaurantVoteCountResponse> counts) {
        long maxVotes = counts.stream().mapToLong(RestaurantVoteCountResponse::voteCount).max().orElse(0);
        return counts.stream().filter(count -> count.voteCount() == maxVotes).toList();
    }

    private RestaurantVoteSessionResponse toResponse(RestaurantVoteSession session) {
        return new RestaurantVoteSessionResponse(
                session.getId(),
                session.getVoteDate(),
                session.getStatus(),
                session.getStartedAt(),
                session.getDeadlineAt(),
                session.getWinnerRestaurant() == null ? null : session.getWinnerRestaurant().getId(),
                session.getWinnerRestaurant() == null ? null : session.getWinnerRestaurant().getName(),
                session.getGroupMessageId(),
                voteCounts(session),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private String buildVotingMessage(RestaurantVoteSession session, List<RestaurantVoteCountResponse> counts) {
        String options = counts.stream()
                .map(count -> count.restaurantName() + " — " + count.voteCount() + " ovoz")
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No active restaurants.");

        return """
                🗳 Bugungi restoran tanlang

                %s

                Ovoz berish uchun avval botga /start bosib ro'yxatdan o'ting.
                ⏰ Voting deadline: %s
                """.formatted(options, session.getDeadlineAt().toLocalTime());
    }

    private String buildTieMessage(List<RestaurantVoteCountResponse> counts) {
        String options = counts.stream()
                .map(count -> count.restaurantName() + " — " + count.voteCount() + " ovoz")
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No restaurants.");
        return "Ovozlar teng bo'ldi. Restoranni tanlang:\n\n" + options;
    }

    private Map<String, Object> voteSessionState(RestaurantVoteSession session) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("voteSessionId", session.getId());
        state.put("voteDate", session.getVoteDate());
        state.put("status", session.getStatus());
        state.put("deadlineAt", session.getDeadlineAt());
        state.put("winnerRestaurantId", session.getWinnerRestaurant() == null ? null : session.getWinnerRestaurant().getId());
        state.put("groupMessageId", session.getGroupMessageId());
        return state;
    }

    private int votingDurationMinutes() {
        return lunchProperties.restaurantVoting() == null || lunchProperties.restaurantVoting().durationMinutes() <= 0
                ? 5
                : lunchProperties.restaurantVoting().durationMinutes();
    }
}
