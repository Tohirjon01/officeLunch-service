package uz.company.lunchbot.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.company.lunchbot.entity.Payment;
import uz.company.lunchbot.enums.PaymentRecordStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByUserOrderId(Long userOrderId);

    List<Payment> findAllByOrderSessionIdOrderByCreatedAtAsc(Long orderSessionId);

    Optional<Payment> findFirstByUserIdAndOrderSessionOrderDateAndStatusInOrderByCreatedAtDesc(
            Long userId,
            LocalDate orderDate,
            Collection<PaymentRecordStatus> statuses
    );

    Optional<Payment> findFirstByUserIdAndStatusInOrderByCreatedAtDesc(
            Long userId,
            Collection<PaymentRecordStatus> statuses
    );
}
