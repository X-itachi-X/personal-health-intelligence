package com.phi.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InviteRepository extends JpaRepository<Invite, String> {

    @Query("""
            SELECT i FROM Invite i
            JOIN FETCH i.family f
            JOIN FETCH i.person p
            WHERE i.code = :code
            """)
    Optional<Invite> findByCodeWithDetails(@Param("code") String code);
}
