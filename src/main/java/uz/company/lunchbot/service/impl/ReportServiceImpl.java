package uz.company.lunchbot.service.impl;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.entity.Payment;
import uz.company.lunchbot.entity.UserOrder;
import uz.company.lunchbot.enums.PaymentRecordStatus;
import uz.company.lunchbot.enums.UserOrderStatus;
import uz.company.lunchbot.exception.NotFoundException;
import uz.company.lunchbot.repository.PaymentRepository;
import uz.company.lunchbot.repository.UserOrderRepository;
import uz.company.lunchbot.security.AdminAccessService;
import uz.company.lunchbot.service.OrderSessionService;
import uz.company.lunchbot.service.ReportService;
import uz.company.lunchbot.util.MoneyUtils;

@Service
public class ReportServiceImpl implements ReportService {

    private final OrderSessionService orderSessionService;
    private final UserOrderRepository userOrderRepository;
    private final PaymentRepository paymentRepository;
    private final AdminAccessService adminAccessService;

    public ReportServiceImpl(OrderSessionService orderSessionService,
                             UserOrderRepository userOrderRepository,
                             PaymentRepository paymentRepository,
                             AdminAccessService adminAccessService) {
        this.orderSessionService = orderSessionService;
        this.userOrderRepository = userOrderRepository;
        this.paymentRepository = paymentRepository;
        this.adminAccessService = adminAccessService;
    }

    @Override
    public String buildCurrentSessionReport(Long actorUserId) {
        adminAccessService.ensureAdmin(actorUserId);

        OrderSession session = orderSessionService.getTodaySession()
                .orElseThrow(() -> new NotFoundException("Today's order session not found"));

        List<UserOrder> orderedOrders = userOrderRepository.findAllByOrderSessionId(session.getId()).stream()
                .filter(order -> order.getStatus() == UserOrderStatus.ORDERED && order.getMenuItem() != null)
                .toList();
        List<Payment> payments = paymentRepository.findAllByOrderSessionIdOrderByCreatedAtAsc(session.getId());

        Map<String, Long> kitchenCounts = new LinkedHashMap<>();
        orderedOrders.stream()
                .sorted(Comparator.comparing(order -> order.getMenuItem().getName(), String.CASE_INSENSITIVE_ORDER))
                .forEach(order -> kitchenCounts.merge(order.getMenuItem().getName(), quantity(order), Long::sum));

        BigDecimal totalToCollect = payments.isEmpty()
                ? orderedOrders.stream().map(UserOrder::getFinalPrice).map(this::defaultMoney).reduce(BigDecimal.ZERO, BigDecimal::add)
                : payments.stream().map(Payment::getAmount).map(this::defaultMoney).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalApproved = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentRecordStatus.PAID)
                .map(Payment::getAmount)
                .map(this::defaultMoney)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, Payment> paymentsByOrderId = new LinkedHashMap<>();
        for (Payment payment : payments) {
            paymentsByOrderId.put(payment.getUserOrder().getId(), payment);
        }

        List<String> debtors = orderedOrders.stream()
                .filter(order -> {
                    Payment payment = paymentsByOrderId.get(order.getId());
                    return payment == null || payment.getStatus() != PaymentRecordStatus.PAID;
                })
                .map(order -> order.getUser().getDisplayName())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return """
                Kitchen View:
                %s

                Finance View:
                Total to collect: %s
                Total approved: %s

                Debtor List:
                %s
                """.formatted(
                lines(kitchenCounts),
                MoneyUtils.formatUzs(totalToCollect),
                MoneyUtils.formatUzs(totalApproved),
                debtors.isEmpty() ? "-" : String.join("\n", debtors)
        );
    }

    private String lines(Map<String, Long> kitchenCounts) {
        if (kitchenCounts.isEmpty()) {
            return "-";
        }
        return kitchenCounts.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("-");
    }

    private long quantity(UserOrder order) {
        return order.getQuantity() == null ? 1L : order.getQuantity().longValue();
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
