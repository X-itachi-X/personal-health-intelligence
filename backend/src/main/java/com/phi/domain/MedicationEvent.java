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
@Table(name = "medication_events")
public class MedicationEvent {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "family_id", length = 36)
    private String familyId;

    @Column(name = "medication_name", nullable = false, length = 255)
    private String medicationName;

    @Column(length = 128)
    private String dosage;

    @Column(name = "started_on")
    private LocalDate startedOn;

    @Column(name = "ended_on")
    private LocalDate endedOn;

    @Column(length = 1024)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_type", nullable = false, length = 16)
    private MedicationCourseType courseType = MedicationCourseType.UNKNOWN;

    /** Human-readable schedule, e.g. "1-0-1 after food" or "morning and night". */
    @Column(name = "schedule_text", length = 255)
    private String scheduleText;

    /** For acute courses — number of days prescribed (may set expected_end_on). */
    @Column(name = "duration_days")
    private Integer durationDays;

    /** Suggested end for acute courses; null for chronic until explicitly ended. */
    @Column(name = "expected_end_on")
    private LocalDate expectedEndOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MedicationSource source = MedicationSource.MANUAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_report_id")
    private LabReport sourceReport;

    @Column(name = "created_by_account_id", length = 36)
    private String createdByAccountId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected MedicationEvent() {
    }

    public MedicationEvent(
            Person person,
            String familyId,
            String medicationName,
            String dosage,
            LocalDate startedOn,
            LocalDate endedOn,
            String notes,
            MedicationCourseType courseType,
            String scheduleText,
            Integer durationDays,
            LocalDate expectedEndOn,
            MedicationSource source,
            LabReport sourceReport,
            String createdByAccountId
    ) {
        this.id = UUID.randomUUID().toString();
        this.person = person;
        this.familyId = familyId;
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.startedOn = startedOn;
        this.endedOn = endedOn;
        this.notes = notes;
        this.courseType = courseType != null ? courseType : MedicationCourseType.UNKNOWN;
        this.scheduleText = scheduleText;
        this.durationDays = durationDays;
        this.expectedEndOn = expectedEndOn;
        this.source = source != null ? source : MedicationSource.MANUAL;
        this.sourceReport = sourceReport;
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

    public String getMedicationName() {
        return medicationName;
    }

    public String getDosage() {
        return dosage;
    }

    public LocalDate getStartedOn() {
        return startedOn;
    }

    public LocalDate getEndedOn() {
        return endedOn;
    }

    public String getNotes() {
        return notes;
    }

    public MedicationCourseType getCourseType() {
        return courseType;
    }

    public String getScheduleText() {
        return scheduleText;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public LocalDate getExpectedEndOn() {
        return expectedEndOn;
    }

    public MedicationSource getSource() {
        return source;
    }

    public LabReport getSourceReport() {
        return sourceReport;
    }

    public String getCreatedByAccountId() {
        return createdByAccountId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void endMedication(LocalDate endedOn) {
        this.endedOn = endedOn;
    }
}
