package com.phi.extraction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DocumentTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(DocumentTextExtractor.class);

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".heic", ".tif", ".tiff", ".bmp"
    );

    private final PdfTextExtractor pdfTextExtractor;
    private final OcrTextExtractor ocrTextExtractor;
    private final TextQualityAssessor textQualityAssessor;

    public DocumentTextExtractor(
            PdfTextExtractor pdfTextExtractor,
            OcrTextExtractor ocrTextExtractor,
            TextQualityAssessor textQualityAssessor
    ) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.ocrTextExtractor = ocrTextExtractor;
        this.textQualityAssessor = textQualityAssessor;
    }

    public TextExtractionResult extract(Path filePath) throws IOException, InterruptedException {
        if (pdfTextExtractor.isPdf(filePath)) {
            return extractPdf(filePath);
        }
        if (isImage(filePath)) {
            return extractImage(filePath);
        }
        if (isPlainText(filePath)) {
            String text = java.nio.file.Files.readString(filePath);
            log.info("Read plain text file ({} chars)", text.length());
            return new TextExtractionResult(text, TextExtractionMethod.PLAIN_TEXT);
        }
        throw new IOException("Unsupported file type: " + filePath.getFileName());
    }

    private TextExtractionResult extractPdf(Path filePath) throws IOException, InterruptedException {
        String textLayer = pdfTextExtractor.extractText(filePath);
        if (textQualityAssessor.isUsableTextLayer(textLayer)) {
            log.info("Extracted PDF text layer ({} chars)", textLayer.length());
            return new TextExtractionResult(textLayer, TextExtractionMethod.PDF_TEXT_LAYER);
        }

        if (!ocrTextExtractor.isAvailable()) {
            log.warn("PDF text layer insufficient and OCR unavailable — scan may fail");
            return new TextExtractionResult(textLayer, TextExtractionMethod.PDF_TEXT_LAYER);
        }

        log.info("PDF text layer insufficient ({} chars), using OCR for scanned PDF", textLayer.length());
        String ocrText = ocrTextExtractor.extractFromPdf(filePath);
        log.info("OCR extracted {} chars from scanned PDF", ocrText.length());
        return new TextExtractionResult(ocrText, TextExtractionMethod.OCR_PDF);
    }

    private TextExtractionResult extractImage(Path filePath) throws IOException, InterruptedException {
        if (!ocrTextExtractor.isAvailable()) {
            throw new IOException(
                    "Image upload requires Tesseract OCR. Install tesseract on the server and set PHI_OCR_ENABLED=true."
            );
        }
        String ocrText = ocrTextExtractor.extractFromImage(filePath);
        log.info("OCR extracted {} chars from image", ocrText.length());
        return new TextExtractionResult(ocrText, TextExtractionMethod.OCR_IMAGE);
    }

    public boolean isImage(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return IMAGE_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    public boolean isPlainText(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".txt");
    }

    /**
     * Runs every applicable free text tool on the file — for platform ops comparison.
     * Does not call Claude or pick a winner; each tool runs independently.
     */
    public List<FreeTextProbe> probeAllFreeTextTools(Path filePath) {
        List<FreeTextProbe> probes = new ArrayList<>();
        if (pdfTextExtractor.isPdf(filePath)) {
            probes.add(runPdfLayerProbe(filePath));
            if (ocrTextExtractor.isAvailable()) {
                // Ops comparison always runs every tool on the same file so you can
                // benchmark PDFBox vs Tesseract vs rules — even when the text layer is good.
                probes.add(runOcrPdfProbe(filePath));
            } else {
                probes.add(FreeTextProbe.skipped(
                        "text.ocr_pdf",
                        "Tesseract OCR — scanned PDF",
                        "Tesseract OCR not available on server"
                ));
            }
        } else if (isImage(filePath)) {
            if (ocrTextExtractor.isAvailable()) {
                probes.add(runOcrImageProbe(filePath));
            } else {
                probes.add(FreeTextProbe.skipped(
                        "text.ocr_image",
                        "Tesseract OCR — photo/image",
                        "Tesseract OCR not available on server"
                ));
            }
        } else if (isPlainText(filePath)) {
            probes.add(runPlainTextProbe(filePath));
        } else {
            probes.add(FreeTextProbe.error(
                    "unknown",
                    "Unsupported file type",
                    "Unsupported file type: " + filePath.getFileName()
            ));
        }
        return probes;
    }

    private FreeTextProbe runPdfLayerProbe(Path filePath) {
        long start = System.currentTimeMillis();
        try {
            String text = pdfTextExtractor.extractText(filePath);
            boolean usable = textQualityAssessor.isUsableTextLayer(text);
            String status = usable ? "success" : (text == null || text.isBlank() ? "error" : "warning");
            return new FreeTextProbe(
                    "text.pdf_layer",
                    "PDFBox — digital PDF text layer",
                    status,
                    System.currentTimeMillis() - start,
                    text,
                    null,
                    usable,
                    false
            );
        } catch (Exception e) {
            return FreeTextProbe.error(
                    "text.pdf_layer",
                    "PDFBox — digital PDF text layer",
                    e.getMessage()
            );
        }
    }

    private FreeTextProbe runOcrPdfProbe(Path filePath) {
        long start = System.currentTimeMillis();
        try {
            String text = ocrTextExtractor.extractFromPdf(filePath);
            boolean lowQuality = textQualityAssessor.looksLowQualityOcr(text);
            String status = text == null || text.isBlank() ? "error" : (lowQuality ? "warning" : "success");
            return new FreeTextProbe(
                    "text.ocr_pdf",
                    "Tesseract OCR — scanned PDF",
                    status,
                    System.currentTimeMillis() - start,
                    text,
                    null,
                    !lowQuality && text != null && !text.isBlank(),
                    lowQuality
            );
        } catch (Exception e) {
            return FreeTextProbe.error(
                    "text.ocr_pdf",
                    "Tesseract OCR — scanned PDF",
                    e.getMessage()
            );
        }
    }

    private FreeTextProbe runOcrImageProbe(Path filePath) {
        long start = System.currentTimeMillis();
        try {
            String text = ocrTextExtractor.extractFromImage(filePath);
            boolean lowQuality = textQualityAssessor.looksLowQualityOcr(text);
            String status = text == null || text.isBlank() ? "error" : (lowQuality ? "warning" : "success");
            return new FreeTextProbe(
                    "text.ocr_image",
                    "Tesseract OCR — photo/image",
                    status,
                    System.currentTimeMillis() - start,
                    text,
                    null,
                    !lowQuality && text != null && !text.isBlank(),
                    lowQuality
            );
        } catch (Exception e) {
            return FreeTextProbe.error(
                    "text.ocr_image",
                    "Tesseract OCR — photo/image",
                    e.getMessage()
            );
        }
    }

    private FreeTextProbe runPlainTextProbe(Path filePath) {
        long start = System.currentTimeMillis();
        try {
            String text = Files.readString(filePath);
            return new FreeTextProbe(
                    "text.plain",
                    "Plain text file",
                    text == null || text.isBlank() ? "error" : "success",
                    System.currentTimeMillis() - start,
                    text,
                    null,
                    text != null && !text.isBlank(),
                    false
            );
        } catch (Exception e) {
            return FreeTextProbe.error("text.plain", "Plain text file", e.getMessage());
        }
    }
}
