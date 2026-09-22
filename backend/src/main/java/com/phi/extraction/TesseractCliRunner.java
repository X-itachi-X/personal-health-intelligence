package com.phi.extraction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Runs Tesseract as a separate OS process so native crashes (SIGSEGV) cannot take down the JVM.
 */
@Component
public class TesseractCliRunner {

    private static final Logger log = LoggerFactory.getLogger(TesseractCliRunner.class);
    private static final long PAGE_TIMEOUT_SECONDS = 120;

    public boolean isAvailable() {
        try {
            Process process = new ProcessBuilder("tesseract", "--version").start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("Tesseract CLI not available: {}", e.getMessage());
            return false;
        }
    }

    public String ocrImage(Path imagePath, String languages, int pageSegMode, String tessdataPath)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("tesseract");
        command.add(imagePath.toString());
        command.add("stdout");
        command.add("-l");
        command.add(languages);
        command.add("--psm");
        command.add(String.valueOf(pageSegMode));
        if (tessdataPath != null && !tessdataPath.isBlank()) {
            command.add("--tessdata-dir");
            command.add(tessdataPath);
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean finished = process.waitFor(PAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Tesseract timed out after " + PAGE_TIMEOUT_SECONDS + "s");
        }
        if (process.exitValue() != 0) {
            throw new IOException("Tesseract failed (exit " + process.exitValue() + "): " + truncate(output));
        }
        return output.trim();
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= 200) {
            return value;
        }
        return value.substring(0, 200) + "…";
    }
}
