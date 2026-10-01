package com.phi.analytics;

import com.phi.access.AccessControlService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.DocumentType;
import com.phi.domain.ExtractionStatus;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetestReminderService {

    private final JdbcTemplate analyticsJdbc;
    private final LabReportRepository labReportRepository;
    private final AccessControlService accessControl;
    private final RetestIntervalLoader intervalLoader;

    public RetestReminderService(
            @Qualifier("analyticsJdbcTemplate") JdbcTemplate analyticsJdbc,
            LabReportRepository labReportRepository,
            AccessControlService accessControl,
            RetestIntervalLoader intervalLoader
    ) {
        this.analyticsJdbc = analyticsJdbc;
        this.labReportRepository = labReportRepository;
        this.accessControl = accessControl;
        this.intervalLoader = intervalLoader;
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.PersonRemindersResponse reminders(AuthenticatedAccount account, Long personId) {
        requireCanViewPerson(account, personId);

        List<LabReport> eligibleReports = labReportRepository.findActiveByPersonId(personId).stream()
            .filter(report -> report.getDocumentType() == DocumentType.LAB_REPORT)
            .filter(report -> report.getExtractionStatus() == ExtractionStatus.COMPLETED
                || report.getExtractionStatus() == ExtractionStatus.TEXT_ONLY)
            .filter(report -> report.getReportDate() != null)
            .toList();
        Set<Long> eligibleReportIds = new HashSet<>();
        eligibleReports.forEach(report -> eligibleReportIds.add(report.getId()));

        Map<String, RetestReminderComputer.LatestBiomarkerReading> latestByCanonical =
            loadLatestBiomarkers(personId, eligibleReportIds);
        LocalDate latestLabReportDate = eligibleReports.stream()
            .map(LabReport::getReportDate)
            .max(LocalDate::compareTo)
            .orElse(null);

        List<AnalyticsDtos.RetestReminder> reminders = RetestReminderComputer.compute(
                LocalDate.now(),
                intervalLoader.config(),
                latestByCanonical,
                latestLabReportDate
        );

        String summary = buildSummary(reminders);
        return new AnalyticsDtos.PersonRemindersResponse(personId, summary, reminders);
    }

        private Map<String, RetestReminderComputer.LatestBiomarkerReading> loadLatestBiomarkers(
                        Long personId,
                        Set<Long> eligibleReportIds
        ) {
                if (eligibleReportIds.isEmpty()) {
                        return Map.of();
                }

                List<BiomarkerRow> rows = analyticsJdbc.query("""
                                SELECT canonical_name, numeric_value, reference_range, report_date, lab_report_id AS report_id
                FROM fact_biomarker fb
                                WHERE person_id = ?
                                    AND report_date IS NOT NULL
                                    AND numeric_value IS NOT NULL
                                ORDER BY canonical_name ASC, report_date DESC, uploaded_at DESC
                """,
                (rs, rowNum) -> new BiomarkerRow(
                        rs.getString("canonical_name"),
                        rs.getBigDecimal("numeric_value"),
                        rs.getString("reference_range"),
                        rs.getDate("report_date").toLocalDate(),
                        rs.getLong("report_id")
                ),
                personId
        );

        Map<String, RetestReminderComputer.LatestBiomarkerReading> latest = new HashMap<>();
        for (BiomarkerRow row : rows) {
            if (!eligibleReportIds.contains(row.reportId())) {
                continue;
            }
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
