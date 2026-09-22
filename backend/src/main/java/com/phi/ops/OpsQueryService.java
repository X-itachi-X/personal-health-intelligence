package com.phi.ops;

import com.phi.domain.AuditEvent;
import com.phi.domain.AuditEventRepository;
import com.phi.domain.ExtractionEvent;
import com.phi.domain.ExtractionEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.storage.ReportFileStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OpsQueryService {

    private final ExtractionEventRepository extractionEventRepository;
    private final AuditEventRepository auditEventRepository;
    private final LabReportRepository labReportRepository;
    private final ReportFileStorageService fileStorageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpsQueryService(
            ExtractionEventRepository extractionEventRepository,
            AuditEventRepository auditEventRepository,
            LabReportRepository labReportRepository,
            ReportFileStorageService fileStorageService
    ) {
        this.extractionEventRepository = extractionEventRepository;
        this.auditEventRepository = auditEventRepository;
        this.labReportRepository = labReportRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public OpsDtos.OpsSummary summary(int days) {
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        List<ExtractionEvent> events = extractionEventRepository.findByCreatedAtAfterOrderByCreatedAtDesc(
                since,
                PageRequest.of(0, 5000)
        );

        int inputTokens = 0;
        int outputTokens = 0;
        int aiParseCalls = 0;
        int aiVisionCalls = 0;
        int ruleSuccesses = 0;
        int failures = 0;
        Map<String, Long> byType = new HashMap<>();

        for (ExtractionEvent event : events) {
            byType.merge(event.getEventType(), 1L, Long::sum);
            if (event.getInputTokens() != null) {
                inputTokens += event.getInputTokens();
            }
            if (event.getOutputTokens() != null) {
                outputTokens += event.getOutputTokens();
            }
            if ("parse.claude".equals(event.getEventType())) {
                aiParseCalls++;
            }
            if ("text.ai_vision".equals(event.getEventType())) {
                aiVisionCalls++;
            }
            if ("parse.rules".equals(event.getEventType()) && "success".equals(event.getStatus())) {
                ruleSuccesses++;
            }
            if ("pipeline.failed".equals(event.getEventType())) {
                failures++;
            }
        }

        return new OpsDtos.OpsSummary(
                days,
                events.size(),
                inputTokens,
                outputTokens,
                aiParseCalls,
                aiVisionCalls,
                ruleSuccesses,
                failures,
                byType
        );
    }

    @Transactional(readOnly = true)
    public OpsDtos.OpsTimeseries timeseries(int days) {
        int windowDays = Math.min(Math.max(days, 1), 90);
        Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        List<ExtractionEvent> events = extractionEventRepository.findByCreatedAtAfterOrderByCreatedAtDesc(
                since,
                PageRequest.of(0, 5000)
        );

        Map<LocalDate, int[]> buckets = new LinkedHashMap<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = windowDays - 1; i >= 0; i--) {
            buckets.put(today.minusDays(i), new int[3]);
        }

        for (ExtractionEvent event : events) {
            LocalDate day = event.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            int[] bucket = buckets.get(day);
            if (bucket == null) {
                continue;
            }
            int tokens = (event.getInputTokens() != null ? event.getInputTokens() : 0)
                    + (event.getOutputTokens() != null ? event.getOutputTokens() : 0);
            bucket[0] += tokens;
            if ("pipeline.failed".equals(event.getEventType())) {
                bucket[1]++;
            }
            if ("pipeline.started".equals(event.getEventType())) {
                bucket[2]++;
            }
        }

        List<OpsDtos.OpsDayBucket> series = new ArrayList<>();
        for (Map.Entry<LocalDate, int[]> entry : buckets.entrySet()) {
            series.add(new OpsDtos.OpsDayBucket(
                    entry.getKey().toString(),
                    entry.getValue()[0],
                    entry.getValue()[1],
                    entry.getValue()[2]
            ));
        }

        return new OpsDtos.OpsTimeseries(windowDays, series);
    }

    @Transactional(readOnly = true)
    public List<OpsDtos.ExtractionEventView> recentEventsGlobal(int limit, String familyId) {
        List<ExtractionEvent> events;
        if (familyId != null && !familyId.isBlank()) {
            events = extractionEventRepository.findByFamilyIdOrderByCreatedAtDesc(
                    familyId,
                    PageRequest.of(0, limit)
            );
        } else {
            events = extractionEventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
        }
        return events.stream().map(this::toEventView).toList();
    }

    @Transactional(readOnly = true)
    public List<OpsDtos.ExtractionEventView> recentErrors(int days, int limit) {
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        return extractionEventRepository
                .findByStatusAndCreatedAtAfterOrderByCreatedAtDesc("error", since, PageRequest.of(0, limit))
                .stream()
                .map(this::toEventView)
                .toList();
    }

    @Transactional(readOnly = true)
    public OpsDtos.ReportPipelineView reportPipeline(Long reportId) {
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        List<ExtractionEvent> events = extractionEventRepository.findByLabReportIdOrderByCreatedAtAsc(reportId);
        int inputTokens = 0;
        int outputTokens = 0;
        for (ExtractionEvent event : events) {
            if (event.getInputTokens() != null) {
                inputTokens += event.getInputTokens();
            }
            if (event.getOutputTokens() != null) {
                outputTokens += event.getOutputTokens();
            }
        }

        String extractedText = report.getExtractedText() != null ? report.getExtractedText() : "";
        boolean fileAvailable = fileStorageService.hasReadableFile(report);
        Long fileSize = null;
        if (fileAvailable) {
            try {
                fileSize = Files.size(Path.of(report.getStoragePath()));
            } catch (IOException ignored) {
                // optional metadata
            }
        }

        return new OpsDtos.ReportPipelineView(
                reportId,
                report.getOriginalFilename(),
                report.getExtractionStatus().name(),
                report.getExtractionError(),
                report.getUploadedAt(),
                deriveTextExtractionTool(events),
                deriveBiomarkerParseTool(events),
                extractedText,
                extractedText.length(),
                fileAvailable,
                fileSize,
                OpsFreeToolService.guessContentType(report.getOriginalFilename()),
                events.stream().map(this::toEventView).toList(),
                inputTokens,
                outputTokens
        );
    }

    @Transactional(readOnly = true)
    public List<OpsDtos.AuditEventView> recentAuditGlobal(int limit, String familyId) {
        List<AuditEvent> events;
        if (familyId != null && !familyId.isBlank()) {
            events = auditEventRepository.findByFamilyIdOrderByCreatedAtDesc(familyId, PageRequest.of(0, limit));
        } else {
            events = auditEventRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
        }
        return events.stream().map(this::toAuditView).toList();
    }

    @Transactional(readOnly = true)
    public List<OpsDtos.ReportOpsRow> recentReports(int limit) {
        return labReportRepository.findRecentActive(PageRequest.of(0, limit))
                .stream()
                .map(report -> new OpsDtos.ReportOpsRow(
                        report.getId(),
                        report.getOriginalFilename(),
                        report.getExtractionStatus().name(),
                        report.getExtractionError(),
                        report.getPerson().getId(),
                        report.getPerson().getDisplayName(),
                        report.getFamilyId(),
                        report.getUploadedByAccountId(),
                        report.getUploadedAt()
                ))
                .toList();
    }

    private OpsDtos.ExtractionEventView toEventView(ExtractionEvent event) {
        return new OpsDtos.ExtractionEventView(
                event.getId(),
                event.getLabReportId(),
                event.getFamilyId(),
                event.getEventType(),
                event.getStatus(),
                event.getDurationMs(),
                event.getCharCount(),
                event.getBiomarkerCount(),
                event.getCoverage(),
                event.getInputTokens(),
                event.getOutputTokens(),
                event.getModel(),
                event.getMessage(),
                parseTextPreview(event.getMetadata()),
                event.getCreatedAt()
        );
    }

    private String deriveTextExtractionTool(List<ExtractionEvent> events) {
        return events.stream()
                .filter(event -> event.getEventType().startsWith("text."))
                .filter(event -> !"text.stored_retry".equals(event.getEventType()))
                .max(Comparator.comparingInt(event -> event.getCharCount() != null ? event.getCharCount() : 0))
                .map(event -> toolLabel(event.getEventType()))
                .orElse("Not recorded");
    }

    private String deriveBiomarkerParseTool(List<ExtractionEvent> events) {
        boolean rulesWin = events.stream()
                .anyMatch(event -> "parse.rules".equals(event.getEventType()) && "success".equals(event.getStatus()));
        if (rulesWin) {
            return "Rules / templates (no AI)";
        }
        if (events.stream().anyMatch(event -> "parse.claude".equals(event.getEventType()))) {
            return "Claude AI (last resort)";
        }
        if (events.stream().anyMatch(event -> "parse.skipped".equals(event.getEventType()))) {
            return "Skipped — text only";
        }
        return "Not recorded";
    }

    private static String toolLabel(String eventType) {
        return switch (eventType) {
            case "text.pdf_layer" -> "PDFBox — digital PDF text layer";
            case "text.ocr_pdf" -> "Tesseract OCR — scanned PDF";
            case "text.ocr_image" -> "Tesseract OCR — photo/image";
            case "text.plain" -> "Plain text file";
            case "text.ai_vision" -> "Claude vision — AI (last resort)";
            case "text.stored_retry" -> "Stored text from database";
            default -> eventType;
        };
    }

    private String parseTextPreview(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(metadata);
            if (node.hasNonNull("preview")) {
                return node.get("preview").asText();
            }
        } catch (Exception ignored) {
            // legacy or malformed metadata
        }
        return null;
    }

    private OpsDtos.AuditEventView toAuditView(AuditEvent event) {
        return new OpsDtos.AuditEventView(
                event.getId(),
                event.getAction(),
                event.getActor() != null ? event.getActor().getId() : null,
                event.getTargetType(),
                event.getTargetId(),
                event.getMetadata(),
                event.getCreatedAt()
        );
    }
}
