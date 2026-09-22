package com.phi.analytics;

import com.phi.access.AccessControlService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.DocumentType;
import com.phi.domain.ExtractionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetestReminderService {

    private final JdbcTemplate jdbcTemplate;
    private final AccessControlService accessControl;
    private final RetestIntervalLoader intervalLoader;

    public RetestReminderService(
            JdbcTemplate jdbcTemplate,
            AccessControlService accessControl,
            RetestIntervalLoader intervalLoader
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.accessControl = accessControl;
        this.intervalLoader = intervalLoader;
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.PersonRemindersResponse reminders(AuthenticatedAccount account, Long personId) {
        requireCanViewPerson(account, personId);

        Map<String, RetestReminderComputer.LatestBiomarkerReading> latestByCanonical = loadLatestBiomarkers(personId);
        LocalDate latestLabReportDate = loadLatestLabReportDate(personId);

        List<AnalyticsDtos.RetestReminder> reminders = RetestReminderComputer.compute(
                LocalDate.now(),
                intervalLoader.config(),
                latestByCanonical,
                latestLabReportDate
        );

        String summary = buildSummary(reminders);
        return new AnalyticsDtos.PersonRemindersResponse(personId, summary, reminders);
    }

    private Map<String, RetestReminderComputer.LatestBiomarkerReading> loadLatestBiomarkers(Long personId) {
        List<BiomarkerRow> rows = jdbcTemplate.query("""
                SELECT bv.canonical_name, bv.numeric_value, bv.reference_range, lr.report_date, lr.id AS report_id
                FROM biomarker_values bv
                JOIN lab_reports lr ON lr.id = bv.lab_report_id
                WHERE lr.person_id = ?
                  AND lr.deleted_at IS NULL
                  AND lr.extraction_status IN (?, ?)
                  AND lr.report_date IS NOT NULL
                  AND bv.numeric_value IS NOT NULL
                ORDER BY bv.canonical_name ASC, lr.report_date DESC, lr.uploaded_at DESC
                """,
                (rs, rowNum) -> new BiomarkerRow(
                        rs.getString("canonical_name"),
                        rs.getBigDecimal("numeric_value"),
                        rs.getString("reference_range"),
                        rs.getDate("report_date").toLocalDate(),
                        rs.getLong("report_id")
                ),
                personId,
                ExtractionStatus.COMPLETED.name(),
                ExtractionStatus.TEXT_ONLY.name()
        );

        Map<String, RetestReminderComputer.LatestBiomarkerReading> latest = new HashMap<>();
        for (BiomarkerRow row : rows) {
            latest.putIfAbsent(
                    row.canonical(),
                    new RetestReminderComputer.LatestBiomarkerReading(
                            row.canonical(),
                            row.numericValue(),
                            row.referenceRange(),
                            row.reportDate(),
                            row.reportId()
                    )
            );
        }
        return latest;
    }

    private LocalDate loadLatestLabReportDate(Long personId) {
        List<LocalDate> dates = jdbcTemplate.query("""
                SELECT report_date
                FROM lab_reports
                WHERE person_id = ?
                  AND deleted_at IS NULL
                  AND document_type = ?
                  AND extraction_status IN (?, ?)
                  AND report_date IS NOT NULL
                ORDER BY report_date DESC, uploaded_at DESC
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getDate("report_date").toLocalDate(),
                personId,
                DocumentType.LAB_REPORT.name(),
                ExtractionStatus.COMPLETED.name(),
                ExtractionStatus.TEXT_ONLY.name()
        );
        return dates.isEmpty() ? null : dates.getFirst();
    }

    private static String buildSummary(List<AnalyticsDtos.RetestReminder> reminders) {
        long overdue = reminders.stream().filter(reminder -> "OVERDUE".equals(reminder.urgency())).count();
        long dueSoon = reminders.stream().filter(reminder -> "DUE_SOON".equals(reminder.urgency())).count();
        if (overdue > 0) {
            return overdue == 1
                    ? "1 follow-up test is overdue."
                    : overdue + " follow-up tests are overdue.";
        }
        if (dueSoon > 0) {
            return dueSoon == 1
                    ? "1 follow-up test due soon."
                    : dueSoon + " follow-up tests due soon.";
        }
        if (reminders.isEmpty()) {
            return "No follow-up tests scheduled yet. Upload a lab report to start tracking.";
        }
        return "No testing is currently due. Continue your current routine.";
    }

    private void requireCanViewPerson(AuthenticatedAccount account, Long personId) {
        accessControl.requireAuthenticated(account);
        if (personId.equals(account.personId())) {
            return;
        }
        accessControl.requireAdvanced(account);
        if (!accessControl.sameFamily(account.personId(), personId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Cannot view reminders for this person"
            );
        }
    }

    private record BiomarkerRow(
            String canonical,
            BigDecimal numericValue,
            String referenceRange,
            LocalDate reportDate,
            Long reportId
    ) {
    }
}
