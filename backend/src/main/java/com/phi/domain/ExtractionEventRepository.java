package com.phi.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExtractionEventRepository extends JpaRepository<ExtractionEvent, String> {

    List<ExtractionEvent> findByLabReportIdOrderByCreatedAtAsc(Long labReportId);

    Optional<ExtractionEvent> findFirstByLabReportIdAndEventTypeOrderByCreatedAtDesc(
            Long labReportId,
            String eventType
    );

    List<ExtractionEvent> findByFamilyIdOrderByCreatedAtDesc(String familyId, Pageable pageable);

    List<ExtractionEvent> findByCreatedAtAfterOrderByCreatedAtDesc(Instant since, Pageable pageable);

    List<ExtractionEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<ExtractionEvent> findByStatusAndCreatedAtAfterOrderByCreatedAtDesc(
            String status,
            Instant since,
            Pageable pageable
    );

    @Query(value = """
            SELECT COALESCE(SUM(input_tokens), 0), COALESCE(SUM(output_tokens), 0)
            FROM extraction_events
            WHERE created_at >= :since
            """, nativeQuery = true)
    List<Object[]> sumTokensSince(@Param("since") Instant since);

    @Query(value = """
            SELECT event_type, COUNT(*)
            FROM extraction_events
            WHERE created_at >= :since
            GROUP BY event_type
            """, nativeQuery = true)
    List<Object[]> countByEventTypeSince(@Param("since") Instant since);
}
