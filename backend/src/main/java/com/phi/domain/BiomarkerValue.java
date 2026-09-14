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
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "biomarker_values")
public class BiomarkerValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_report_id", nullable = false)
    private LabReport labReport;

    @Column(nullable = false, length = 128)
    private String canonicalName;

    @Column(length = 255)
    private String rawTestName;

    @Column(precision = 14, scale = 4)
    private BigDecimal numericValue;

    @Column(length = 255)
    private String textValue;

    @Column(length = 64)
    private String unit;

    @Column(length = 128)
    private String referenceRange;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column
    private Integer sourcePage;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected BiomarkerValue() {
    }

    public static BiomarkerValue fromExtraction(
            LabReport report,
            String canonicalName,
            String rawTestName,
            BigDecimal numericValue,
            String textValue,
            String unit,
            String referenceRange,
            BigDecimal confidence,
            Integer sourcePage
    ) {
        BiomarkerValue value = new BiomarkerValue();
        value.labReport = report;
        value.canonicalName = canonicalName;
        value.rawTestName = rawTestName;
        value.numericValue = numericValue;
        value.textValue = textValue;
        value.unit = unit;
        value.referenceRange = referenceRange;
        value.confidence = confidence;
        value.sourcePage = sourcePage;
        return value;
    }

    public Long getId() {
        return id;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public String getRawTestName() {
        return rawTestName;
    }

    public BigDecimal getNumericValue() {
        return numericValue;
    }

    public String getTextValue() {
        return textValue;
    }

    public String getUnit() {
        return unit;
    }

    public String getReferenceRange() {
        return referenceRange;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public Integer getSourcePage() {
        return sourcePage;
    }
}
