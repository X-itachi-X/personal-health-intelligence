package com.phi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invites")
public class Invite {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Account createdBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Invite() {
    }

    public Invite(Family family, Person person, String code, Account createdBy, Instant expiresAt) {
        this.id = UUID.randomUUID().toString();
        this.family = family;
        this.person = person;
        this.code = code;
        this.createdBy = createdBy;
        this.expiresAt = expiresAt;
    }

    public String getId() {
        return id;
    }

    public Family getFamily() {
        return family;
    }

    public String getFamilyId() {
        return family.getId();
    }

    public Person getPerson() {
        return person;
    }

    public Long getPersonId() {
        return person.getId();
    }

    public String getCode() {
        return code;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public boolean isValid() {
        return !isUsed() && !isExpired();
    }

    public boolean isUsed() {
        return acceptedAt != null;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void markUsed() {
        this.acceptedAt = Instant.now();
    }
}
