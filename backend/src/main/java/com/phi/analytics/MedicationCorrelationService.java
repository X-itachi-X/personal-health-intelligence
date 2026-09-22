package com.phi.analytics;

import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.MedicationCourseType;
import com.phi.domain.MedicationEvent;
import com.phi.domain.MedicationEventRepository;
import com.phi.medication.MedicationDtos;
import com.phi.medication.MedicationService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicationCorrelationService {

    private static final Map<String, List<String>> MEDICATION_BIOMARKERS = Map.ofEntries(
            Map.entry("metformin", List.of("fasting_glucose", "hba1c")),
            Map.entry("glimepiride", List.of("fasting_glucose", "hba1c")),
            Map.entry("sitagliptin", List.of("fasting_glucose", "hba1c")),
            Map.entry("atorvastatin", List.of("ldl_cholesterol", "hdl")),
            Map.entry("rosuvastatin", List.of("ldl_cholesterol", "hdl")),
            Map.entry("levothyroxine", List.of("tsh")),
            Map.entry("thyroxine", List.of("tsh")),
            Map.entry("amlodipine", List.of("creatinine", "potassium")),
            Map.entry("telmisartan", List.of("creatinine", "potassium"))
    );

    private final MedicationEventRepository medicationEventRepository;
    private final PersonChangeService changeService;

    public MedicationCorrelationService(
            MedicationEventRepository medicationEventRepository,
            PersonChangeService changeService
    ) {
        this.medicationEventRepository = medicationEventRepository;
        this.changeService = changeService;
    }

    @Transactional(readOnly = true)
    public List<AnalyticsDtos.InsightCard> correlationCards(AuthenticatedAccount account, Long personId) {
        List<MedicationEvent> activeMeds = medicationEventRepository.findByPersonId(personId).stream()
                .filter(med -> med.getEndedOn() == null)
                .toList();
        if (activeMeds.isEmpty()) {
            return List.of();
        }

        AnalyticsDtos.PersonChangesResponse changes = changeService.changesSincePreviousReport(account, personId);
        if (changes.previousReportDate() == null) {
            return List.of();
        }

        Map<String, AnalyticsDtos.BiomarkerChange> changeByCanonical = new LinkedHashMap<>();
        for (AnalyticsDtos.BiomarkerChange change : changes.changes()) {
            changeByCanonical.put(change.canonical(), change);
        }

        List<AnalyticsDtos.InsightCard> cards = new ArrayList<>();
        for (MedicationEvent medication : activeMeds) {
            List<String> canonicals = relatedCanonicals(medication.getMedicationName());
            for (String canonical : canonicals) {
                AnalyticsDtos.BiomarkerChange change = changeByCanonical.get(canonical);
                if (change == null
                        || change.percentChange() == null
                        || Math.abs(change.percentChange()) < 5) {
                    continue;
                }
                String courseLabel = medication.getCourseType() == MedicationCourseType.CHRONIC
                        ? "ongoing medication"
                        : "recent course";
                cards.add(new AnalyticsDtos.InsightCard(
                        "med-" + medication.getId() + "-" + canonical,
                        "MEDICATION_CONTEXT",
                        medication.getMedicationName() + " — " + formatName(canonical),
                        String.format(
                                "While on %s (%s), %s changed %.1f%% between %s and %s. "
                                        + "This is context only — discuss with your doctor.",
                                medication.getMedicationName(),
                                courseLabel,
                                formatName(canonical),
                                change.percentChange(),
                                changes.previousReportDate(),
                                changes.latestReportDate()
                        ),
                        "info",
                        canonical,
                        changes.latestReportId()
                ));
                break;
            }
            if (cards.size() >= 3) {
                break;
            }
        }
        return cards;
    }

    private static List<String> relatedCanonicals(String medicationName) {
        String lower = medicationName.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> entry : MEDICATION_BIOMARKERS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return List.of();
    }

    private static String formatName(String canonical) {
        return canonical.replace('_', ' ');
    }
}
