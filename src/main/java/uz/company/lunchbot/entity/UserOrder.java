package uz.company.lunchbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import uz.company.lunchbot.enums.PaymentStatus;
import uz.company.lunchbot.enums.UserOrderStatus;

@Getter
@Setter
@Entity
@Table(name = "user_orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_orders_session_user", columnNames = {"order_session_id", "user_id"})
})
public class UserOrder extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_session_id", nullable = false)
    private OrderSession orderSession;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private LunchUser user;

    @ManyToOne
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserOrderStatus status;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "food_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal foodPrice;

    @Column(name = "container_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal containerPrice;

    @Column(name = "delivery_share", nullable = false, precision = 19, scale = 2)
    private BigDecimal deliveryShare;

    @Column(name = "final_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 32)
    private PaymentStatus paymentStatus;

    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt;
}
