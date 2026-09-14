package com.phi.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BiomarkerNormalizerTest {

    private final BiomarkerNormalizer normalizer = new BiomarkerNormalizer();

    @Test
    void normalizesCommonAliases() {
        assertEquals("vitamin_d", normalizer.normalize("Vitamin D"));
        assertEquals("lp_a", normalizer.normalize("Lipoprotein (a)"));
        assertEquals("alt", normalizer.normalize("SGPT"));
        assertEquals("hba1c", normalizer.normalize("Glycated Hemoglobin"));
    }
}
