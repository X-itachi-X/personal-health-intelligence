package com.phi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "persons")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 16)
    private String sex;

    @Column(name = "height_cm")
    private BigDecimal heightCm;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "blood_group", length = 8)
    private String bloodGroup;

    @Column(length = 1024)
    private String medications;

    @Column(length = 1024)
    private String conditions;

    @Column(length = 1024)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Person() {
    }

    public Person(String displayName) {
        this.displayName = displayName;
    }

    public static Person withProfile(
            String displayName,
            LocalDate dateOfBirth,
            String sex,
            BigDecimal heightCm,
            BigDecimal weightKg,
            String bloodGroup,
            String medications,
            String conditions,
            String notes
    ) {
        Person person = new Person(displayName);
        person.dateOfBirth = dateOfBirth;
        person.sex = sex;
        person.heightCm = heightCm;
        person.weightKg = weightKg;
        person.bloodGroup = bloodGroup;
        person.medications = medications;
        person.conditions = conditions;
        person.notes = notes;
        return person;
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getSex() {
        return sex;
    }

    public BigDecimal getHeightCm() {
        return heightCm;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public String getMedications() {
        return medications;
    }

    public String getConditions() {
        return conditions;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void updateProfile(
            String displayName,
            LocalDate dateOfBirth,
            String sex,
            BigDecimal heightCm,
            BigDecimal weightKg,
            String bloodGroup,
            String medications,
            String conditions,
            String notes
    ) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName;
        }
        this.dateOfBirth = dateOfBirth;
        this.sex = sex;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.bloodGroup = bloodGroup;
        this.medications = medications;
        this.conditions = conditions;
        this.notes = notes;
    }
}
