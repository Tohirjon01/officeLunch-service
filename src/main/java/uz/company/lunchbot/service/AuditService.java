package uz.company.lunchbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uz.company.lunchbot.entity.AuditLog;
import uz.company.lunchbot.entity.LunchUser;
import uz.company.lunchbot.entity.OrderSession;
import uz.company.lunchbot.enums.AuditAction;
import uz.company.lunchbot.repository.AuditLogRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, Long orderSessionId, Long userId, Object oldValue, Object newValue) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(action);
            auditLog.setOldValue(writeSafely(oldValue));
            auditLog.setNewValue(writeSafely(newValue));
            if (orderSessionId != null) {
                auditLog.setOrderSession(entityManager.getReference(OrderSession.class, orderSessionId));
            }
            if (userId != null) {
                auditLog.setUser(entityManager.getReference(LunchUser.class, userId));
            }
            auditLogRepository.save(auditLog);
        } catch (Exception exception) {
            log.error("audit_log_failed action={} orderSessionId={} userId={} reason={}",
                    action, orderSessionId, userId, exception.getMessage(), exception);
        }
    }

    private String writeSafely(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return String.valueOf(value);
        }
    }
}
