package com.phi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_feedback")
public class UserFeedback {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "submitter_email", nullable = false, length = 320)
    private String submitterEmail;

    @Column(name = "submitter_name", nullable = false)
    private String submitterName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FeedbackCategory category;

    @Column(nullable = false, length = 4000)
    private String message;

    @Column
    private Integer rating;

    @Column(name = "screen_context", length = 256)
    private String screenContext;

    @Column(name = "app_platform", length = 32)
    private String appPlatform;

    @Column(name = "app_version", length = 64)
    private String appVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FeedbackStatus status = FeedbackStatus.pending;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected UserFeedback() {
    }

    public UserFeedback(
            Account account,
            String submitterEmail,
            String submitterName,
            FeedbackCategory category,
            String message,
            Integer rating,
            String screenContext,
            String appPlatform,
            String appVersion
    ) {
        this.id = UUID.randomUUID().toString();
        this.account = account;
        this.submitterEmail = submitterEmail;
        this.submitterName = submitterName;
        this.category = category;
        this.message = message;
        this.rating = rating;
        this.screenContext = screenContext;
        this.appPlatform = appPlatform;
        this.appVersion = appVersion;
    }

    public String getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public String getSubmitterEmail() {
        return submitterEmail;
    }

    public String getSubmitterName() {
        return submitterName;
    }

    public FeedbackCategory getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public Integer getRating() {
        return rating;
    }

    public String getScreenContext() {
        return screenContext;
    }

    public String getAppPlatform() {
        return appPlatform;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public FeedbackStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setStatus(FeedbackStatus status) {
        this.status = status;
    }
}
