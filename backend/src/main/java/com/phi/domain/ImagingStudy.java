package com.phi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "imaging_studies")
public class ImagingStudy {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "family_id", length = 36)
    private String familyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_report_id")
    private LabReport sourceReport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ImagingModality modality;

    @Column(name = "body_region", length = 128)
    private String bodyRegion;

    @Column(name = "study_date", nullable = false)
    private LocalDate studyDate;

    @Column(length = 255)
    private String facility;

    @Column(columnDefinition = "CLOB")
    private String impression;

    @Column(name = "created_by_account_id", length = 36)
    private String createdByAccountId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ImagingStudy() {
    }

    public ImagingStudy(
            Person person,
            String familyId,
            LabReport sourceReport,
            ImagingModality modality,
            String bodyRegion,
            LocalDate studyDate,
            String facility,
            String impression,
            String createdByAccountId
    ) {
        this.id = UUID.randomUUID().toString();
        this.person = person;
        this.familyId = familyId;
        this.sourceReport = sourceReport;
        this.modality = modality;
        this.bodyRegion = bodyRegion;
        this.studyDate = studyDate;
        this.facility = facility;
        this.impression = impression;
        this.createdByAccountId = createdByAccountId;
    }

    public String getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public String getFamilyId() {
        return familyId;
    }

    public LabReport getSourceReport() {
        return sourceReport;
    }

    public ImagingModality getModality() {
        return modality;
    }

    public String getBodyRegion() {
        return bodyRegion;
    }

    public LocalDate getStudyDate() {
        return studyDate;
    }

    public String getFacility() {
        return facility;
    }

    public String getImpression() {
        return impression;
    }

    public String getCreatedByAccountId() {
        return createdByAccountId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
