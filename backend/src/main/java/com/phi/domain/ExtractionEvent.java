package com.phi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "extraction_events")
public class ExtractionEvent {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "lab_report_id", nullable = false)
    private Long labReportId;

    @Column(name = "family_id", length = 36)
    private String familyId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "char_count")
    private Integer charCount;

    @Column(name = "biomarker_count")
    private Integer biomarkerCount;

    @Column
    private Double coverage;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(length = 64)
    private String model;

    @Column(length = 512)
    private String message;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ExtractionEvent() {
    }

    public ExtractionEvent(
            Long labReportId,
            String familyId,
            String eventType,
            String status,
            Long durationMs,
            Integer charCount,
            Integer biomarkerCount,
            Double coverage,
            Integer inputTokens,
            Integer outputTokens,
            String model,
            String message,
            String metadata
    ) {
        this.id = UUID.randomUUID().toString();
        this.labReportId = labReportId;
        this.familyId = familyId;
        this.eventType = eventType;
        this.status = status;
        this.durationMs = durationMs;
        this.charCount = charCount;
        this.biomarkerCount = biomarkerCount;
        this.coverage = coverage;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.model = model;
        this.message = message;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public Long getLabReportId() {
        return labReportId;
    }

    public String getFamilyId() {
        return familyId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getStatus() {
        return status;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public Integer getCharCount() {
        return charCount;
    }

    public Integer getBiomarkerCount() {
        return biomarkerCount;
    }

    public Double getCoverage() {
        return coverage;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public String getModel() {
        return model;
    }

    public String getMessage() {
        return message;
    }

    public String getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
