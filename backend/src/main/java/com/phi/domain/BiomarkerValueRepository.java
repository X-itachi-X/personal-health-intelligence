package com.phi.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BiomarkerValueRepository extends JpaRepository<BiomarkerValue, Long> {
    List<BiomarkerValue> findByLabReportIdOrderByCanonicalNameAsc(Long labReportId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("delete from BiomarkerValue b where b.labReport.id = :reportId")
    void deleteByLabReportId(@Param("reportId") Long reportId);
}
