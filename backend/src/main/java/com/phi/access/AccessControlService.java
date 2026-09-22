package com.phi.access;

import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.FamilyMembership;
import com.phi.domain.FamilyMembershipRepository;
import com.phi.domain.FamilyRole;
import com.phi.domain.LabReport;
import com.phi.domain.UiMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccessControlService {

    private final FamilyMembershipRepository membershipRepository;
    private final PlatformAccessService platformAccess;

    public AccessControlService(
            FamilyMembershipRepository membershipRepository,
            PlatformAccessService platformAccess
    ) {
        this.membershipRepository = membershipRepository;
        this.platformAccess = platformAccess;
    }

    public boolean hasAdvancedAccess(AuthenticatedAccount account) {
        if (account == null) {
            return false;
        }
        if (account.uiMode() == UiMode.advanced) {
            return true;
        }
        return platformAccess.isPlatformAdmin(account);
    }

    public void requireAuthenticated(AuthenticatedAccount account) {
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    public FamilyMembership requireActiveMembership(String familyId, Long personId) {
        return membershipRepository.findActiveMembership(familyId, personId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a family member"));
    }

    public boolean sameFamily(Long viewerPersonId, Long targetPersonId) {
        List<FamilyMembership> viewerFamilies = membershipRepository.findActiveByPersonId(viewerPersonId);
        for (FamilyMembership membership : viewerFamilies) {
            if (membershipRepository.existsByFamilyIdAndPersonIdAndLeftAtIsNull(
                    membership.getFamilyId(),
                    targetPersonId
            )) {
                return true;
            }
        }
        return false;
    }

    public boolean canReadReport(AuthenticatedAccount account, LabReport report) {
        if (report.isDeleted()) {
            return false;
        }
        if (report.getPerson().getId().equals(account.personId())) {
            return true;
        }
        if (!hasAdvancedAccess(account)) {
            return false;
        }
        return sameFamily(account.personId(), report.getPerson().getId());
    }

    public void requireCanReadReport(AuthenticatedAccount account, LabReport report) {
        requireAuthenticated(account);
        if (!canReadReport(account, report)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public boolean canUploadFor(AuthenticatedAccount account, String familyId, Long personId) {
        requireActiveMembership(familyId, account.personId());
        if (personId.equals(account.personId())) {
            return true;
        }
        if (!hasAdvancedAccess(account)) {
            return false;
        }
        return membershipRepository.existsByFamilyIdAndPersonIdAndLeftAtIsNull(familyId, personId);
    }

    public void requireCanUploadFor(AuthenticatedAccount account, String familyId, Long personId) {
        requireAuthenticated(account);
        if (!canUploadFor(account, familyId, personId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot upload for this person");
        }
    }

    public void requireAdvanced(AuthenticatedAccount account) {
        requireAuthenticated(account);
        if (!hasAdvancedAccess(account)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Advanced mode required");
        }
    }

    public void requireFamilyRole(
            AuthenticatedAccount account,
            String familyId,
            FamilyRole minimumRole
    ) {
        FamilyMembership membership = requireActiveMembership(familyId, account.personId());
        if (membership.getFamilyRole().ordinal() > minimumRole.ordinal()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient family role");
        }
    }

    public String resolveFamilyId(AuthenticatedAccount account, String requestedFamilyId) {
        if (requestedFamilyId != null && !requestedFamilyId.isBlank()) {
            requireActiveMembership(requestedFamilyId, account.personId());
            return requestedFamilyId;
        }
        List<FamilyMembership> memberships = membershipRepository.findActiveByPersonId(account.personId());
        if (memberships.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active family membership");
        }
        return memberships.getFirst().getFamilyId();
    }
}
