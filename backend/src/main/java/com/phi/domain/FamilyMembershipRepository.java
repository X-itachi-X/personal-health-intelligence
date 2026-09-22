package com.phi.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FamilyMembershipRepository extends JpaRepository<FamilyMembership, String> {

    @Query("""
            SELECT m FROM FamilyMembership m
            JOIN FETCH m.family f
            JOIN FETCH m.person p
            WHERE m.person.id = :personId AND m.leftAt IS NULL
            """)
    List<FamilyMembership> findActiveByPersonId(@Param("personId") Long personId);

    @Query("""
            SELECT m FROM FamilyMembership m
            JOIN FETCH m.person p
            WHERE m.family.id = :familyId AND m.leftAt IS NULL
            """)
    List<FamilyMembership> findActiveByFamilyId(@Param("familyId") String familyId);

    @Query("""
            SELECT m FROM FamilyMembership m
            WHERE m.family.id = :familyId AND m.person.id = :personId AND m.leftAt IS NULL
            """)
    Optional<FamilyMembership> findActiveMembership(
            @Param("familyId") String familyId,
            @Param("personId") Long personId
    );

    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
            FROM FamilyMembership m
            WHERE m.family.id = :familyId AND m.person.id = :personId AND m.leftAt IS NULL
            """)
    boolean existsByFamilyIdAndPersonIdAndLeftAtIsNull(
            @Param("familyId") String familyId,
            @Param("personId") Long personId
    );
}
