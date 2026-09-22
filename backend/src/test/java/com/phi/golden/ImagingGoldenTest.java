package com.phi.golden;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phi.imaging.ImagingDtos;
import com.phi.imaging.ImagingTextParser;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ImagingGoldenTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void imagingParserMatchesGoldenFixtures() throws Exception {
        for (Path path : GoldenDatasetPaths.imagingFixtures()) {
            GoldenImagingFixture fixture = objectMapper.readValue(path.toFile(), GoldenImagingFixture.class);
            ImagingDtos.ExtractedImagingStudy study = ImagingTextParser.parse(
                    fixture.sourceText(),
                    LocalDate.parse(fixture.reportDate())
            );
            GoldenImagingFixture.ExpectedStudy expected = fixture.expected();

            assertEquals(expected.modality(), study.modality(), "Modality for " + path);
            assertEquals(expected.bodyRegion(), study.bodyRegion(), "Body region for " + path);
            assertNotNull(study.impression());
            assertTrue(
                    study.impression().toLowerCase().contains(expected.impressionContains().toLowerCase()),
                    "Impression for " + path
            );

            if (expected.facilityContains() != null) {
                assertNotNull(study.facility());
                assertTrue(
                        study.facility().toLowerCase().contains(expected.facilityContains().toLowerCase()),
                        "Facility for " + path
                );
            }

            if (expected.minFindings() != null) {
                assertTrue(study.findings().size() >= expected.minFindings(), "Findings count for " + path);
            }
        }
    }
}
