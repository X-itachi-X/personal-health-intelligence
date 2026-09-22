package com.phi.invite;

import com.phi.access.AccessControlService;
import com.phi.audit.AuditService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.Family;
import com.phi.domain.FamilyMembershipRepository;
import com.phi.domain.FamilyRole;
import com.phi.domain.Invite;
import com.phi.domain.InviteRepository;
import com.phi.domain.Person;
import com.phi.domain.PersonRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InviteService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final InviteRepository inviteRepository;
    private final PersonRepository personRepository;
    private final FamilyMembershipRepository membershipRepository;
    private final AccessControlService accessControl;
    private final AuditService auditService;

    public InviteService(
            InviteRepository inviteRepository,
            PersonRepository personRepository,
            FamilyMembershipRepository membershipRepository,
            AccessControlService accessControl,
            AuditService auditService
    ) {
        this.inviteRepository = inviteRepository;
        this.personRepository = personRepository;
        this.membershipRepository = membershipRepository;
        this.accessControl = accessControl;
        this.auditService = auditService;
    }

    @Transactional
    public InviteDtos.InviteResponse createInvite(
            AuthenticatedAccount account,
            String familyId,
            Long personId
    ) {
        accessControl.requireAdvanced(account);
        accessControl.requireFamilyRole(account, familyId, FamilyRole.maintainer);
        accessControl.requireActiveMembership(familyId, personId);

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));
        Family family = membershipRepository.findActiveMembership(familyId, account.personId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN))
                .getFamily();

        String code = generateCode();
        Invite invite = new Invite(
                family,
                person,
                code,
                account.account(),
                Instant.now().plus(7, ChronoUnit.DAYS)
        );
        invite = inviteRepository.save(invite);

        auditService.log(account.accountId(), familyId, "invite.created", personId, "{\"code\":\"" + code + "\"}");
        return toResponse(invite);
    }

    @Transactional(readOnly = true)
    public InviteDtos.InvitePreview preview(String code) {
        return inviteRepository.findByCodeWithDetails(code)
                .map(invite -> new InviteDtos.InvitePreview(
                        invite.getCode(),
                        invite.getFamily().getDisplayName(),
                        invite.getPerson().getDisplayName(),
                        invite.isValid()
                ))
                .orElse(new InviteDtos.InvitePreview(code, null, null, false));
    }

    private InviteDtos.InviteResponse toResponse(Invite invite) {
        return new InviteDtos.InviteResponse(
                invite.getId(),
                invite.getCode(),
                invite.getFamilyId(),
                invite.getPersonId(),
                invite.getExpiresAt()
        );
    }

    private static String generateCode() {
        StringBuilder builder = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            builder.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }
}
