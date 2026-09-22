package com.phi.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.phi.domain.ExtractionStatus;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.domain.Person;
import com.phi.domain.PersonRepository;
import com.phi.extraction.ReportExtractionService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ReportEphemeralStorageTest {

    @Autowired
    private ReportExtractionService extractionService;

    @Autowired
    private ReportFileStorageService fileStorageService;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private LabReportRepository labReportRepository;

    @Test
    void purgesIngestFileAfterLocalTextExtraction() throws Exception {
        Person person = personRepository.save(new Person("Ephemeral Test"));
        Path storagePath = Path.of(System.getProperty("java.io.tmpdir"), "phi-ephemeral-" + System.nanoTime() + ".txt");
        Files.writeString(storagePath, "Hemoglobin 14.2 g/dL\nVitamin D 25 ng/mL");

        LabReport report = new LabReport(
                person,
                null,
                null,
                "ephemeral.txt",
                storagePath.toString(),
                "abc123"
        );
        report = labReportRepository.save(report);
        Long reportId = report.getId();

        assertTrue(Files.exists(storagePath));

        extractionService.extractAndPersistLocalText(reportId);

        LabReport updated = labReportRepository.findById(reportId).orElseThrow();
        assertEquals(ExtractionStatus.TEXT_EXTRACTED, updated.getExtractionStatus());
        assertNull(updated.getStoragePath());
        assertFalse(Files.exists(storagePath));
        assertTrue(fileStorageService.canRetryFromStoredText(updated));
    }
}
