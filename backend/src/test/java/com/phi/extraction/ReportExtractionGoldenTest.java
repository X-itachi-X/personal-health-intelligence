package com.phi.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.phi.domain.BiomarkerValue;
import com.phi.domain.BiomarkerValueRepository;
import com.phi.domain.ExtractionStatus;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.domain.Person;
import com.phi.domain.PersonRepository;
import com.phi.golden.GoldenDatasetAssertions;
import com.phi.golden.GoldenDatasetLoader;
import com.phi.golden.GoldenReportFixture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ReportExtractionGoldenTest {

    @Autowired
    private ReportExtractionService extractionService;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private LabReportRepository labReportRepository;

    @Autowired
    private BiomarkerValueRepository biomarkerValueRepository;

    @Test
    void persistedExtractionMatchesGoldenDataset() throws Exception {
        GoldenDatasetLoader loader = new GoldenDatasetLoader();
        GoldenReportFixture fixture = loader.loadAll().getFirst().fixture();

        Person person = personRepository.save(new Person(fixture.patient()));
        Path storageDir = Path.of(System.getProperty("java.io.tmpdir"), "phi-golden-test");
        Files.createDirectories(storageDir);
        Path storagePath = storageDir.resolve("golden-report.pdf");
        Files.writeString(storagePath, "golden test placeholder");

        LabReport report = new LabReport(
                person,
                null,
                null,
                "golden-report.pdf",
                storagePath.toString(),
                null
        );
        report.setReportDate(LocalDate.parse(fixture.reportDate()));
        report = labReportRepository.save(report);

        List<ClaudeBiomarkerDto> extracted = GoldenDatasetAssertions.toExtractionDtos(fixture);
        extractionService.completeExtraction(report.getId(), "golden extracted text", extracted);

        List<BiomarkerValue> saved = biomarkerValueRepository.findByLabReportIdOrderByCanonicalNameAsc(report.getId());
        GoldenDatasetAssertions.assertBiomarkersMatch(fixture, saved);

        LabReport updated = labReportRepository.findById(report.getId()).orElseThrow();
        assertEquals(ExtractionStatus.COMPLETED, updated.getExtractionStatus());
        assertEquals(fixture.biomarkers().size(), saved.size());
    }
}
