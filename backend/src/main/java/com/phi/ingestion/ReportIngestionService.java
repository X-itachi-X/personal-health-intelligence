package com.phi.ingestion;

import com.phi.config.PhiProperties;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.domain.Person;
import com.phi.domain.PersonRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReportIngestionService {

    private final PhiProperties properties;
    private final PersonRepository personRepository;
    private final LabReportRepository labReportRepository;

    public ReportIngestionService(
            PhiProperties properties,
            PersonRepository personRepository,
            LabReportRepository labReportRepository
    ) {
        this.properties = properties;
        this.personRepository = personRepository;
        this.labReportRepository = labReportRepository;
    }

    @Transactional
    public LabReport ingest(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        Person person = personRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No person configured"));

        Path reportsDir = Path.of(properties.storage().reportsDir());
        Files.createDirectories(reportsDir);

        String safeName = sanitizeFilename(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "_" + safeName;
        Path destination = reportsDir.resolve(storedName);

        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        LabReport report = new LabReport(person, safeName, destination.toString());
        return labReportRepository.save(report);
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "report.bin";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
