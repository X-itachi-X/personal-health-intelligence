package com.phi.analytics;

import com.phi.access.AccessControlService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.BiomarkerValue;
import com.phi.domain.BiomarkerValueRepository;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonChangeService {

    private final LabReportRepository labReportRepository;
    private final BiomarkerValueRepository biomarkerValueRepository;
    private final AccessControlService accessControl;

    public PersonChangeService(
            LabReportRepository labReportRepository,
            BiomarkerValueRepository biomarkerValueRepository,
            AccessControlService accessControl
    ) {
        this.labReportRepository = labReportRepository;
        this.biomarkerValueRepository = biomarkerValueRepository;
        this.accessControl = accessControl;
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.PersonChangesResponse changesSincePreviousReport(
            AuthenticatedAccount account,
            Long personId
    ) {
        accessControl.requireAdvanced(account);
        requireCanViewPerson(account, personId);

        List<LabReport> reports = labReportRepository.findCompletedWithReportDateByPersonId(
                personId,
                PageRequest.of(0, 2)
        );

        if (reports.isEmpty()) {
            return new AnalyticsDtos.PersonChangesResponse(
                    personId,
                    null,
                    null,
                    null,
                    null,
                    0,
                    List.of()
            );
        }

        LabReport latest = reports.getFirst();
        if (reports.size() < 2) {
            return new AnalyticsDtos.PersonChangesResponse(
                    personId,
                    latest.getId(),
                    latest.getReportDate().toString(),
                    null,
                    null,
                    0,
                    List.of()
            );
        }

        LabReport previous = reports.get(1);
        Map<String, BiomarkerValue> latestValues = indexByCanonical(
                biomarkerValueRepository.findByLabReportIdOrderByCanonicalNameAsc(latest.getId())
        );
        Map<String, BiomarkerValue> previousValues = indexByCanonical(
                biomarkerValueRepository.findByLabReportIdOrderByCanonicalNameAsc(previous.getId())
        );

        List<AnalyticsDtos.BiomarkerChange> changes = new ArrayList<>();
        for (Map.Entry<String, BiomarkerValue> entry : latestValues.entrySet()) {
            String canonical = entry.getKey();
            BiomarkerValue latestValue = entry.getValue();
            BiomarkerValue priorValue = previousValues.get(canonical);
            changes.add(toChange(canonical, latestValue, priorValue));
        }

        for (Map.Entry<String, BiomarkerValue> entry : previousValues.entrySet()) {
            if (!latestValues.containsKey(entry.getKey())) {
                changes.add(removedChange(entry.getKey(), entry.getValue()));
            }
        }

        changes.sort(Comparator
                .comparing((AnalyticsDtos.BiomarkerChange change) -> magnitude(change.percentChange()))
                .reversed()
                .thenComparing(AnalyticsDtos.BiomarkerChange::canonical));

        return new AnalyticsDtos.PersonChangesResponse(
                personId,
                latest.getId(),
                latest.getReportDate().toString(),
                previous.getId(),
                previous.getReportDate().toString(),
                changes.size(),
                changes
        );
    }

    private static Map<String, BiomarkerValue> indexByCanonical(List<BiomarkerValue> values) {
        Map<String, BiomarkerValue> indexed = new HashMap<>();
        for (BiomarkerValue value : values) {
            if (value.getNumericValue() != null) {
                indexed.put(value.getCanonicalName(), value);
            }
        }
        return indexed;
    }

    private static AnalyticsDtos.BiomarkerChange toChange(
            String canonical,
            BiomarkerValue latest,
            BiomarkerValue previous
    ) {
        if (previous == null || previous.getNumericValue() == null) {
            return new AnalyticsDtos.BiomarkerChange(
                    canonical,
                    latest.getRawTestName(),
                    latest.getUnit(),
                    latest.getNumericValue().doubleValue(),
                    null,
                    null,
                    null,
                    "NEW",
                    latest.getReferenceRange()
            );
        }

        double latestNumeric = latest.getNumericValue().doubleValue();
        double previousNumeric = previous.getNumericValue().doubleValue();
        double delta = latestNumeric - previousNumeric;
        Double percentChange = previousNumeric != 0 ? (delta / previousNumeric) * 100.0 : null;
        String direction = delta > 0 ? "UP" : delta < 0 ? "DOWN" : "UNCHANGED";

        return new AnalyticsDtos.BiomarkerChange(
                canonical,
                latest.getRawTestName(),
                latest.getUnit(),
                latestNumeric,
                previousNumeric,
                delta,
                percentChange,
                direction,
                latest.getReferenceRange()
        );
    }

    private static AnalyticsDtos.BiomarkerChange removedChange(String canonical, BiomarkerValue previous) {
        return new AnalyticsDtos.BiomarkerChange(
                canonical,
                previous.getRawTestName(),
                previous.getUnit(),
                null,
                previous.getNumericValue().doubleValue(),
                null,
                null,
                "REMOVED",
                previous.getReferenceRange()
        );
    }

    private static double magnitude(Double percentChange) {
        return percentChange == null ? -1 : Math.abs(percentChange);
    }

    private void requireCanViewPerson(AuthenticatedAccount account, Long personId) {
        if (personId.equals(account.personId())) {
            return;
        }
        if (!accessControl.sameFamily(account.personId(), personId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Cannot view analytics for this person"
            );
        }
    }
}
