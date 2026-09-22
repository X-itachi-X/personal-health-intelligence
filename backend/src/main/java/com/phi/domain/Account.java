package com.phi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(length = 36)
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false, unique = true)
    private Person person;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "device_id")
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ui_mode", nullable = false, length = 16)
    private UiMode uiMode = UiMode.basic;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected Account() {
    }

    public Account(Person person, String email, String passwordHash, String deviceId) {
        this.id = UUID.randomUUID().toString();
        this.person = person;
        this.email = email.toLowerCase();
        this.passwordHash = passwordHash;
        this.deviceId = deviceId;
    }

    public String getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public Long getPersonId() {
        return person.getId();
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public UiMode getUiMode() {
        return uiMode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void bindDevice(String deviceId) {
        this.deviceId = deviceId;
    }

    public void recordLogin() {
        this.lastLoginAt = Instant.now();
    }

    public void setUiMode(UiMode uiMode) {
        this.uiMode = uiMode;
    }

    public void linkPerson(Person person) {
        this.person = person;
    }
}
