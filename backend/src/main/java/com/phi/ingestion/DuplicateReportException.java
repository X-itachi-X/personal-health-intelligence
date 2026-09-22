package com.phi.ingestion;

import com.phi.domain.ExtractionStatus;

public class DuplicateReportException extends RuntimeException {

    private final Long existingReportId;
    private final String existingFilename;
    private final ExtractionStatus existingStatus;

    public DuplicateReportException(Long existingReportId, String existingFilename, ExtractionStatus existingStatus) {
        super("This file was already uploaded for this person.");
        this.existingReportId = existingReportId;
        this.existingFilename = existingFilename;
        this.existingStatus = existingStatus;
    }

    public Long getExistingReportId() {
        return existingReportId;
    }

    public String getExistingFilename() {
        return existingFilename;
    }

    public ExtractionStatus getExistingStatus() {
        return existingStatus;
    }
}
