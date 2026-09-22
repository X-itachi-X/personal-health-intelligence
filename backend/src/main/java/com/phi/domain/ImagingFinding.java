package com.phi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "imaging_findings")
public class ImagingFinding {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "imaging_study_id", nullable = false)
    private ImagingStudy imagingStudy;

    @Column(name = "finding_text", nullable = false, length = 1024)
    private String findingText;

    @Column(length = 16)
    private String severity;

    @Column(name = "measurement_value", length = 64)
    private String measurementValue;

    @Column(name = "measurement_unit", length = 32)
    private String measurementUnit;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ImagingFinding() {
    }

    public ImagingFinding(
            ImagingStudy imagingStudy,
            String findingText,
            String severity,
            String measurementValue,
            String measurementUnit,
            int sortOrder
    ) {
        this.id = UUID.randomUUID().toString();
        this.imagingStudy = imagingStudy;
        this.findingText = findingText;
        this.severity = severity;
        this.measurementValue = measurementValue;
        this.measurementUnit = measurementUnit;
        this.sortOrder = sortOrder;
    }

    public String getId() {
        return id;
    }

    public ImagingStudy getImagingStudy() {
        return imagingStudy;
    }

    public String getFindingText() {
        return findingText;
    }

    public String getSeverity() {
        return severity;
    }

    public String getMeasurementValue() {
        return measurementValue;
    }

    public String getMeasurementUnit() {
        return measurementUnit;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
