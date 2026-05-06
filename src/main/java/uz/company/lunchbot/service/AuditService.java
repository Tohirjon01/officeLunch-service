package uz.company.lunchbot.service;

import uz.company.lunchbot.enums.AuditAction;
public interface AuditService {

    void log(AuditAction action, Long orderSessionId, Long userId, Object oldValue, Object newValue);
}
