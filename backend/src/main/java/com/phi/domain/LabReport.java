package com.phi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "lab_reports")
public class LabReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(nullable = false, length = 512)
    private String originalFilename;

    @Column(nullable = false, length = 1024)
    private String storagePath;

    @Column
    private Instant uploadedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExtractionStatus extractionStatus = ExtractionStatus.PENDING;

    @Column(columnDefinition = "CLOB")
    private String extractedText;

    @Column(length = 1024)
    private String extractionError;

    @Column
    private Instant extractedAt;

    protected LabReport() {
    }

    public LabReport(Person person, String originalFilename, String storagePath) {
        this.person = person;
        this.originalFilename = originalFilename;
        this.storagePath = storagePath;
    }

    public Long getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public ExtractionStatus getExtractionStatus() {
        return extractionStatus;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public String getExtractionError() {
        return extractionError;
    }

    public Instant getExtractedAt() {
        return extractedAt;
    }

    public void markProcessing() {
        this.extractionStatus = ExtractionStatus.PROCESSING;
        this.extractionError = null;
    }

    public void markCompleted(String extractedText) {
        this.extractionStatus = ExtractionStatus.COMPLETED;
        this.extractedText = extractedText;
        this.extractedAt = Instant.now();
        this.extractionError = null;
    }

    public void markTextOnly(String extractedText) {
        this.extractionStatus = ExtractionStatus.TEXT_ONLY;
        this.extractedText = extractedText;
        this.extractedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.extractionStatus = ExtractionStatus.FAILED;
        this.extractionError = error;
        this.extractedAt = Instant.now();
    }
}
