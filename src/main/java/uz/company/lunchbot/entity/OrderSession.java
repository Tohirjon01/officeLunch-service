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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import uz.company.lunchbot.enums.OrderSessionStatus;

@Getter
@Setter
@Entity
@Table(name = "order_sessions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_order_sessions_order_date_restaurant", columnNames = {"order_date", "restaurant_id"})
})
public class OrderSession extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderSessionStatus status;

    @Column(name = "delivery_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal deliveryPrice;

    @Column(name = "container_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal containerPrice;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private LunchUser createdBy;
}
