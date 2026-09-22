package com.phi.golden;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phi.domain.MedicationCourseType;
import com.phi.prescription.PrescriptionDtos;
import com.phi.prescription.PrescriptionTextParser;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrescriptionGoldenTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void prescriptionParserMatchesGoldenFixtures() throws Exception {
        for (Path path : GoldenDatasetPaths.prescriptionFixtures()) {
            GoldenPrescriptionFixture fixture = objectMapper.readValue(path.toFile(), GoldenPrescriptionFixture.class);
            List<PrescriptionDtos.ExtractedMedicationItem> parsed = PrescriptionTextParser.parse(
                    fixture.sourceText(),
                    LocalDate.parse(fixture.reportDate())
            );

            assertEquals(fixture.medications().size(), parsed.size(), "Medication count for " + path);

            for (GoldenPrescriptionFixture.ExpectedMedication expected : fixture.medications()) {
                PrescriptionDtos.ExtractedMedicationItem match = parsed.stream()
                        .filter(item -> item.medicationName().toLowerCase()
                                .contains(expected.medicationName().toLowerCase()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError(
                                "Missing medication " + expected.medicationName() + " in " + path));

                assertEquals(
                        MedicationCourseType.valueOf(expected.courseType()),
                        match.courseType(),
                        "Course type for " + expected.medicationName()
                );

                if (expected.scheduleText() != null) {
                    assertNotNull(match.scheduleText());
                    assertTrue(
                            match.scheduleText().toLowerCase().contains(expected.scheduleText().toLowerCase()),
                            "Schedule for " + expected.medicationName()
                    );
                }

                if (expected.durationDays() != null) {
                    assertEquals(expected.durationDays(), match.durationDays());
                }
            }
        }
    }
}
