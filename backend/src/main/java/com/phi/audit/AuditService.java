package com.phi.audit;

import com.phi.domain.Account;
import com.phi.domain.AccountRepository;
import com.phi.domain.AuditEvent;
import com.phi.domain.AuditEventRepository;
import com.phi.domain.Family;
import com.phi.domain.FamilyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final AccountRepository accountRepository;
    private final FamilyRepository familyRepository;

    public AuditService(
            AuditEventRepository auditEventRepository,
            AccountRepository accountRepository,
            FamilyRepository familyRepository
    ) {
        this.auditEventRepository = auditEventRepository;
        this.accountRepository = accountRepository;
        this.familyRepository = familyRepository;
    }

    @Transactional
    public void log(String accountId, String familyId, String action, Long targetPersonId, String metadata) {
        Account actor = accountId != null
                ? accountRepository.findById(accountId).orElse(null)
                : null;
        Family family = familyId != null
                ? familyRepository.findById(familyId).orElse(null)
                : null;
        String targetType = targetPersonId != null ? "person" : null;
        String targetId = targetPersonId != null ? String.valueOf(targetPersonId) : null;
        auditEventRepository.save(new AuditEvent(family, actor, action, targetType, targetId, metadata));
    }
}
