package com.phi.extraction;

import com.phi.config.PhiProperties;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OcrTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(OcrTextExtractor.class);

    private final PhiProperties properties;
    private final TesseractCliRunner cliRunner;
    private volatile Boolean available;

    public OcrTextExtractor(PhiProperties properties, TesseractCliRunner cliRunner) {
        this.properties = properties;
        this.cliRunner = cliRunner;
    }

    public boolean isEnabled() {
        return properties.ocr().enabled();
    }

    public boolean isAvailable() {
        if (!isEnabled()) {
            return false;
        }
        if (available != null) {
            return available;
        }
        synchronized (this) {
            if (available != null) {
                return available;
            }
            available = cliRunner.isAvailable();
            if (available) {
                log.info("Tesseract OCR available via CLI (languages={})", properties.ocr().languages());
            } else {
                log.warn("Tesseract OCR not available — install tesseract package on the server");
            }
            return available;
        }
    }

    public String extractFromImage(Path imagePath) throws IOException, InterruptedException {
        return cliRunner.ocrImage(
                imagePath,
                properties.ocr().languages(),
                properties.ocr().pageSegMode(),
                properties.ocr().resolvedTessdataPath()
        );
    }

    public String extractFromPdf(Path pdfPath) throws IOException, InterruptedException {
        int maxPages = properties.ocr().effectiveMaxPages();
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            int dpi = properties.ocr().effectiveRenderDpi();
            int pageCount = document.getNumberOfPages();
            int limit = maxPages > 0 ? Math.min(pageCount, maxPages) : pageCount;

            if (maxPages > 0 && pageCount > maxPages) {
                log.warn(
                        "OCR limited to first {} of {} PDF pages (set PHI_OCR_MAX_PAGES=0 for no limit)",
                        maxPages,
                        pageCount
                );
            }

            StringBuilder combined = new StringBuilder();
            for (int page = 0; page < limit; page++) {
                String pageText = ocrPdfPage(renderer, dpi, page);
                if (pageText != null && !pageText.isBlank()) {
                    if (combined.length() > 0) {
                        combined.append("\n\n");
                    }
                    combined.append(pageText.trim());
                }
            }
            return combined.toString().trim();
        }
    }

    private String ocrPdfPage(PDFRenderer renderer, int dpi, int pageIndex) {
        Path tempImage = null;
        try {
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, dpi);
            tempImage = Files.createTempFile("phi-ocr-page-", ".png");
            ImageIO.write(image, "png", tempImage.toFile());
            return cliRunner.ocrImage(
                    tempImage,
                    properties.ocr().languages(),
                    properties.ocr().pageSegMode(),
                    properties.ocr().resolvedTessdataPath()
            );
        } catch (Exception e) {
            log.warn("OCR failed on PDF page {}: {}", pageIndex + 1, e.getMessage());
            return "";
        } finally {
            if (tempImage != null) {
                try {
                    Files.deleteIfExists(tempImage);
                } catch (IOException ignored) {
                    // best effort cleanup
                }
            }
        }
    }
}
