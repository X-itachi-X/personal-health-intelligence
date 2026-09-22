package com.phi.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByEmailIgnoreCase(String email);

    @Query("""
            SELECT a FROM Account a
            JOIN FETCH a.person
            WHERE LOWER(a.email) = LOWER(:email)
            """)
    Optional<Account> findByEmailIgnoreCaseWithPerson(@Param("email") String email);

    @Query("""
            SELECT a FROM Account a
            JOIN FETCH a.person
            WHERE a.id = :id
            """)
    Optional<Account> findByIdWithPerson(@Param("id") String id);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM Account a
            WHERE a.person.id = :personId
            """)
    boolean existsByPersonId(@Param("personId") Long personId);
}
