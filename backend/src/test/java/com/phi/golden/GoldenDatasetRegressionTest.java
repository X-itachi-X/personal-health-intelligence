package com.phi.golden;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GoldenDatasetRegressionTest {

    private final GoldenDatasetLoader loader = new GoldenDatasetLoader();

    @Test
    void loadsAllGoldenReportFixtures() throws Exception {
        var fixtures = loader.loadAll();
        assertFalse(fixtures.isEmpty(), "Expected at least one golden report fixture");
        for (GoldenDatasetLoader.LoadedFixture loaded : fixtures) {
            GoldenReportFixture fixture = loaded.fixture();
            assertNotNull(fixture.patient(), loaded.path() + ": patient required");
            assertNotNull(fixture.reportDate(), loaded.path() + ": report_date required");
            assertFalse(fixture.biomarkers().isEmpty(), loaded.path() + ": biomarkers required");
            if (fixture.biomarkerCount() != null) {
                assertEquals(fixture.biomarkerCount(), fixture.biomarkers().size(), loaded.path() + ": biomarker_count mismatch");
            }
            for (GoldenReportFixture.GoldenBiomarker biomarker : fixture.biomarkers()) {
                assertNotNull(biomarker.canonical(), loaded.path() + ": canonical required");
                assertNotNull(biomarker.value(), loaded.path() + ": value required for " + biomarker.canonical());
            }
            if (fixture.expectedFindings() != null) {
                assertFalse(fixture.expectedFindings().isEmpty(), loaded.path() + ": expected_findings should not be empty");
            }
        }
    }

    @Test
    void goldenAssertionsDetectValueMismatch() {
        GoldenReportFixture fixture = new GoldenReportFixture(
                "test",
                "2026-01-01",
                "unit-test",
                null,
                1,
                List.of(new GoldenReportFixture.GoldenBiomarker("vitamin_d", 21.3, "ng/mL", "30-100", "Vitamin D")),
                List.of("vitamin_d_insufficient")
        );
        var dtos = GoldenDatasetAssertions.toExtractionDtos(fixture);
        assertFalse(dtos.isEmpty());
        assertTrue(dtos.getFirst().canonical().equals("vitamin_d"));
    }
}
