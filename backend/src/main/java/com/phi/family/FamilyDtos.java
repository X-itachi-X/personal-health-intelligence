package com.phi.family;

import com.phi.domain.FamilyRole;
import com.phi.domain.Relationship;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class FamilyDtos {

    private FamilyDtos() {
    }

    public record FamilySummary(
            String id,
            String displayName,
            FamilyRole myRole,
            Relationship myRelationship,
            int memberCount
    ) {
    }

    public record MemberSummary(
            Long personId,
            String displayName,
            Relationship relationship,
            FamilyRole role,
            boolean hasAccount
    ) {
    }

    public record FamilyDetail(
            String id,
            String displayName,
            List<MemberSummary> members
    ) {
    }

    public record RenameFamilyRequest(
            @NotBlank String displayName
    ) {
    }

    public record AddMemberRequest(
            @NotBlank String displayName,
            @NotNull LocalDate dateOfBirth,
            @NotBlank String sex,
            @NotNull Relationship relationship,
            @NotNull FamilyRole role
    ) {
    }

    public record FeedItem(
            Long reportId,
            String personName,
            String filename,
            String extractionStatus,
            Instant uploadedAt
    ) {
    }
}
