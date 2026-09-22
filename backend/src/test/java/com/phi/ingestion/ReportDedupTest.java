package com.phi.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.phi.access.AccessControlService;
import com.phi.audit.AuditService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.config.PhiProperties;
import com.phi.domain.Account;
import com.phi.domain.BiomarkerValueRepository;
import com.phi.domain.DocumentType;
import com.phi.domain.ExtractionStatus;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.domain.Person;
import com.phi.domain.PersonRepository;
import com.phi.storage.ReportFileStorageService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class ReportDedupTest {

    @TempDir
    Path tempDir;

    private ReportIngestionService ingestionService;
    private LabReportRepository labReportRepository;
    private Person person;
    private AuthenticatedAccount account;

    private static PhiProperties testProperties(java.nio.file.Path tempDir) {
        return new PhiProperties(
                new PhiProperties.Storage(tempDir.toString(), 0),
                new PhiProperties.Claude("", "", "", true, 0.8, false),
                new PhiProperties.Ocr(false, "eng", "", 150, 100, 80, 200, 3, 0),
                new PhiProperties.Jwt("test-secret-min-32-characters-long!!", 1),
                new PhiProperties.Google(""),
                new PhiProperties.Analytics(tempDir.resolve("analytics.duckdb").toString()),
                new PhiProperties.Platform("")
        );
    }

    @BeforeEach
    void setUp() {
        PhiProperties properties = testProperties(tempDir);

        PersonRepository personRepository = mock(PersonRepository.class);
        labReportRepository = mock(LabReportRepository.class);
        AccessControlService accessControl = mock(AccessControlService.class);
        AuditService auditService = mock(AuditService.class);
        ReportFileStorageService fileStorageService = new ReportFileStorageService(properties, labReportRepository);

        BiomarkerValueRepository biomarkerValueRepository = mock(BiomarkerValueRepository.class);

        ingestionService = new ReportIngestionService(
                properties,
                personRepository,
                labReportRepository,
                accessControl,
                auditService,
                fileStorageService,
                biomarkerValueRepository
        );

        person = new Person("Dedup Test");
        ReflectionTestUtils.setField(person, "id", 1L);
        Account accountEntity = new Account(person, "user@test.com", "hash", null);
        account = new AuthenticatedAccount(accountEntity);

        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(accessControl.resolveFamilyId(any(), any())).thenReturn("family-1");
        doNothing().when(accessControl).requireAuthenticated(any());
        doNothing().when(accessControl).requireCanUploadFor(any(), anyString(), anyLong());
        when(labReportRepository.save(any())).thenAnswer(invocation -> {
            LabReport report = invocation.getArgument(0);
            return report;
        });
    }

    @Test
    void rejectsDuplicateUploadForSamePerson() throws Exception {
        byte[] content = "same lab report bytes".getBytes(StandardCharsets.UTF_8);
        String hash = new ReportFileStorageService(
                testProperties(tempDir),
                labReportRepository
        ).hashContent(new java.io.ByteArrayInputStream(content));

        LabReport existing = new LabReport(person, "family-1", account.accountId(), "first.pdf", "/tmp/first.pdf", hash);
        existing.markCompleted("done");
        when(labReportRepository.findActiveByPersonIdAndContentHashAndDocumentType(
                anyLong(), anyString(), eq(DocumentType.LAB_REPORT)))
                .thenReturn(Optional.of(existing));

        MockMultipartFile file = new MockMultipartFile("file", "second.pdf", "application/pdf", content);

        DuplicateReportException error = assertThrows(
                DuplicateReportException.class,
                () -> ingestionService.ingest(file, account, "family-1", null, null, DocumentType.LAB_REPORT)
        );

        assertEquals("first.pdf", error.getExistingFilename());
        assertEquals(ExtractionStatus.COMPLETED, error.getExistingStatus());
        assertEquals(0, Files.list(tempDir).count());
    }

    @Test
    void reingestsFailedDuplicateInsteadOfRejecting() throws Exception {
        byte[] content = "failed lab report bytes".getBytes(StandardCharsets.UTF_8);
        String hash = new ReportFileStorageService(
                testProperties(tempDir),
                labReportRepository
        ).hashContent(new java.io.ByteArrayInputStream(content));

        LabReport existing = new LabReport(person, "family-1", account.accountId(), "failed.pdf", "/tmp/failed.pdf", hash);
        ReflectionTestUtils.setField(existing, "id", 42L);
        existing.markFailed("telemetry overflow");
        when(labReportRepository.findActiveByPersonIdAndContentHashAndDocumentType(
                anyLong(), anyString(), eq(DocumentType.LAB_REPORT)))
                .thenReturn(Optional.of(existing));

        MockMultipartFile file = new MockMultipartFile("file", "failed.pdf", "application/pdf", content);

        LabReport reingested = ingestionService.ingest(file, account, "family-1", null, null, DocumentType.LAB_REPORT);

        assertEquals(existing.getId(), reingested.getId());
        assertEquals(ExtractionStatus.PENDING, reingested.getExtractionStatus());
        assertEquals(1, Files.list(tempDir).count());
    }

    @Test
    void allowsSameFileUploadedAsDifferentDocumentType() throws Exception {
        byte[] content = "shared scan bytes".getBytes(StandardCharsets.UTF_8);
        String hash = new ReportFileStorageService(
                testProperties(tempDir),
                labReportRepository
        ).hashContent(new java.io.ByteArrayInputStream(content));

        LabReport existingLab = new LabReport(person, "family-1", account.accountId(), "panel.pdf", "/tmp/panel.pdf", hash);
        existingLab.markCompleted("done");
        when(labReportRepository.findActiveByPersonIdAndContentHashAndDocumentType(
                anyLong(), anyString(), eq(DocumentType.LAB_REPORT)))
                .thenReturn(Optional.of(existingLab));
        when(labReportRepository.findActiveByPersonIdAndContentHashAndDocumentType(
                anyLong(), anyString(), eq(DocumentType.IMAGING_REPORT)))
                .thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("file", "scan.pdf", "application/pdf", content);

        LabReport uploaded = ingestionService.ingest(
                file, account, "family-1", null, null, DocumentType.IMAGING_REPORT);

        assertEquals(DocumentType.IMAGING_REPORT, uploaded.getDocumentType());
        assertEquals(1, Files.list(tempDir).count());
    }
}
