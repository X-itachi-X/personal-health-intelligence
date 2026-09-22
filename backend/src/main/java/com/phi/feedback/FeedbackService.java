package com.phi.feedback;

import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.Account;
import com.phi.domain.AccountRepository;
import com.phi.domain.FeedbackStatus;
import com.phi.domain.UserFeedback;
import com.phi.domain.UserFeedbackRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FeedbackService {

    private final UserFeedbackRepository feedbackRepository;
    private final AccountRepository accountRepository;

    public FeedbackService(UserFeedbackRepository feedbackRepository, AccountRepository accountRepository) {
        this.feedbackRepository = feedbackRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public FeedbackDtos.SubmitResponse submit(AuthenticatedAccount principal, FeedbackDtos.SubmitRequest request) {
        Account account = accountRepository.findByIdWithPerson(principal.accountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        UserFeedback feedback = new UserFeedback(
                account,
                account.getEmail(),
                account.getPerson().getDisplayName(),
                request.category(),
                request.message().trim(),
                request.rating(),
                blankToNull(request.screenContext()),
                blankToNull(request.appPlatform()),
                blankToNull(request.appVersion())
        );
        feedback = feedbackRepository.save(feedback);
        return new FeedbackDtos.SubmitResponse(feedback.getId(), feedback.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<FeedbackDtos.FeedbackView> listForOps() {
        return feedbackRepository.findRecent().stream().map(this::toView).toList();
    }

    @Transactional
    public FeedbackDtos.FeedbackView updateStatus(String feedbackId, FeedbackStatus status) {
        UserFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feedback not found"));
        feedback.setStatus(status);
        feedback = feedbackRepository.save(feedback);
        return toView(feedback);
    }

    private FeedbackDtos.FeedbackView toView(UserFeedback feedback) {
        return new FeedbackDtos.FeedbackView(
                feedback.getId(),
                feedback.getAccount().getId(),
                feedback.getSubmitterEmail(),
                feedback.getSubmitterName(),
                feedback.getCategory(),
                feedback.getMessage(),
                feedback.getRating(),
                feedback.getScreenContext(),
                feedback.getAppPlatform(),
                feedback.getAppVersion(),
                feedback.getStatus(),
                feedback.getCreatedAt()
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
