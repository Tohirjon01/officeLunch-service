package uz.company.lunchbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import uz.company.lunchbot.enums.PaymentMethod;
import uz.company.lunchbot.enums.PaymentRecordStatus;

@Getter
@Setter
@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payments_user_order", columnNames = {"user_order_id"})
})
public class Payment extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "user_order_id", nullable = false)
    private UserOrder userOrder;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private LunchUser user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_session_id", nullable = false)
    private OrderSession orderSession;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 32)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PaymentRecordStatus status;

    @Column(name = "receipt_file_id")
    private String receiptFileId;

    @Column(name = "receipt_message_id")
    private Long receiptMessageId;

    @Column(name = "admin_comment")
    private String adminComment;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
