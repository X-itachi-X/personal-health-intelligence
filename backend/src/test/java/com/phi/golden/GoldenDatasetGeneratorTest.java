package com.phi.golden;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.phi.extraction.BiomarkerNormalizer;
import com.phi.extraction.ClaudeBiomarkerDto;
import com.phi.extraction.ClaudeExtractionClient;
import com.phi.extraction.PdfTextExtractor;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Manual regeneration: GENERATE_GOLDEN=true ./gradlew test --tests GoldenDatasetGeneratorTest
 */
@SpringBootTest
@Tag("manual")
class GoldenDatasetGeneratorTest {

    private static final Path PDF_SOURCE = Path.of(
            System.getProperty("user.dir")).getParent()
            .resolve("data/ankit_6972960_20260906073107_stationerypdf_oh_merged.pdf");

    @Autowired
    private PdfTextExtractor pdfTextExtractor;

    @Autowired
    private ClaudeExtractionClient claudeClient;

    @Autowired
    private BiomarkerNormalizer normalizer;

    @Test
    void generateFromSourcePdf() throws Exception {
        if (!"true".equalsIgnoreCase(System.getenv("GENERATE_GOLDEN"))) {
            return;
        }
        if (!claudeClient.isConfigured()) {
            throw new IllegalStateException("CLAUDE_API_KEY required to regenerate golden dataset");
        }
        if (!Files.isRegularFile(PDF_SOURCE)) {
            throw new IllegalStateException("Source PDF not found: " + PDF_SOURCE);
        }

        String text = pdfTextExtractor.extractText(PDF_SOURCE);
        List<ClaudeBiomarkerDto> extracted = claudeClient.extractBiomarkers(text).biomarkers();

        List<Map<String, Object>> biomarkers = new ArrayList<>();
        for (ClaudeBiomarkerDto dto : extracted) {
            String canonical = dto.testName() != null && !dto.testName().isBlank()
                    ? normalizer.normalize(dto.testName())
                    : normalizer.normalize(dto.canonical());
            if (canonical.equals("unknown") || dto.value() == null) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("canonical", canonical);
            entry.put("value", dto.value().doubleValue());
            if (dto.unit() != null && !dto.unit().isBlank()) {
                entry.put("unit", dto.unit());
            }
            if (dto.referenceRange() != null && !dto.referenceRange().isBlank()) {
                entry.put("reference", dto.referenceRange());
            }
            if (dto.testName() != null && !dto.testName().isBlank()) {
                entry.put("test_name", dto.testName());
            }
            biomarkers.add(entry);
        }

        biomarkers.sort(Comparator.comparing(m -> (String) m.get("canonical")));

        Map<String, Object> fixture = new LinkedHashMap<>();
        fixture.put("patient", "ankit");
        fixture.put("report_date", "2026-09-06");
        fixture.put("source_pdf", "ankit_6972960_20260906073107_stationerypdf_oh_merged.pdf");
        fixture.put("source", "Orange Health Ultra Full Body Checkup — extracted via Claude");
        fixture.put("biomarker_count", biomarkers.size());
        fixture.put("biomarkers", biomarkers);
        fixture.put("expected_findings", List.of(
                "elevated_lp_a",
                "alt_elevated",
                "vitamin_d_insufficient",
                "microcytic_rbc_pattern",
                "folate_low_homocysteine_upper_normal",
                "hdl_below_target"
        ));

        Path outDir = GoldenDatasetPaths.root().resolve("ankit");
        Files.createDirectories(outDir);
        Path outJson = outDir.resolve("report_2026_09.json");
        Path outPdf = outDir.resolve("ankit_6972960_20260906073107_stationerypdf_oh_merged.pdf");

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(outJson.toFile(), fixture);
        Files.copy(PDF_SOURCE, outPdf, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("Wrote " + biomarkers.size() + " biomarkers to " + outJson);
    }
}
