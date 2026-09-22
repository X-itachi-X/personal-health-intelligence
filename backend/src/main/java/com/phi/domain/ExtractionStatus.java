package com.phi.domain;

public enum ExtractionStatus {
    PENDING,
    PROCESSING,
    /** Local PDF/image text extracted and persisted; Claude not yet invoked. */
    TEXT_EXTRACTED,
    /** Text saved but lab report date could not be determined — user must supply it. */
    AWAITING_REPORT_DATE,
    /** Prescription text parsed — user must confirm extracted medications. */
    AWAITING_MEDICATION_CONFIRMATION,
    /** Imaging report parsed — user must confirm extracted study and findings. */
    AWAITING_IMAGING_CONFIRMATION,
    COMPLETED,
    TEXT_ONLY,
    FAILED
}
