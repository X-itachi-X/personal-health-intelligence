package com.phi.family;

import com.phi.access.AccessControlService;
import com.phi.audit.AuditService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.AccountRepository;
import com.phi.domain.Family;
import com.phi.domain.FamilyMembership;
import com.phi.domain.FamilyMembershipRepository;
import com.phi.domain.FamilyRepository;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.domain.Person;
import com.phi.domain.PersonRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final FamilyMembershipRepository membershipRepository;
    private final PersonRepository personRepository;
    private final AccountRepository accountRepository;
    private final LabReportRepository labReportRepository;
    private final AccessControlService accessControl;
    private final AuditService auditService;

    public FamilyService(
            FamilyRepository familyRepository,
            FamilyMembershipRepository membershipRepository,
            PersonRepository personRepository,
            AccountRepository accountRepository,
            LabReportRepository labReportRepository,
            AccessControlService accessControl,
            AuditService auditService
    ) {
        this.familyRepository = familyRepository;
        this.membershipRepository = membershipRepository;
        this.personRepository = personRepository;
        this.accountRepository = accountRepository;
        this.labReportRepository = labReportRepository;
        this.accessControl = accessControl;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<FamilyDtos.FamilySummary> listFamilies(AuthenticatedAccount account) {
        accessControl.requireAuthenticated(account);
        return membershipRepository.findActiveByPersonId(account.personId()).stream()
                .map(membership -> new FamilyDtos.FamilySummary(
                        membership.getFamilyId(),
                        membership.getFamily().getDisplayName(),
                        membership.getFamilyRole(),
                        membership.getRelationship(),
                        membershipRepository.findActiveByFamilyId(membership.getFamilyId()).size()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public FamilyDtos.FamilyDetail getFamily(AuthenticatedAccount account, String familyId) {
        accessControl.requireAdvanced(account);
        accessControl.requireActiveMembership(familyId, account.personId());
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<FamilyDtos.MemberSummary> members = membershipRepository.findActiveByFamilyId(familyId).stream()
                .map(this::toMemberSummary)
                .toList();
        return new FamilyDtos.FamilyDetail(family.getId(), family.getDisplayName(), members);
    }

    @Transactional
    public FamilyDtos.FamilyDetail renameFamily(
            AuthenticatedAccount account,
            String familyId,
            String displayName
    ) {
        accessControl.requireAdvanced(account);
        accessControl.requireFamilyRole(account, familyId, com.phi.domain.FamilyRole.admin);
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        family.setDisplayName(displayName);
        familyRepository.save(family);
        return getFamily(account, familyId);
    }

    @Transactional
    public FamilyDtos.MemberSummary addMember(
            AuthenticatedAccount account,
            String familyId,
            FamilyDtos.AddMemberRequest request
    ) {
        accessControl.requireAdvanced(account);
        accessControl.requireFamilyRole(account, familyId, com.phi.domain.FamilyRole.maintainer);
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Person person = Person.withProfile(
                request.displayName(),
                request.dateOfBirth(),
                request.sex(),
                null,
                null,
                null,
                null,
                null,
                null
        );
        person = personRepository.save(person);

        FamilyMembership membership = new FamilyMembership(
                family,
                person,
                request.relationship(),
                request.role()
        );
        membership = membershipRepository.save(membership);

        auditService.log(
                account.accountId(),
                familyId,
                "member.added",
                person.getId(),
                null
        );
        return toMemberSummary(membership);
    }

    @Transactional(readOnly = true)
    public List<FamilyDtos.FeedItem> familyFeed(AuthenticatedAccount account, String familyId) {
        accessControl.requireAuthenticated(account);
        String resolvedFamilyId = accessControl.resolveFamilyId(account, familyId);
        return labReportRepository.findActiveByFamilyId(resolvedFamilyId).stream()
                .filter(report -> accessControl.canReadReport(account, report))
                .map(this::toFeedItem)
                .toList();
    }

    private FamilyDtos.MemberSummary toMemberSummary(FamilyMembership membership) {
        boolean hasAccount = accountRepository.existsByPersonId(membership.getPersonId());
        return new FamilyDtos.MemberSummary(
                membership.getPersonId(),
                membership.getPerson().getDisplayName(),
                membership.getRelationship(),
                membership.getFamilyRole(),
                hasAccount
        );
    }

    private FamilyDtos.FeedItem toFeedItem(LabReport report) {
        return new FamilyDtos.FeedItem(
                report.getId(),
                report.getPerson().getDisplayName(),
                report.getOriginalFilename(),
                report.getExtractionStatus().name(),
                report.getUploadedAt()
        );
    }
}
