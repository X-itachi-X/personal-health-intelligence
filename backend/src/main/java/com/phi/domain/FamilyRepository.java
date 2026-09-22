package com.phi.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyRepository extends JpaRepository<Family, String> {
    Optional<Family> findByCreatedById(String accountId);

    boolean existsByCreatedById(String accountId);
}
