package com.phi.extraction;

public enum TextExtractionMethod {
    PDF_TEXT_LAYER,
    OCR_PDF,
    OCR_IMAGE,
    PLAIN_TEXT,
    /** Last resort — only when local PDF/OCR extraction fails or returns no usable text. */
    AI_VISION
}
