package com.phi.api;

import com.phi.auth.AuthenticatedAccount;
import com.phi.feedback.FeedbackDtos;
import com.phi.feedback.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public FeedbackDtos.SubmitResponse submit(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @Valid @RequestBody FeedbackDtos.SubmitRequest request
    ) {
        return feedbackService.submit(account, request);
    }
}
