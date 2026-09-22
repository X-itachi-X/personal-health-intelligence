package com.phi.storage;

import com.phi.config.PhiProperties;
import com.phi.domain.ExtractionStatus;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportFileStorageService {

    private static final Logger log = LoggerFactory.getLogger(ReportFileStorageService.class);

    private final PhiProperties properties;
    private final LabReportRepository labReportRepository;

    public ReportFileStorageService(PhiProperties properties, LabReportRepository labReportRepository) {
        this.properties = properties;
        this.labReportRepository = labReportRepository;
    }

    public String hashContent(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream digestStream = new DigestInputStream(inputStream, digest)) {
                byte[] buffer = new byte[8192];
                while (digestStream.read(buffer) != -1) {
                    // drain stream through digest
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public String hashFile(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return hashContent(inputStream);
        }
    }

    @Transactional
    public void purgeStoredFile(LabReport report) {
        String storagePath = report.getStoragePath();
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }

        Path path = Path.of(storagePath);
        try {
            if (Files.deleteIfExists(path)) {
                log.info("Purged ingest file for report {}", report.getId());
            }
        } catch (IOException e) {
            log.warn("Failed to delete ingest file for report {} at {}", report.getId(), storagePath, e);
            return;
        }

        report.clearStoragePath();
        labReportRepository.save(report);
    }

    public void purgeAfterExtractionIfDue(LabReport report) {
        if (properties.storage().effectiveRetainFilesDays() == 0) {
            purgeStoredFile(report);
        }
    }

    @Transactional
    public int purgeExpiredFiles() {
        int retainDays = properties.storage().effectiveRetainFilesDays();
        if (retainDays <= 0) {
            return 0;
        }

        Instant cutoff = Instant.now().minus(retainDays, ChronoUnit.DAYS);
        List<LabReport> due = labReportRepository.findDueForFilePurge(cutoff);
        int purged = 0;
        for (LabReport report : due) {
            purgeStoredFile(report);
            purged++;
        }
        if (purged > 0) {
            log.info("Purged {} expired ingest file(s)", purged);
        }
        return purged;
    }

    public boolean hasReadableFile(LabReport report) {
        if (report.getStoragePath() == null || report.getStoragePath().isBlank()) {
            return false;
        }
        return Files.isRegularFile(Path.of(report.getStoragePath()));
    }

    public boolean canRetryFromStoredText(LabReport report) {
        return report.getExtractedText() != null && !report.getExtractedText().isBlank();
    }

    public boolean isTerminalExtraction(ExtractionStatus status) {
        return status == ExtractionStatus.COMPLETED || status == ExtractionStatus.TEXT_ONLY;
    }
}
