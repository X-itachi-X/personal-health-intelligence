package com.phi.domain;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {

    List<AuditEvent> findByFamilyIdOrderByCreatedAtDesc(String familyId, Pageable pageable);

    List<AuditEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
