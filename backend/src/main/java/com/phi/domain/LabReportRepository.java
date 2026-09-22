package com.phi.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LabReportRepository extends JpaRepository<LabReport, Long> {

    @Query("""
            SELECT r FROM LabReport r
            JOIN FETCH r.person p
            WHERE r.id = :id AND r.deletedAt IS NULL
            """)
    Optional<LabReport> findActiveById(@Param("id") Long id);

    @Query("""
            SELECT r FROM LabReport r
            JOIN FETCH r.person p
            WHERE r.person.id = :personId AND r.deletedAt IS NULL
            ORDER BY r.uploadedAt DESC
            """)
    List<LabReport> findActiveByPersonId(@Param("personId") Long personId);

    @Query("""
            SELECT r FROM LabReport r
            JOIN FETCH r.person p
            WHERE r.familyId = :familyId AND r.deletedAt IS NULL
            ORDER BY r.uploadedAt DESC
            """)
    List<LabReport> findActiveByFamilyId(@Param("familyId") String familyId);

    @Query("""
            SELECT r FROM LabReport r
            WHERE r.person.id = :personId
            AND r.contentHash = :contentHash
            AND r.documentType = :documentType
            AND r.deletedAt IS NULL
            """)
    Optional<LabReport> findActiveByPersonIdAndContentHashAndDocumentType(
            @Param("personId") Long personId,
            @Param("contentHash") String contentHash,
            @Param("documentType") DocumentType documentType
    );

    @Query("""
            SELECT r FROM LabReport r
            WHERE r.storagePath IS NOT NULL
            AND r.deletedAt IS NULL
            AND r.extractionStatus IN (com.phi.domain.ExtractionStatus.COMPLETED, com.phi.domain.ExtractionStatus.TEXT_ONLY)
            AND r.extractedAt IS NOT NULL
            AND r.extractedAt < :cutoff
            """)
    List<LabReport> findDueForFilePurge(@Param("cutoff") Instant cutoff);

    @Query("""
            SELECT r FROM LabReport r
            JOIN FETCH r.person p
            WHERE r.deletedAt IS NULL
            ORDER BY r.uploadedAt DESC
            """)
    List<LabReport> findRecentActive(Pageable pageable);

    @Query("""
            SELECT r FROM LabReport r
            WHERE r.person.id = :personId
            AND r.deletedAt IS NULL
            AND r.extractionStatus = :status
            AND r.reportDate IS NOT NULL
            ORDER BY r.reportDate DESC, r.uploadedAt DESC
            """)
    List<LabReport> findCompletedWithReportDateByPersonId(
            @Param("personId") Long personId,
            @Param("status") ExtractionStatus status,
            Pageable pageable
    );

    default List<LabReport> findCompletedWithReportDateByPersonId(Long personId, Pageable pageable) {
        return findCompletedWithReportDateByPersonId(personId, ExtractionStatus.COMPLETED, pageable);
    }
}
