package com.phi.imaging;

import com.phi.analytics.AnalyticsDtos;
import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.BiomarkerValue;
import com.phi.domain.BiomarkerValueRepository;
import com.phi.domain.ImagingFinding;
import com.phi.domain.ImagingFindingRepository;
import com.phi.domain.ImagingStudy;
import com.phi.domain.ImagingStudyRepository;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.reasoning.yaml.CrossModalRuleEngine;
import com.phi.reasoning.yaml.CrossModalRuleEngine.CrossModalContext;
import com.phi.reasoning.yaml.CrossModalRuleEngine.CrossModalFinding;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImagingCorrelationService {

    private final ImagingStudyRepository imagingStudyRepository;
    private final ImagingFindingRepository imagingFindingRepository;
    private final LabReportRepository labReportRepository;
    private final BiomarkerValueRepository biomarkerValueRepository;
    private final CrossModalRuleEngine crossModalRuleEngine;

    public ImagingCorrelationService(
            ImagingStudyRepository imagingStudyRepository,
            ImagingFindingRepository imagingFindingRepository,
            LabReportRepository labReportRepository,
            BiomarkerValueRepository biomarkerValueRepository,
            CrossModalRuleEngine crossModalRuleEngine
    ) {
        this.imagingStudyRepository = imagingStudyRepository;
        this.imagingFindingRepository = imagingFindingRepository;
        this.labReportRepository = labReportRepository;
        this.biomarkerValueRepository = biomarkerValueRepository;
        this.crossModalRuleEngine = crossModalRuleEngine;
    }

    @Transactional(readOnly = true)
    public List<AnalyticsDtos.InsightCard> correlationCards(AuthenticatedAccount account, Long personId) {
        List<ImagingStudy> studies = imagingStudyRepository.findByPersonIdOrderByStudyDateDesc(personId);
        if (studies.isEmpty()) {
            return List.of();
        }

        ImagingStudy latest = studies.getFirst();
        CrossModalContext context = new CrossModalContext(
                imagingTexts(latest),
                presentBiomarkers(personId),
                latest.getModality(),
                latest.getStudyDate().toString(),
                latest.getImpression()
        );

        return crossModalRuleEngine.evaluate(context).stream()
                .map(finding -> toCard(finding, latest))
                .limit(3)
                .toList();
    }

    private Set<String> presentBiomarkers(Long personId) {
        Set<String> canonicals = new LinkedHashSet<>();
        List<LabReport> reports = labReportRepository.findCompletedWithReportDateByPersonId(
                personId,
                PageRequest.of(0, 3)
        );
        for (LabReport report : reports) {
            for (BiomarkerValue value : biomarkerValueRepository.findByLabReportIdOrderByCanonicalNameAsc(report.getId())) {
                if (value.getCanonicalName() != null) {
                    canonicals.add(value.getCanonicalName());
                }
            }
        }
        return canonicals;
    }

    private List<String> imagingTexts(ImagingStudy study) {
        List<String> texts = new ArrayList<>();
        if (study.getImpression() != null) {
            texts.add(study.getImpression().toLowerCase(Locale.ROOT));
        }
        for (ImagingFinding finding : imagingFindingRepository.findByImagingStudyIdOrderBySortOrderAsc(study.getId())) {
            texts.add(finding.getFindingText().toLowerCase(Locale.ROOT));
        }
        return texts;
    }

    private static AnalyticsDtos.InsightCard toCard(CrossModalFinding finding, ImagingStudy study) {
        return new AnalyticsDtos.InsightCard(
                "imaging-" + finding.ruleId(),
                "IMAGING_CONTEXT",
                finding.title(),
                finding.message(),
                finding.severity(),
                null,
                study.getSourceReport() != null ? study.getSourceReport().getId() : null
        );
    }
}
