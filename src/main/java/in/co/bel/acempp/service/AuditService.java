package in.co.bel.acempp.service;

import in.co.bel.acempp.entity.AuditLog;
import in.co.bel.acempp.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;

    public void save(AuditLog auditLog) {
        auditRepository.save(auditLog);
    }
}
