package com.phi.auth;

import com.phi.domain.Relationship;
import com.phi.domain.UiMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            @NotBlank String displayName,
            @NotNull LocalDate dateOfBirth,
            @NotBlank String sex,
            @NotNull Relationship relationship,
            String inviteCode,
            BigDecimal heightCm,
            BigDecimal weightKg,
            String bloodGroup,
            String medications,
            String conditions,
            String notes
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record AuthResponse(
            String token,
            String accountId,
            Long personId,
            String email,
            String displayName,
            UiMode uiMode,
            boolean platformAdmin
    ) {
    }

    public record MeResponse(
            String accountId,
            Long personId,
            String email,
            String displayName,
            UiMode uiMode,
            boolean platformAdmin
    ) {
    }

    public record UiModeRequest(
            @NotNull UiMode uiMode
    ) {
    }

    public record GoogleAuthRequest(
            @NotBlank String idToken
    ) {
    }
}
