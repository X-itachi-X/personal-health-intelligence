package com.phi.ingestion;

import com.phi.access.AccessControlService;
import com.phi.audit.AuditService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.config.PhiProperties;
import com.phi.domain.BiomarkerValueRepository;
import com.phi.domain.DocumentType;
import com.phi.domain.ExtractionStatus;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.domain.Person;
import com.phi.domain.ReportDateSource;
import com.phi.domain.PersonRepository;
import com.phi.storage.ReportFileStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReportIngestionService {

    private final PhiProperties properties;
    private final PersonRepository personRepository;
    private final LabReportRepository labReportRepository;
    private final AccessControlService accessControl;
    private final AuditService auditService;
    private final ReportFileStorageService fileStorageService;
    private final BiomarkerValueRepository biomarkerValueRepository;

    public ReportIngestionService(
            PhiProperties properties,
            PersonRepository personRepository,
            LabReportRepository labReportRepository,
            AccessControlService accessControl,
            AuditService auditService,
            ReportFileStorageService fileStorageService,
            BiomarkerValueRepository biomarkerValueRepository
    ) {
        this.properties = properties;
        this.personRepository = personRepository;
        this.labReportRepository = labReportRepository;
        this.accessControl = accessControl;
        this.auditService = auditService;
        this.fileStorageService = fileStorageService;
        this.biomarkerValueRepository = biomarkerValueRepository;
    }

    @Transactional
    public LabReport ingest(
            MultipartFile file,
            AuthenticatedAccount account,
            String familyId,
            Long personId,
            LocalDate reportDate,
            DocumentType documentType
    ) throws IOException {
        accessControl.requireAuthenticated(account);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String resolvedFamilyId = accessControl.resolveFamilyId(account, familyId);
        Long targetPersonId = personId != null ? personId : account.personId();
        accessControl.requireCanUploadFor(account, resolvedFamilyId, targetPersonId);

        Person person = personRepository.findById(targetPersonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));

        Path reportsDir = Path.of(properties.storage().reportsDir());
        Files.createDirectories(reportsDir);

        String safeName = sanitizeFilename(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "_" + safeName;
        Path destination = reportsDir.resolve(storedName);

        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        String contentHash = fileStorageService.hashFile(destination);
        DocumentType resolvedDocumentType = documentType != null ? documentType : DocumentType.LAB_REPORT;
        var existing = labReportRepository.findActiveByPersonIdAndContentHashAndDocumentType(
                targetPersonId,
                contentHash,
                resolvedDocumentType
        );
        if (existing.isPresent()) {
            LabReport duplicate = existing.get();
            if (duplicate.getExtractionStatus() == ExtractionStatus.FAILED) {
                LabReport reingested = reingestFailedReport(duplicate, destination, safeName, contentHash, account);
                if (reportDate != null) {
                    applyUserReportDate(reingested, reportDate);
                    reingested = labReportRepository.save(reingested);
                }
                return reingested;
            }
            Files.deleteIfExists(destination);
            throw new DuplicateReportException(
                    duplicate.getId(),
                    duplicate.getOriginalFilename(),
                    duplicate.getExtractionStatus()
            );
        }

        LabReport report = new LabReport(
                person,
                resolvedFamilyId,
                account.accountId(),
                safeName,
                destination.toString(),
                contentHash,
                documentType != null ? documentType : DocumentType.LAB_REPORT
        );
        if (reportDate != null) {
            applyUserReportDate(report, reportDate);
        }
        report = labReportRepository.save(report);

        auditService.log(
                account.accountId(),
                resolvedFamilyId,
                "report.uploaded",
                person.getId(),
                "{\"reportId\":" + report.getId() + "}"
        );
        return report;
    }

    @Transactional
    public LabReport replaceFailedReport(
            MultipartFile file,
            AuthenticatedAccount account,
            Long reportId,
            LocalDate reportDate
    ) throws IOException {
        accessControl.requireAuthenticated(account);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        accessControl.requireCanUploadFor(account, report.getFamilyId(), report.getPerson().getId());

        if (report.getExtractionStatus() != ExtractionStatus.FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only failed reports can be replaced. Use retry for other cases."
            );
        }

        Path reportsDir = Path.of(properties.storage().reportsDir());
        Files.createDirectories(reportsDir);

        String safeName = sanitizeFilename(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "_" + safeName;
        Path destination = reportsDir.resolve(storedName);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        String contentHash = fileStorageService.hashFile(destination);
        var duplicate = labReportRepository.findActiveByPersonIdAndContentHashAndDocumentType(
                report.getPerson().getId(),
                contentHash,
                report.getDocumentType()
        );
        if (duplicate.isPresent() && !duplicate.get().getId().equals(reportId)) {
            Files.deleteIfExists(destination);
            LabReport other = duplicate.get();
            throw new DuplicateReportException(
                    other.getId(),
                    other.getOriginalFilename(),
                    other.getExtractionStatus()
            );
        }

        LabReport reingested = reingestFailedReport(report, destination, safeName, contentHash, account);
        if (reportDate != null) {
            applyUserReportDate(reingested, reportDate);
            reingested = labReportRepository.save(reingested);
        }
        return reingested;
    }

    private static void applyUserReportDate(LabReport report, LocalDate reportDate) {
        report.setReportDate(reportDate);
        report.setReportDateSource(ReportDateSource.USER);
    }

    @Transactional
    public void softDelete(AuthenticatedAccount account, Long reportId) {
        accessControl.requireAuthenticated(account);
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean isUploader = report.getUploadedByAccountId() != null
                && report.getUploadedByAccountId().equals(account.accountId());
        boolean isAdmin = false;
        if (report.getFamilyId() != null) {
            try {
                accessControl.requireFamilyRole(
                        account,
                        report.getFamilyId(),
                        com.phi.domain.FamilyRole.admin
                );
                isAdmin = true;
            } catch (ResponseStatusException ignored) {
                // not admin
            }
        }

        if (!isUploader && !isAdmin && !report.getPerson().getId().equals(account.personId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        report.softDelete();
        labReportRepository.save(report);
        auditService.log(
                account.accountId(),
                report.getFamilyId(),
                "report.deleted",
                report.getPerson().getId(),
                "{\"reportId\":" + reportId + "}"
        );
    }

    private LabReport reingestFailedReport(
            LabReport report,
            Path destination,
            String safeName,
            String contentHash,
            AuthenticatedAccount account
    ) throws IOException {
        String previousPath = report.getStoragePath();
        if (previousPath != null && !previousPath.isBlank() && !previousPath.equals(destination.toString())) {
            Files.deleteIfExists(Path.of(previousPath));
        }

        biomarkerValueRepository.deleteByLabReportId(report.getId());
        report.resetForReExtraction(destination.toString(), safeName, contentHash);
        report = labReportRepository.save(report);

        auditService.log(
                account.accountId(),
                report.getFamilyId(),
                "report.reingested",
                report.getPerson().getId(),
                "{\"reportId\":" + report.getId() + "}"
        );
        return report;
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "report.bin";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
