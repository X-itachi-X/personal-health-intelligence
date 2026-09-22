package com.phi.invite;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class InviteDtos {

    private InviteDtos() {
    }

    public record CreateInviteRequest(
            @NotBlank String familyId,
            @NotNull Long personId
    ) {
    }

    public record InviteResponse(
            String id,
            String code,
            String familyId,
            Long personId,
            Instant expiresAt
    ) {
    }

    public record InvitePreview(
            String code,
            String familyName,
            String personName,
            boolean valid
    ) {
    }
}
