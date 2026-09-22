package com.phi.analytics;

import com.phi.access.AccessControlService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.FamilyMembershipRepository;
import com.phi.reasoning.TrendInsightAnalyzer;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsQueryService {

    private final JdbcTemplate analyticsJdbc;
    private final AnalyticsSchemaInitializer schemaInitializer;
    private final AccessControlService accessControl;
    private final FamilyMembershipRepository membershipRepository;

    public AnalyticsQueryService(
            @Qualifier("analyticsJdbcTemplate") JdbcTemplate analyticsJdbcTemplate,
            AnalyticsSchemaInitializer schemaInitializer,
            AccessControlService accessControl,
            FamilyMembershipRepository membershipRepository
    ) {
        this.analyticsJdbc = analyticsJdbcTemplate;
        this.schemaInitializer = schemaInitializer;
        this.accessControl = accessControl;
        this.membershipRepository = membershipRepository;
    }

    public AnalyticsDtos.PersonTrendResponse personTrend(
            AuthenticatedAccount account,
            Long personId,
            String canonical
    ) {
        accessControl.requireAdvanced(account);
        requireCanViewPerson(account, personId);

        schemaInitializer.ensureSchema();

        List<AnalyticsDtos.TrendPoint> points = analyticsJdbc.query("""
                SELECT report_date, numeric_value, unit, lab_report_id
                FROM fact_biomarker
                WHERE person_id = ? AND canonical_name = ?
                ORDER BY report_date ASC, uploaded_at ASC
                """,
                (rs, rowNum) -> new AnalyticsDtos.TrendPoint(
                        rs.getDate("report_date").toString(),
                        rs.getDouble("numeric_value"),
                        rs.getString("unit"),
                        rs.getLong("lab_report_id")
                ),
                personId,
                canonical
        );

        String unit = points.stream()
                .map(AnalyticsDtos.TrendPoint::unit)
                .filter(u -> u != null && !u.isBlank())
                .findFirst()
                .orElse(null);

        AnalyticsDtos.TrendInsight insight = TrendInsightAnalyzer.analyze(points);
        return new AnalyticsDtos.PersonTrendResponse(personId, canonical, unit, points, insight);
    }

    public AnalyticsDtos.FamilyCompareResponse familyCompare(
            AuthenticatedAccount account,
            String familyId,
            String canonical
    ) {
        accessControl.requireAdvanced(account);
        accessControl.requireActiveMembership(familyId, account.personId());

        schemaInitializer.ensureSchema();

        List<AnalyticsDtos.FamilyCompareMember> members = new ArrayList<>();
        membershipRepository.findActiveByFamilyId(familyId).forEach(membership -> {
            Long personId = membership.getPersonId();
            String personName = membership.getPerson().getDisplayName();

            List<AnalyticsDtos.TrendPoint> points = analyticsJdbc.query("""
                    SELECT report_date, numeric_value, unit, lab_report_id
                    FROM fact_biomarker
                    WHERE person_id = ? AND family_id = ? AND canonical_name = ?
                    ORDER BY report_date ASC, uploaded_at ASC
                    """,
                    (rs, rowNum) -> new AnalyticsDtos.TrendPoint(
                            rs.getDate("report_date").toString(),
                            rs.getDouble("numeric_value"),
                            rs.getString("unit"),
                            rs.getLong("lab_report_id")
                    ),
                    personId,
                    familyId,
                    canonical
            );

            if (!points.isEmpty()) {
                members.add(new AnalyticsDtos.FamilyCompareMember(personId, personName, points));
            }
        });

        return new AnalyticsDtos.FamilyCompareResponse(canonical, members);
    }

    public List<String> availableCanonicals(AuthenticatedAccount account, Long personId) {
        accessControl.requireAdvanced(account);
        requireCanViewPerson(account, personId);

        schemaInitializer.ensureSchema();

        return analyticsJdbc.queryForList("""
                SELECT DISTINCT canonical_name
                FROM fact_biomarker
                WHERE person_id = ?
                ORDER BY canonical_name
                """,
                String.class,
                personId
        );
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
