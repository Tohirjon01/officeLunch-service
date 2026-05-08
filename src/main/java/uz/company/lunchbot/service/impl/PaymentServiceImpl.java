package uz.company.lunchbot.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.company.lunchbot.bot.keyboard.TelegramKeyboards;
import uz.company.lunchbot.bot.message.TelegramMessages;
import uz.company.lunchbot.config.LunchProperties;
import uz.company.lunchbot.dto.response.PaymentResponse;
import uz.company.lunchbot.dto.response.PaymentSummaryResponse;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.Payment;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.AuditAction;
import uz.company.lunchbot.enums.PaymentMethod;
import uz.company.lunchbot.enums.PaymentRecordStatus;
import uz.company.lunchbot.enums.PaymentStatus;
import uz.company.lunchbot.enums.UserLanguage;
import uz.company.lunchbot.enums.UserOrderStatus;
import uz.company.lunchbot.exception.BadRequestException;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.repository.PaymentRepository;
import uz.company.lunchbot.repository.UserOrderRepository;
import uz.company.lunchbot.security.AdminAccessService;
import uz.company.lunchbot.service.AuditService;
import uz.company.lunchbot.service.NotificationService;
import uz.company.lunchbot.service.PaymentService;
import uz.company.lunchbot.service.UserService;
import uz.company.lunchbot.util.MoneyUtils;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserOrderRepository userOrderRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final TelegramMessages telegramMessages;
    private final TelegramKeyboards telegramKeyboards;
    private final AdminAccessService adminAccessService;
    private final AuditService auditService;
    private final LunchProperties lunchProperties;
    private final Clock clock;

    @Override
    public Payment getRequired(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + id));
    }

    @Override
    @Transactional
    public void initializePaymentsForClosedSession(OrderSession session) {
        if (lunchProperties.payment() == null || !lunchProperties.payment().enabled()) {
            return;
        }

        List<UserOrder> orderedOrders = userOrderRepository.findAllByOrderSessionIdAndStatus(session.getId(), UserOrderStatus.ORDERED);
        for (UserOrder order : orderedOrders) {
            Payment payment = paymentRepository.findByUserOrderId(order.getId()).orElseGet(Payment::new);
            boolean created = payment.getId() == null;
            payment.setUserOrder(order);
            payment.setUser(order.getUser());
            payment.setOrderSession(session);
            payment.setAmount(defaultMoney(order.getFinalPrice()));
            payment.setPaymentMethod(payment.getPaymentMethod() == null ? PaymentMethod.CARD : payment.getPaymentMethod());
            payment.setStatus(payment.getStatus() == null ? PaymentRecordStatus.WAITING_PAYMENT : payment.getStatus());
            Payment saved = paymentRepository.save(payment);
            order.setPaymentStatus(saved.getStatus() == PaymentRecordStatus.PAID ? PaymentStatus.PAID : PaymentStatus.UNPAID);
            userOrderRepository.save(order);

            if (created) {
                auditService.log(AuditAction.PAYMENT_CREATED, session.getId(), order.getUser().getId(), null, saved.getAmount());
            }
            if (saved.getStatus() != PaymentRecordStatus.PAID) {
                sendPaymentReport(saved);
            }
        }
    }

    @Override
    @Transactional
    public void submitReceipt(Long telegramUserId, String receiptFileId, Long receiptMessageId) {
        LunchUser user = userService.getApprovedUserByTelegramUserId(telegramUserId);
        Payment payment = paymentRepository.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
                        user.getId(),
                        EnumSet.of(PaymentRecordStatus.WAITING_PAYMENT, PaymentRecordStatus.REJECTED)
                )
                .orElseThrow(() -> new BadRequestException(telegramMessages.noPendingPaymentForReceipt()));

        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setReceiptFileId(receiptFileId);
        payment.setReceiptMessageId(receiptMessageId);
        payment.setStatus(PaymentRecordStatus.WAITING_APPROVAL);
        payment.setAdminComment(null);
        payment.setPaidAt(null);
        Payment saved = paymentRepository.save(payment);
        auditService.log(AuditAction.PAYMENT_RECEIPT_SENT, saved.getOrderSession().getId(), user.getId(), null, saved.getReceiptFileId());

        notifyAdminsAboutReceipt(saved);
        notificationService.sendPrivateText(user.getPrivateChatId(), telegramMessages.paymentReceiptAcceptedForReview(), null);
    }

    @Override
    @Transactional
    public void declareCashPayment(Long telegramUserId, Long paymentId) {
        LunchUser user = userService.getApprovedUserByTelegramUserId(telegramUserId);
        Payment payment = getRequired(paymentId);
        if (!payment.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You cannot modify another user's payment");
        }
        if (!EnumSet.of(PaymentRecordStatus.WAITING_PAYMENT, PaymentRecordStatus.REJECTED).contains(payment.getStatus())) {
            throw new BadRequestException("Cash payment can only be declared for waiting or rejected payments");
        }

        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentRecordStatus.CASH_DECLARED);
        payment.setReceiptFileId(null);
        payment.setReceiptMessageId(null);
        payment.setAdminComment(null);
        Payment saved = paymentRepository.save(payment);
        auditService.log(AuditAction.PAYMENT_CASH_DECLARED, saved.getOrderSession().getId(), user.getId(), null, saved.getAmount());

        notifyAdminsAboutCashDeclaration(saved);
        notificationService.sendPrivateText(user.getPrivateChatId(), telegramMessages.cashPaymentDeclared(), null);
    }

    @Override
    @Transactional
    public PaymentResponse approvePayment(Long paymentId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        Payment payment = getRequired(paymentId);
        payment.setStatus(PaymentRecordStatus.PAID);
        payment.setPaidAt(LocalDateTime.now(clock));
        payment.setAdminComment(null);
        payment.getUserOrder().setPaymentStatus(PaymentStatus.PAID);
        userOrderRepository.save(payment.getUserOrder());
        Payment saved = paymentRepository.save(payment);
        auditService.log(AuditAction.PAYMENT_APPROVED, saved.getOrderSession().getId(), actorUserId, null, saved.getId());
        notifyUserAboutPayment(saved, telegramMessages.paymentApproved());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse rejectPayment(Long paymentId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        Payment payment = getRequired(paymentId);
        payment.setStatus(PaymentRecordStatus.REJECTED);
        payment.setPaidAt(null);
        payment.getUserOrder().setPaymentStatus(PaymentStatus.UNPAID);
        userOrderRepository.save(payment.getUserOrder());
        Payment saved = paymentRepository.save(payment);
        auditService.log(AuditAction.PAYMENT_REJECTED, saved.getOrderSession().getId(), actorUserId, null, saved.getId());
        notifyUserAboutPayment(saved, telegramMessages.paymentRejected(), telegramKeyboards.cashPaymentButton(saved.getId()));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse markCashPaid(Long paymentId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        Payment payment = getRequired(paymentId);
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentRecordStatus.PAID);
        payment.setPaidAt(LocalDateTime.now(clock));
        payment.setAdminComment(null);
        payment.getUserOrder().setPaymentStatus(PaymentStatus.PAID);
        userOrderRepository.save(payment.getUserOrder());
        Payment saved = paymentRepository.save(payment);
        auditService.log(AuditAction.PAYMENT_CASH_MARKED_PAID, saved.getOrderSession().getId(), actorUserId, null, saved.getId());
        notifyUserAboutPayment(saved, telegramMessages.cashPaymentApproved());
        return toResponse(saved);
    }

    @Override
    public List<PaymentResponse> getPaymentsForSession(Long sessionId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        return paymentRepository.findAllByOrderSessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PaymentSummaryResponse getPaymentSummary(Long sessionId, Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);
        List<PaymentResponse> payments = getPaymentsForSession(sessionId, actorUserId);

        BigDecimal expected = payments.stream().map(PaymentResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<PaymentResponse> paid = filter(payments, PaymentRecordStatus.PAID);
        BigDecimal paidAmount = paid.stream().map(PaymentResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PaymentSummaryResponse(
                sessionId,
                expected,
                paidAmount,
                expected.subtract(paidAmount),
                filter(payments, PaymentRecordStatus.WAITING_PAYMENT),
                waitingApprovalPayments(payments),
                filter(payments, PaymentRecordStatus.CASH_DECLARED),
                paid,
                filter(payments, PaymentRecordStatus.REJECTED)
        );
    }

    @Override
    public String buildPaymentSummaryText(Long sessionId, Long actorUserId) {
        PaymentSummaryResponse summary = getPaymentSummary(sessionId, actorUserId);
        return """
                Bugungi to'lov holati:

                Total expected: %s
                Total paid: %s
                Total unpaid: %s

                Waiting:
                %s

                Waiting approval:
                %s

                Cash:
                %s

                Paid:
                %s

                Rejected:
                %s
                """.formatted(
                MoneyUtils.formatUzs(summary.totalExpectedAmount()),
                MoneyUtils.formatUzs(summary.totalPaidAmount()),
                MoneyUtils.formatUzs(summary.totalUnpaidAmount()),
                lines(summary.waitingPayments(), false),
                lines(summary.receiptSentPayments(), false),
                lines(summary.cashDeclaredPayments(), false),
                lines(summary.paidPayments(), true),
                lines(summary.rejectedPayments(), false)
        );
    }

    @Override
    public String getUserPaymentStatusText(Long telegramUserId, Long paymentId) {
        LunchUser user = userService.getApprovedUserByTelegramUserId(telegramUserId);
        Payment payment = getRequired(paymentId);
        if (!payment.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You cannot view another user's payment");
        }
        return telegramMessages.paymentStatus(payment.getStatus(), languageOf(user));
    }

    private List<PaymentResponse> filter(List<PaymentResponse> payments, PaymentRecordStatus status) {
        return payments.stream().filter(payment -> payment.status() == status).toList();
    }

    private List<PaymentResponse> waitingApprovalPayments(List<PaymentResponse> payments) {
        return payments.stream()
                .filter(payment -> payment.status() == PaymentRecordStatus.WAITING_APPROVAL
                        || payment.status() == PaymentRecordStatus.RECEIPT_SENT)
                .toList();
    }

    private String lines(List<PaymentResponse> payments, boolean includeMethod) {
        if (payments.isEmpty()) {
            return "-";
        }
        return payments.stream()
                .map(payment -> "- " + payment.userDisplayName() + " — " + MoneyUtils.formatUzs(payment.amount())
                        + (includeMethod ? " " + payment.paymentMethod() : ""))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("-");
    }

    private void sendPaymentReport(Payment payment) {
        LunchUser user = payment.getUser();
        if (user.getPrivateChatId() == null) {
            return;
        }
        notificationService.sendPrivateText(
                user.getPrivateChatId(),
                buildPaymentReport(payment),
                telegramKeyboards.cashPaymentButton(payment.getId())
        );
    }

    private String buildPaymentReport(Payment payment) {
        UserOrder order = payment.getUserOrder();
        String mealName = order.getMenuItem() == null ? "N/A" : order.getMenuItem().getName();
        int quantity = order.getQuantity() == null ? 1 : order.getQuantity();
        return """
                Bugungi buyurtmangiz bo'yicha to'lov:

                Restoran: %s

                Buyurtma:
                - %s x%d — %s
                - Idish — %s
                - Dostavka ulushi — %s

                Total Price: %s

                Payment Instructions:
                Card Number: %s
                Card Owner: %s

                To'lov qilganingizdan keyin "Upload Receipt" tugmasini bosing va chek rasmini shu botga yuboring.
                Agar naqd bermoqchi bo'lsangiz, "Naqd to'layman" tugmasini bosing.
                """.formatted(
                payment.getOrderSession().getRestaurant().getName(),
                mealName,
                quantity,
                MoneyUtils.formatUzs(lineTotal(order.getFoodPrice(), order.getQuantity())),
                MoneyUtils.formatUzs(lineTotal(order.getContainerPrice(), order.getQuantity())),
                MoneyUtils.formatUzs(order.getDeliveryShare()),
                MoneyUtils.formatUzs(payment.getAmount()),
                lunchProperties.payment().cardNumber(),
                lunchProperties.payment().cardOwnerName()
        );
    }

    private void notifyAdminsAboutReceipt(Payment payment) {
        String mealName = payment.getUserOrder().getMenuItem() == null ? "N/A" : payment.getUserOrder().getMenuItem().getName();
        String message = """
                Receipt waiting approval

                User: %s
                Phone: %s
                Total amount: %s
                Meal: %s
                """.formatted(
                payment.getUser().getDisplayName(),
                blankOrDash(payment.getUser().getPhoneNumber()),
                MoneyUtils.formatUzs(payment.getAmount()),
                mealName
        );

        adminNotificationChatIds().forEach(adminChatId -> {
                    notificationService.sendPrivateText(
                            adminChatId,
                            message,
                            telegramKeyboards.cardPaymentAdminActions(payment.getId())
                    );
                    notificationService.copyPrivateMessage(
                            adminChatId,
                            payment.getUser().getPrivateChatId(),
                            payment.getReceiptMessageId()
                    );
                });
    }

    private void notifyAdminsAboutCashDeclaration(Payment payment) {
        String mealName = payment.getUserOrder().getMenuItem() == null ? "N/A" : payment.getUserOrder().getMenuItem().getName();
        String message = """
                User wants to pay cash

                User: %s
                Phone: %s
                Amount: %s
                Meal: %s
                """.formatted(
                payment.getUser().getDisplayName(),
                blankOrDash(payment.getUser().getPhoneNumber()),
                MoneyUtils.formatUzs(payment.getAmount()),
                mealName
        );

        adminNotificationChatIds().forEach(adminChatId -> notificationService.sendPrivateText(
                        adminChatId,
                        message,
                        telegramKeyboards.cashPaymentAdminActions(payment.getId())
                ));
    }

    private void notifyUserAboutPayment(Payment payment, String message) {
        notifyUserAboutPayment(payment, message, null);
    }

    private void notifyUserAboutPayment(Payment payment, String message, Object keyboard) {
        if (payment.getUser().getPrivateChatId() != null) {
            notificationService.sendPrivateText(payment.getUser().getPrivateChatId(), message, keyboard);
        }
    }

    private List<Long> adminNotificationChatIds() {
        LinkedHashSet<Long> chatIds = userService.getApprovedAdmins().stream()
                .map(LunchUser::getPrivateChatId)
                .filter(Objects::nonNull)
                .filter(chatId -> chatId > 0)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Long bootstrapAdminChatId = lunchProperties.bootstrap() == null ? null : lunchProperties.bootstrap().superAdminPrivateChatId();
        if (bootstrapAdminChatId != null && bootstrapAdminChatId > 0) {
            chatIds.add(bootstrapAdminChatId);
        }

        return List.copyOf(chatIds);
    }

    private UserLanguage languageOf(LunchUser user) {
        return user == null || user.getLanguage() == null ? UserLanguage.UZ : user.getLanguage();
    }

    private String blankOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getUserOrder().getId(),
                payment.getUser().getId(),
                payment.getUser().getDisplayName(),
                payment.getOrderSession().getId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getReceiptFileId(),
                payment.getReceiptMessageId(),
                payment.getAdminComment(),
                payment.getPaidAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal lineTotal(BigDecimal unitPrice, Integer quantity) {
        return defaultMoney(unitPrice).multiply(BigDecimal.valueOf(quantity == null ? 0L : quantity.longValue()));
    }
}
