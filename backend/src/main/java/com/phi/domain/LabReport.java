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
import java.time.LocalDate;

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

    @Column(length = 1024)
    private String storagePath;

    @Column(length = 64)
    private String contentHash;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_date_source", length = 16)
    private ReportDateSource reportDateSource;

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

    @Column(name = "family_id", length = 36)
    private String familyId;

    @Column(name = "uploaded_by_account_id", length = 36)
    private String uploadedByAccountId;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 16)
    private DocumentType documentType = DocumentType.LAB_REPORT;

    @Column(name = "prescription_extract", columnDefinition = "CLOB")
    private String prescriptionExtract;

    @Column(name = "imaging_extract", columnDefinition = "CLOB")
    private String imagingExtract;

    protected LabReport() {
    }

    public LabReport(
            Person person,
            String familyId,
            String uploadedByAccountId,
            String originalFilename,
            String storagePath,
            String contentHash
    ) {
        this.person = person;
        this.familyId = familyId;
        this.uploadedByAccountId = uploadedByAccountId;
        this.originalFilename = originalFilename;
        this.storagePath = storagePath;
        this.contentHash = contentHash;
    }

    public LabReport(
            Person person,
            String familyId,
            String uploadedByAccountId,
            String originalFilename,
            String storagePath,
            String contentHash,
            DocumentType documentType
    ) {
        this(person, familyId, uploadedByAccountId, originalFilename, storagePath, contentHash);
        this.documentType = documentType != null ? documentType : DocumentType.LAB_REPORT;
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

    public String getContentHash() {
        return contentHash;
    }

    public void clearStoragePath() {
        this.storagePath = null;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public ReportDateSource getReportDateSource() {
        return reportDateSource;
    }

    public void setReportDateSource(ReportDateSource reportDateSource) {
        this.reportDateSource = reportDateSource;
    }

    public boolean hasReportDate() {
        return reportDate != null;
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

    public void markTextExtracted(String extractedText) {
        this.extractionStatus = ExtractionStatus.TEXT_EXTRACTED;
        this.extractedText = extractedText;
        this.extractedAt = Instant.now();
        this.extractionError = null;
    }

    public void markAwaitingReportDate(String extractedText) {
        this.extractionStatus = ExtractionStatus.AWAITING_REPORT_DATE;
        this.extractedText = extractedText;
        this.extractedAt = Instant.now();
        this.extractionError = null;
    }

    public void markAwaitingMedicationConfirmation(String extractedText, String prescriptionExtractJson) {
        this.extractionStatus = ExtractionStatus.AWAITING_MEDICATION_CONFIRMATION;
        this.extractedText = extractedText;
        this.prescriptionExtract = prescriptionExtractJson;
        this.extractedAt = Instant.now();
        this.extractionError = null;
    }

    public void markAwaitingImagingConfirmation(String extractedText, String imagingExtractJson) {
        this.extractionStatus = ExtractionStatus.AWAITING_IMAGING_CONFIRMATION;
        this.extractedText = extractedText;
        this.imagingExtract = imagingExtractJson;
        this.extractedAt = Instant.now();
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

    public void resetForReExtraction(String storagePath, String originalFilename, String contentHash) {
        this.storagePath = storagePath;
        this.originalFilename = originalFilename;
        this.contentHash = contentHash;
        this.extractionStatus = ExtractionStatus.PENDING;
        this.extractedText = null;
        this.extractionError = null;
        this.extractedAt = null;
        this.reportDate = null;
        this.reportDateSource = null;
        this.uploadedAt = Instant.now();
    }

    public String getFamilyId() {
        return familyId;
    }

    public String getUploadedByAccountId() {
        return uploadedByAccountId;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getPrescriptionExtract() {
        return prescriptionExtract;
    }

    public void setPrescriptionExtract(String prescriptionExtract) {
        this.prescriptionExtract = prescriptionExtract;
    }

    public boolean isPrescription() {
        return documentType == DocumentType.PRESCRIPTION;
    }

    public String getImagingExtract() {
        return imagingExtract;
    }

    public void setImagingExtract(String imagingExtract) {
        this.imagingExtract = imagingExtract;
    }

    public boolean isImagingReport() {
        return documentType == DocumentType.IMAGING_REPORT;
    }
}
