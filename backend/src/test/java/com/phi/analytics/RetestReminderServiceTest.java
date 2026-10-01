package com.phi.analytics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.phi.access.AccessControlService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.Account;
import com.phi.domain.DocumentType;
import com.phi.domain.ExtractionStatus;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.domain.Person;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class RetestReminderServiceTest {

    @Test
    void loadsLatestBiomarkersFromAnalyticsFactTable() {
        DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:reminder-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("""
                CREATE TABLE fact_biomarker (
                    biomarker_id BIGINT PRIMARY KEY,
                    person_id BIGINT,
                    family_id VARCHAR,
                    lab_report_id BIGINT,
                    report_date DATE,
                    canonical_name VARCHAR,
                    numeric_value DOUBLE,
                    text_value VARCHAR,
                    unit VARCHAR,
                    reference_range VARCHAR,
                    uploaded_at TIMESTAMP
                )
                """);


        LabReport report = mock(LabReport.class);
        when(report.getId()).thenReturn(99L);
        when(report.getDocumentType()).thenReturn(DocumentType.LAB_REPORT);
        when(report.getExtractionStatus()).thenReturn(ExtractionStatus.COMPLETED);
        when(report.getReportDate()).thenReturn(LocalDate.of(2025, 1, 15));
        LabReportRepository labReportRepository = mock(LabReportRepository.class);
        when(labReportRepository.findActiveByPersonId(42L)).thenReturn(List.of(report));
        jdbcTemplate.update(
                "INSERT INTO fact_biomarker (biomarker_id, person_id, family_id, lab_report_id, report_date, canonical_name, numeric_value, text_value, unit, reference_range, uploaded_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                1L,
                42L,
                "family-1",
                99L,
                Date.valueOf(LocalDate.of(2025, 1, 15)),
                "vitamin_d",
                18.0,
                "",
                "ng/mL",
                "30-100",
                Timestamp.valueOf("2025-01-15 09:00:00")
        );

        RetestIntervalLoader intervalLoader = mock(RetestIntervalLoader.class);
        when(intervalLoader.config()).thenReturn(new RetestIntervalConfig(
                new RetestIntervalConfig.General(12, "Routine health checkup", 30),
                List.of(new RetestIntervalConfig.Rule("vitamin_d", "Vitamin D", 3, 12))
        ));

        AccessControlService accessControl = mock(AccessControlService.class);
        Account account = mock(Account.class);
        Person person = mock(Person.class);
        when(account.getPerson()).thenReturn(person);
        when(account.getId()).thenReturn("account-1");
        when(person.getId()).thenReturn(42L);

        RetestReminderService service = new RetestReminderService(
                jdbcTemplate,
                labReportRepository,
                accessControl,
                intervalLoader
        );
        AnalyticsDtos.PersonRemindersResponse response = service.reminders(new AuthenticatedAccount(account), 42L);

        assertFalse(response.reminders().isEmpty());
    }
}
