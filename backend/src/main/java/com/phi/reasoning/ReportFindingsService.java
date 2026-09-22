package com.phi.reasoning;

import com.phi.domain.BiomarkerValue;
import com.phi.domain.BiomarkerValueRepository;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.reasoning.ReferenceRangeAssessor.Status;
import com.phi.reasoning.yaml.YamlRuleEngine;
import com.phi.reasoning.yaml.YamlRuleEngine.ClinicalRuleFinding;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportFindingsService {

    private final LabReportRepository labReportRepository;
    private final BiomarkerValueRepository biomarkerValueRepository;
    private final YamlRuleEngine yamlRuleEngine;

    public ReportFindingsService(
            LabReportRepository labReportRepository,
            BiomarkerValueRepository biomarkerValueRepository,
            YamlRuleEngine yamlRuleEngine
    ) {
        this.labReportRepository = labReportRepository;
        this.biomarkerValueRepository = biomarkerValueRepository;
        this.yamlRuleEngine = yamlRuleEngine;
    }

    @Transactional(readOnly = true)
    public FindingsResponse findingsForReport(Long reportId) {
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        List<BiomarkerValue> biomarkers = biomarkerValueRepository.findByLabReportIdOrderByCanonicalNameAsc(reportId);
        List<Finding> findings = new ArrayList<>();
        Set<String> seenRuleIds = new LinkedHashSet<>();

        for (ClinicalRuleFinding ruleFinding : yamlRuleEngine.evaluate(biomarkers)) {
            if (!seenRuleIds.add(ruleFinding.ruleId())) {
                continue;
            }
            findings.add(new Finding(
                    "CLINICAL_RULE",
                    ruleFinding.ruleId(),
                    ruleFinding.canonical(),
                    null,
                    null,
                    null,
                    null,
                    ruleFinding.severity(),
                    ruleFinding.action(),
                    ruleFinding.message()
            ));
        }

        for (BiomarkerValue biomarker : biomarkers) {
            Status status = ReferenceRangeAssessor.assess(biomarker.getNumericValue(), biomarker.getReferenceRange());
            if (status == Status.HIGH || status == Status.LOW) {
                findings.add(new Finding(
                        "REFERENCE_RANGE",
                        null,
                        biomarker.getCanonicalName(),
                        biomarker.getRawTestName(),
                        biomarker.getNumericValue() != null ? biomarker.getNumericValue().doubleValue() : null,
                        biomarker.getUnit(),
                        biomarker.getReferenceRange(),
                        status.name(),
                        null,
                        messageFor(status, biomarker.getRawTestName(), biomarker.getReferenceRange())
                ));
            }
        }

        findings.sort(Comparator
                .comparing((Finding finding) -> finding.kind().equals("CLINICAL_RULE") ? 0 : 1)
                .thenComparing(Finding::canonical, Comparator.nullsLast(String::compareTo)));

        int attentionCount = findings.size();
        return new FindingsResponse(
                reportId,
                report.getReportDate() != null ? report.getReportDate().toString() : null,
                biomarkers.size(),
                attentionCount,
                findings
        );
    }

    private static String messageFor(Status status, String testName, String referenceRange) {
        String label = testName != null && !testName.isBlank() ? testName : "This marker";
        if (status == Status.HIGH) {
            return label + " is above the reference range (" + referenceRange + ").";
        }
        if (status == Status.LOW) {
            return label + " is below the reference range (" + referenceRange + ").";
        }
        return label + " is outside the reference range.";
    }

    public record Finding(
            String kind,
            String ruleId,
            String canonical,
            String testName,
            Double value,
            String unit,
            String referenceRange,
            String severity,
            String action,
            String message
    ) {
    }

    public record FindingsResponse(
            Long reportId,
            String reportDate,
            int biomarkerCount,
            int outOfRangeCount,
            List<Finding> findings
    ) {
    }
}
