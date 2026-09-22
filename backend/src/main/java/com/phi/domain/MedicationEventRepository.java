package com.phi.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicationEventRepository extends JpaRepository<MedicationEvent, String> {

    @Query("""
            SELECT m FROM MedicationEvent m
            JOIN FETCH m.person p
            WHERE m.person.id = :personId
            ORDER BY m.startedOn DESC, m.createdAt DESC
            """)
    List<MedicationEvent> findByPersonId(@Param("personId") Long personId);
}
