package com.phi.analytics;

import com.phi.access.AccessControlService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.FamilyMembershipRepository;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.reasoning.ReportFindingsService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FamilyHealthSnapshotService {

    private final AccessControlService accessControl;
    private final FamilyMembershipRepository membershipRepository;
    private final LabReportRepository labReportRepository;
    private final ReportFindingsService findingsService;

    public FamilyHealthSnapshotService(
            AccessControlService accessControl,
            FamilyMembershipRepository membershipRepository,
            LabReportRepository labReportRepository,
            ReportFindingsService findingsService
    ) {
        this.accessControl = accessControl;
        this.membershipRepository = membershipRepository;
        this.labReportRepository = labReportRepository;
        this.findingsService = findingsService;
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.FamilyHealthSnapshot snapshot(AuthenticatedAccount account, String familyId) {
        accessControl.requireAdvanced(account);
        accessControl.requireActiveMembership(familyId, account.personId());

        List<AnalyticsDtos.MemberSnapshot> members = new ArrayList<>();
        int outOfRangeMembers = 0;
        int reportsWithData = 0;

        for (var membership : membershipRepository.findActiveByFamilyId(familyId)) {
            Long personId = membership.getPersonId();
            List<LabReport> reports = labReportRepository.findCompletedWithReportDateByPersonId(
                    personId,
                    PageRequest.of(0, 1)
            );
            if (reports.isEmpty()) {
                members.add(new AnalyticsDtos.MemberSnapshot(
                        personId,
                        membership.getPerson().getDisplayName(),
                        0,
                        null
                ));
                continue;
            }

            reportsWithData++;
            LabReport latest = reports.getFirst();
            var findings = findingsService.findingsForReport(latest.getId());
            if (findings.outOfRangeCount() > 0) {
                outOfRangeMembers++;
            }
            members.add(new AnalyticsDtos.MemberSnapshot(
                    personId,
                    membership.getPerson().getDisplayName(),
                    findings.outOfRangeCount(),
                    latest.getReportDate().toString()
            ));
        }

        return new AnalyticsDtos.FamilyHealthSnapshot(
                familyId,
                members.size(),
                reportsWithData,
                outOfRangeMembers,
                members
        );
    }
}
