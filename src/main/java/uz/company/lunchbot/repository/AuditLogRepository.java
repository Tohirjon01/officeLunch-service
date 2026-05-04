package uz.company.lunchbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.company.lunchbot.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
