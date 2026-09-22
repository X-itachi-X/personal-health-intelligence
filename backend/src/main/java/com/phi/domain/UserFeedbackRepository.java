package com.phi.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, String> {

    @Query("""
            SELECT f FROM UserFeedback f
            JOIN FETCH f.account
            ORDER BY f.createdAt DESC
            """)
    List<UserFeedback> findRecent();
}
