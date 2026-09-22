package com.phi.ops;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phi.domain.ExtractionEvent;
import com.phi.domain.ExtractionEventRepository;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.extraction.ClaudeBiomarkerDto;
import com.phi.extraction.DocumentTextExtractor;
import com.phi.extraction.FreeTextProbe;
import com.phi.extraction.rules.RuleExtractionResult;
import com.phi.extraction.rules.RuleExtractionService;
import com.phi.storage.ReportFileStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OpsFreeToolService {

    private static final String FREE_TOOLS_EVENT = "ops.free_tools";

    private final LabReportRepository labReportRepository;
    private final ExtractionEventRepository extractionEventRepository;
    private final ReportFileStorageService fileStorageService;
    private final DocumentTextExtractor documentTextExtractor;
    private final RuleExtractionService ruleExtractionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpsFreeToolService(
            LabReportRepository labReportRepository,
            ExtractionEventRepository extractionEventRepository,
            ReportFileStorageService fileStorageService,
            DocumentTextExtractor documentTextExtractor,
            RuleExtractionService ruleExtractionService
    ) {
        this.labReportRepository = labReportRepository;
        this.extractionEventRepository = extractionEventRepository;
        this.fileStorageService = fileStorageService;
        this.documentTextExtractor = documentTextExtractor;
        this.ruleExtractionService = ruleExtractionService;
    }

    @Transactional(readOnly = true)
    public Optional<OpsDtos.FreeToolComparisonView> loadCachedComparison(Long reportId) {
        return extractionEventRepository
                .findFirstByLabReportIdAndEventTypeOrderByCreatedAtDesc(reportId, FREE_TOOLS_EVENT)
                .flatMap(this::deserializeComparison);
    }

    @Transactional
    public OpsDtos.FreeToolComparisonView compareFreeTools(Long reportId, boolean refresh) {
        if (!refresh) {
            Optional<OpsDtos.FreeToolComparisonView> cached = loadCachedComparison(reportId);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        OpsDtos.FreeToolComparisonView result = runComparison(reportId);
        saveComparison(reportId, result);
        return result;
    }

    private OpsDtos.FreeToolComparisonView runComparison(Long reportId) {
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        List<OpsDtos.FreeToolResult> tools = new ArrayList<>();
        boolean hasFile = fileStorageService.hasReadableFile(report);
        Long fileSize = null;
        String contentType = null;
        String unavailableReason = null;

        if (hasFile) {
            Path path = Path.of(report.getStoragePath());
            try {
                fileSize = Files.size(path);
            } catch (IOException ignored) {
                // optional metadata
            }
            contentType = guessContentType(report.getOriginalFilename());

            for (FreeTextProbe probe : documentTextExtractor.probeAllFreeTextTools(path)) {
                tools.add(toTextResult(probe));
                if (probe.text() != null && !probe.text().isBlank()) {
                    tools.add(toRulesResult(probe.toolId(), probe.text()));
                }
            }
        } else {
            unavailableReason = "Original file was purged after text extraction. "
                    + "Use Replace file on the report screen, or set PHI_RETAIN_FILES_DAYS>0 to keep uploads.";
            String stored = report.getExtractedText();
            if (stored != null && !stored.isBlank()) {
                tools.add(storedTextResult(stored));
                tools.add(toRulesResult("text.stored_db", stored));
            }
        }

        return new OpsDtos.FreeToolComparisonView(
                reportId,
                report.getOriginalFilename(),
                hasFile,
                fileSize,
                contentType,
                unavailableReason,
                tools
        );
    }

    private void saveComparison(Long reportId, OpsDtos.FreeToolComparisonView comparison) {
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        try {
            String metadata = objectMapper.writeValueAsString(comparison);
            extractionEventRepository.save(new ExtractionEvent(
                    reportId,
                    report.getFamilyId(),
                    FREE_TOOLS_EVENT,
                    "success",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Free-tool comparison saved",
                    metadata
            ));
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save comparison");
        }
    }

    private Optional<OpsDtos.FreeToolComparisonView> deserializeComparison(ExtractionEvent event) {
        if (event.getMetadata() == null || event.getMetadata().isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(event.getMetadata(), OpsDtos.FreeToolComparisonView.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public OpsFileResource loadOriginalFile(Long reportId) {
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        if (!fileStorageService.hasReadableFile(report)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Original file not available — it may have been purged. Set PHI_RETAIN_FILES_DAYS>0 to retain uploads."
            );
        }

        Path path = Path.of(report.getStoragePath());
        try {
            byte[] bytes = Files.readAllBytes(path);
            return new OpsFileResource(
                    report.getOriginalFilename(),
                    guessContentType(report.getOriginalFilename()),
                    bytes.length,
                    bytes
            );
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read stored file");
        }
    }

    public record OpsFileResource(String filename, String contentType, long sizeBytes, byte[] bytes) {
    }

    private OpsDtos.FreeToolResult toTextResult(FreeTextProbe probe) {
        return new OpsDtos.FreeToolResult(
                probe.toolId(),
                probe.label(),
                "text",
                null,
                probe.status(),
                probe.durationMs(),
                probe.text() != null ? probe.text().length() : 0,
                null,
                null,
                null,
                probe.text(),
                probe.error(),
                List.of()
        );
    }

    private OpsDtos.FreeToolResult storedTextResult(String text) {
        return new OpsDtos.FreeToolResult(
                "text.stored_db",
                "Stored text from database (file purged)",
                "text",
                null,
                "info",
                null,
                text.length(),
                null,
                null,
                null,
                text,
                null,
                List.of()
        );
    }

    private OpsDtos.FreeToolResult toRulesResult(String sourceToolId, String text) {
        long start = System.currentTimeMillis();
        Optional<RuleExtractionResult> result = ruleExtractionService.tryExtract(text);
        if (result.isEmpty()) {
            return new OpsDtos.FreeToolResult(
                    "parse.rules",
                    "Rules / templates (no AI)",
                    "parse",
                    sourceToolId,
                    "error",
                    System.currentTimeMillis() - start,
                    text.length(),
                    0,
                    0.0,
                    null,
                    null,
                    "No matching lab template or no biomarkers found",
                    List.of()
            );
        }

        RuleExtractionResult extraction = result.get();
        List<OpsDtos.BiomarkerPreview> biomarkers = extraction.biomarkers().stream()
                .map(this::toBiomarkerPreview)
                .toList();
        String status = extraction.biomarkers().isEmpty() ? "error" : "success";
        return new OpsDtos.FreeToolResult(
                "parse.rules",
                "Rules / templates (no AI)",
                "parse",
                sourceToolId,
                status,
                System.currentTimeMillis() - start,
                text.length(),
                extraction.biomarkers().size(),
                extraction.coverage(),
                extraction.labFormat(),
                null,
                null,
                biomarkers
        );
    }

    private OpsDtos.BiomarkerPreview toBiomarkerPreview(ClaudeBiomarkerDto dto) {
        String value = dto.value() != null ? dto.value().toPlainString() : dto.textValue();
        return new OpsDtos.BiomarkerPreview(dto.canonical(), dto.testName(), value, dto.unit());
    }

    static String guessContentType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".txt")) {
            return "text/plain";
        }
        return "application/octet-stream";
    }
}
