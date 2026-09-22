package com.phi.extraction;

import com.phi.config.PhiProperties;
import org.springframework.stereotype.Component;

@Component
public class TextQualityAssessor {

    private final PhiProperties properties;

    public TextQualityAssessor(PhiProperties properties) {
        this.properties = properties;
    }

    public boolean isUsableTextLayer(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() < properties.ocr().minTextLayerChars()) {
            return false;
        }
        long letters = normalized.chars().filter(Character::isLetter).count();
        return letters >= properties.ocr().minLetterChars();
    }

    /**
     * OCR from phone scans and handwriting is often messy. We still proceed, but callers may warn users.
     */
    public boolean looksLowQualityOcr(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() < properties.ocr().minOcrChars()) {
            return true;
        }
        long letters = normalized.chars().filter(Character::isLetter).count();
        long digits = normalized.chars().filter(Character::isDigit).count();
        if (letters + digits < 40) {
            return true;
        }
        double letterRatio = letters / (double) Math.max(normalized.length(), 1);
        return letterRatio < 0.15;
    }
}
