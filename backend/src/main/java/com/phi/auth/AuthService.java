package com.phi.auth;

import com.phi.access.PlatformAccessService;
import com.phi.audit.AuditService;
import com.phi.domain.Account;
import com.phi.domain.Family;
import com.phi.domain.FamilyMembership;
import com.phi.domain.FamilyMembershipRepository;
import com.phi.domain.FamilyRepository;
import com.phi.domain.FamilyRole;
import com.phi.domain.Invite;
import com.phi.domain.InviteRepository;
import com.phi.domain.Person;
import com.phi.domain.PersonRepository;
import com.phi.domain.Relationship;
import com.phi.domain.UiMode;
import com.phi.domain.AccountRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final AccountRepository accountRepository;
    private final PersonRepository personRepository;
    private final FamilyRepository familyRepository;
    private final FamilyMembershipRepository membershipRepository;
    private final InviteRepository inviteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final PlatformAccessService platformAccess;

    public AuthService(
            AccountRepository accountRepository,
            PersonRepository personRepository,
            FamilyRepository familyRepository,
            FamilyMembershipRepository membershipRepository,
            InviteRepository inviteRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService,
            GoogleTokenVerifier googleTokenVerifier,
            PlatformAccessService platformAccess
    ) {
        this.accountRepository = accountRepository;
        this.personRepository = personRepository;
        this.familyRepository = familyRepository;
        this.membershipRepository = membershipRepository;
        this.inviteRepository = inviteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.platformAccess = platformAccess;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (accountRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        Person person = Person.withProfile(
                request.displayName(),
                request.dateOfBirth(),
                request.sex(),
                request.heightCm(),
                request.weightKg(),
                request.bloodGroup(),
                request.medications(),
                request.conditions(),
                request.notes()
        );
        person = personRepository.save(person);

        Account account = new Account(
                person,
                request.email(),
                passwordEncoder.encode(request.password()),
                null
        );
        account = accountRepository.save(account);

        if (request.inviteCode() != null && !request.inviteCode().isBlank()) {
            acceptInvite(request.inviteCode(), account);
        } else {
            createOwnedFamily(account, request.relationship());
        }

        auditService.log(account.getId(), null, "account.registered", person.getId(), null);
        return toAuthResponse(account, jwtService.createToken(account.getId(), account.getEmail()));
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        Account account = accountRepository.findByEmailIgnoreCaseWithPerson(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return toAuthResponse(account, jwtService.createToken(account.getId(), account.getEmail()));
    }

    @Transactional
    public AuthDtos.AuthResponse loginWithGoogle(AuthDtos.GoogleAuthRequest request) {
        GoogleTokenVerifier.GoogleProfile profile = googleTokenVerifier.verify(request.idToken());

        Account account = accountRepository.findByEmailIgnoreCaseWithPerson(profile.email()).orElse(null);
        if (account == null) {
            String displayName = profile.name() != null && !profile.name().isBlank()
                    ? profile.name()
                    : profile.email().split("@")[0];

            Person person = Person.withProfile(
                    displayName,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            person = personRepository.save(person);

            account = new Account(
                    person,
                    profile.email(),
                    passwordEncoder.encode(UUID.randomUUID().toString()),
                    null
            );
            account = accountRepository.save(account);
            createOwnedFamily(account, Relationship.self);
            auditService.log(account.getId(), null, "account.registered.google", person.getId(), null);
        }

        return toAuthResponse(account, jwtService.createToken(account.getId(), account.getEmail()));
    }

    @Transactional(readOnly = true)
    public AuthDtos.MeResponse me(AuthenticatedAccount principal) {
        Account account = accountRepository.findByIdWithPerson(principal.accountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toMeResponse(account);
    }

    @Transactional
    public AuthDtos.MeResponse updateUiMode(AuthenticatedAccount principal, UiMode uiMode) {
        Account account = accountRepository.findById(principal.accountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        account.setUiMode(uiMode);
        accountRepository.save(account);
        return me(new AuthenticatedAccount(account));
    }

    private void createOwnedFamily(Account account, com.phi.domain.Relationship relationship) {
        if (familyRepository.existsByCreatedById(account.getId())) {
            return;
        }
        Family family = new Family(account.getPerson().getDisplayName() + "'s Family", account);
        family = familyRepository.save(family);

        FamilyMembership membership = new FamilyMembership(
                family,
                account.getPerson(),
                relationship,
                FamilyRole.admin
        );
        membershipRepository.save(membership);
    }

    private void acceptInvite(String code, Account account) {
        Invite invite = inviteRepository.findByCodeWithDetails(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid invite code"));

        if (invite.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite already used");
        }
        if (invite.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite expired");
        }

        Person invitedPerson = invite.getPerson();
        if (!invitedPerson.getId().equals(account.getPerson().getId())) {
            invitedPerson.updateProfile(
                    account.getPerson().getDisplayName(),
                    account.getPerson().getDateOfBirth(),
                    account.getPerson().getSex(),
                    account.getPerson().getHeightCm(),
                    account.getPerson().getWeightKg(),
                    account.getPerson().getBloodGroup(),
                    account.getPerson().getMedications(),
                    account.getPerson().getConditions(),
                    account.getPerson().getNotes()
            );
            Person duplicatePerson = account.getPerson();
            account.linkPerson(invitedPerson);
            accountRepository.save(account);
            personRepository.delete(duplicatePerson);
            personRepository.save(invitedPerson);
        }

        if (!membershipRepository.existsByFamilyIdAndPersonIdAndLeftAtIsNull(
                invite.getFamily().getId(),
                account.getPerson().getId()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invite person is not linked to this family"
            );
        }

        invite.markUsed();
        inviteRepository.save(invite);
        auditService.log(
                account.getId(),
                invite.getFamily().getId(),
                "invite.accepted",
                account.getPerson().getId(),
                "{\"inviteId\":\"" + invite.getId() + "\"}"
        );
    }

    private AuthDtos.AuthResponse toAuthResponse(Account account, String token) {
        AuthenticatedAccount principal = new AuthenticatedAccount(account);
        return new AuthDtos.AuthResponse(
                token,
                account.getId(),
                account.getPerson().getId(),
                account.getEmail(),
                account.getPerson().getDisplayName(),
                account.getUiMode(),
                platformAccess.isPlatformAdmin(principal)
        );
    }

    private AuthDtos.MeResponse toMeResponse(Account account) {
        AuthenticatedAccount principal = new AuthenticatedAccount(account);
        return new AuthDtos.MeResponse(
                account.getId(),
                account.getPerson().getId(),
                account.getEmail(),
                account.getPerson().getDisplayName(),
                account.getUiMode(),
                platformAccess.isPlatformAdmin(principal)
        );
    }
}
