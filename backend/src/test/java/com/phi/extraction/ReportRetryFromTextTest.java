package com.phi.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.phi.domain.ExtractionStatus;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.domain.Person;
import com.phi.domain.PersonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ReportRetryFromTextTest {

    @Autowired
    private ReportExtractionService extractionService;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private LabReportRepository labReportRepository;

    @Test
    void localExtractionFailsWhenFileAndStoredTextMissing() throws Exception {
        Person person = personRepository.save(new Person("Retry Text"));
        LabReport report = new LabReport(person, null, null, "missing.pdf", null, "hash123");
        report.markFailed("previous failure");
        report = labReportRepository.save(report);

        boolean ready = extractionService.extractAndPersistLocalText(report.getId());

        assertEquals(false, ready);

        LabReport updated = labReportRepository.findById(report.getId()).orElseThrow();
        assertEquals(ExtractionStatus.FAILED, updated.getExtractionStatus());
        assertEquals("No text could be extracted from the document", updated.getExtractionError());
    }

    @Test
    void localExtractionPersistsStoredTextBeforeClaude() throws Exception {
        Person person = personRepository.save(new Person("Persist Text"));
        java.nio.file.Path storagePath = java.nio.file.Path.of(
                System.getProperty("java.io.tmpdir"),
                "phi-staged-" + System.nanoTime() + ".txt"
        );
        java.nio.file.Files.writeString(storagePath, "Glucose 95 mg/dL");

        LabReport report = new LabReport(
                person,
                null,
                null,
                "staged.txt",
                storagePath.toString(),
                "hash789"
        );
        report = labReportRepository.save(report);

        boolean ready = extractionService.extractAndPersistLocalText(report.getId());

        assertEquals(true, ready);

        LabReport updated = labReportRepository.findById(report.getId()).orElseThrow();
        assertEquals(ExtractionStatus.TEXT_EXTRACTED, updated.getExtractionStatus());
        assertNotNull(updated.getExtractedText());
        assertNull(updated.getStoragePath());
    }

    @Test
    void retryUsesStoredTextWithoutReReadingFile() throws Exception {
        Person person = personRepository.save(new Person("Retry Stored Text"));
        LabReport report = new LabReport(person, null, null, "retry.pdf", null, "hash456");
        report.markTextExtracted("Hemoglobin 14.2 g/dL");
        report = labReportRepository.save(report);

        boolean ready = extractionService.extractAndPersistLocalText(report.getId());

        assertEquals(true, ready);
        assertEquals(
                ExtractionStatus.TEXT_EXTRACTED,
                labReportRepository.findById(report.getId()).orElseThrow().getExtractionStatus()
        );
    }
}
