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
import java.util.UUID;

@Entity
@Table(name = "family_memberships")
public class FamilyMembership {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Relationship relationship;

    @Enumerated(EnumType.STRING)
    @Column(name = "family_role", nullable = false, length = 16)
    private FamilyRole familyRole = FamilyRole.member;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    @Column(name = "left_at")
    private Instant leftAt;

    protected FamilyMembership() {
    }

    public FamilyMembership(Family family, Person person, Relationship relationship, FamilyRole familyRole) {
        this.id = UUID.randomUUID().toString();
        this.family = family;
        this.person = person;
        this.relationship = relationship;
        this.familyRole = familyRole;
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

    public Relationship getRelationship() {
        return relationship;
    }

    public FamilyRole getFamilyRole() {
        return familyRole;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Instant getLeftAt() {
        return leftAt;
    }

    public boolean isActive() {
        return leftAt == null;
    }

    public void leave() {
        this.leftAt = Instant.now();
    }

    public void setFamilyRole(FamilyRole familyRole) {
        this.familyRole = familyRole;
    }
}
