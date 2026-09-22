package com.phi.extraction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.phi.domain.ExtractionStatus;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.domain.Person;
import com.phi.domain.PersonRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ReportRetryTest {

    @Autowired
    private ReportExtractionService extractionService;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private LabReportRepository labReportRepository;

    @Test
    void retryRejectsProcessingReport() throws Exception {
        Person person = personRepository.save(new Person("Retry Test"));
        Path storagePath = Path.of(System.getProperty("java.io.tmpdir"), "phi-retry-test.pdf");
        Files.writeString(storagePath, "retry test");

        LabReport report = new LabReport(person, null, null, "retry.pdf", storagePath.toString(), null);
        report.markProcessing();
        report = labReportRepository.save(report);
        Long reportId = report.getId();

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> extractionService.retryExtraction(reportId)
        );
        assertEquals("Extraction already in progress", error.getMessage());
    }

    @Test
    void retryAcceptsFailedReport() throws Exception {
        Person person = personRepository.save(new Person("Retry Failed"));
        Path storagePath = Path.of(System.getProperty("java.io.tmpdir"), "phi-retry-failed.pdf");
        Files.writeString(storagePath, "retry failed test");

        LabReport report = new LabReport(person, null, null, "failed.pdf", storagePath.toString(), null);
        report.markFailed("Claude timeout");
        report = labReportRepository.save(report);
        Long reportId = report.getId();

        assertDoesNotThrow(() -> extractionService.retryExtraction(reportId));
    }
}
