package com.phi.analytics;

import com.phi.domain.BiomarkerValue;
import com.phi.domain.BiomarkerValueRepository;
import com.phi.domain.ExtractionStatus;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.domain.Person;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsSyncService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsSyncService.class);

    private final JdbcTemplate analyticsJdbc;
    private final AnalyticsSchemaInitializer schemaInitializer;
    private final LabReportRepository labReportRepository;
    private final BiomarkerValueRepository biomarkerValueRepository;

    public AnalyticsSyncService(
            @Qualifier("analyticsJdbcTemplate") JdbcTemplate analyticsJdbcTemplate,
            AnalyticsSchemaInitializer schemaInitializer,
            LabReportRepository labReportRepository,
            BiomarkerValueRepository biomarkerValueRepository
    ) {
        this.analyticsJdbc = analyticsJdbcTemplate;
        this.schemaInitializer = schemaInitializer;
        this.labReportRepository = labReportRepository;
        this.biomarkerValueRepository = biomarkerValueRepository;
    }

    public void syncReport(Long reportId) {
        schemaInitializer.ensureSchema();

        LabReport report = labReportRepository.findActiveById(reportId).orElse(null);
        if (report == null) {
            return;
        }
        if (report.getExtractionStatus() != ExtractionStatus.COMPLETED) {
            return;
        }

        Person person = report.getPerson();
        upsertPerson(person, report.getFamilyId());

        analyticsJdbc.update("DELETE FROM fact_biomarker WHERE lab_report_id = ?", reportId.longValue());

        List<BiomarkerValue> biomarkers = biomarkerValueRepository.findByLabReportIdOrderByCanonicalNameAsc(reportId);
        LocalDate reportDate = report.getReportDate();
        if (reportDate == null) {
            log.warn("Skipping DuckDB sync for report {} — report date is required", reportId);
            return;
        }
        Date sqlReportDate = Date.valueOf(reportDate);
        Timestamp uploadedAt = Timestamp.from(report.getUploadedAt());

        for (BiomarkerValue biomarker : biomarkers) {
            if (biomarker.getNumericValue() == null) {
                continue;
            }
            analyticsJdbc.update("DELETE FROM fact_biomarker WHERE biomarker_id = ?", biomarker.getId().longValue());
            analyticsJdbc.update("""
                    INSERT INTO fact_biomarker (
                        biomarker_id, person_id, family_id, lab_report_id, report_date,
                        canonical_name, numeric_value, text_value, unit, reference_range, uploaded_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    biomarker.getId().longValue(),
                    person.getId().longValue(),
                    report.getFamilyId(),
                    reportId.longValue(),
                    sqlReportDate.toString(),
                    biomarker.getCanonicalName(),
                    biomarker.getNumericValue().doubleValue(),
                    biomarker.getTextValue() != null ? biomarker.getTextValue() : "",
                    biomarker.getUnit() != null ? biomarker.getUnit() : "",
                    biomarker.getReferenceRange() != null ? biomarker.getReferenceRange() : "",
                    uploadedAt.toInstant().toString()
            );
        }

        log.debug("Synced report {} with {} biomarkers to DuckDB", reportId, biomarkers.size());
    }

    public AnalyticsDtos.RebuildResponse rebuildAll() {
        schemaInitializer.ensureSchema();
        analyticsJdbc.execute("DELETE FROM fact_biomarker");
        analyticsJdbc.execute("DELETE FROM dim_person");

        List<LabReport> reports = labReportRepository.findAll().stream()
                .filter(r -> !r.isDeleted())
                .filter(r -> r.getExtractionStatus() == ExtractionStatus.COMPLETED)
                .toList();

        int biomarkerCount = 0;
        for (LabReport report : reports) {
            syncReport(report.getId());
            biomarkerCount += biomarkerValueRepository.findByLabReportIdOrderByCanonicalNameAsc(report.getId()).size();
        }

        int personCount = analyticsJdbc.queryForObject(
                "SELECT COUNT(*) FROM dim_person",
                Integer.class
        );

        return new AnalyticsDtos.RebuildResponse(
                personCount,
                biomarkerCount,
                "Analytics store rebuilt from H2"
        );
    }

    private void upsertPerson(Person person, String familyId) {
        Date dateOfBirth = person.getDateOfBirth() != null
                ? Date.valueOf(person.getDateOfBirth())
                : null;

        analyticsJdbc.update("""
                INSERT INTO dim_person (person_id, family_id, display_name, sex, date_of_birth)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (person_id) DO UPDATE SET
                    family_id = excluded.family_id,
                    display_name = excluded.display_name,
                    sex = excluded.sex,
                    date_of_birth = excluded.date_of_birth
                """,
                person.getId().longValue(),
                familyId,
                person.getDisplayName(),
                person.getSex(),
                dateOfBirth != null ? dateOfBirth.toString() : null
        );
    }
}
