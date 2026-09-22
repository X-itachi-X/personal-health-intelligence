package com.phi.feedback;

import com.phi.domain.FeedbackCategory;
import com.phi.domain.FeedbackStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class FeedbackDtos {

    private FeedbackDtos() {
    }

    public record SubmitRequest(
            @NotNull FeedbackCategory category,
            @NotBlank @Size(min = 3, max = 4000) String message,
            @Min(1) @Max(5) Integer rating,
            @Size(max = 256) String screenContext,
            @Size(max = 32) String appPlatform,
            @Size(max = 64) String appVersion
    ) {
    }

    public record SubmitResponse(
            String id,
            Instant createdAt
    ) {
    }

    public record FeedbackView(
            String id,
            String accountId,
            String submitterEmail,
            String submitterName,
            FeedbackCategory category,
            String message,
            Integer rating,
            String screenContext,
            String appPlatform,
            String appVersion,
            FeedbackStatus status,
            Instant createdAt
    ) {
    }

    public record UpdateStatusRequest(
            @NotNull FeedbackStatus status
    ) {
    }
}
