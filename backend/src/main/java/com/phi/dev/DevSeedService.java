package com.phi.dev;

import com.phi.analytics.AnalyticsSyncService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.Account;
import com.phi.domain.AccountRepository;
import com.phi.domain.BiomarkerValue;
import com.phi.domain.BiomarkerValueRepository;
import com.phi.domain.ExtractionStatus;
import com.phi.domain.Family;
import com.phi.domain.FamilyMembership;
import com.phi.domain.FamilyMembershipRepository;
import com.phi.domain.FamilyRepository;
import com.phi.domain.FamilyRole;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.domain.Person;
import com.phi.domain.PersonRepository;
import com.phi.domain.Relationship;
import com.phi.domain.UiMode;
import com.phi.dev.DevSeedCatalog.BiomarkerSpec;
import com.phi.dev.DevSeedCatalog.FamilyScenario;
import com.phi.dev.DevSeedCatalog.MemberSpec;
import com.phi.dev.DevSeedCatalog.ReportSpec;
import com.phi.dev.DevSeedCatalog.ScenarioKind;
import com.phi.domain.ReportDateSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("dev")
public class DevSeedService {

    private static final String DEMO_PASSWORD = "demo-not-for-login";

    private final AccountRepository accountRepository;
    private final PersonRepository personRepository;
    private final FamilyRepository familyRepository;
    private final FamilyMembershipRepository membershipRepository;
    private final LabReportRepository labReportRepository;
    private final BiomarkerValueRepository biomarkerValueRepository;
    private final PasswordEncoder passwordEncoder;
    private final AnalyticsSyncService analyticsSyncService;

    public DevSeedService(
            AccountRepository accountRepository,
            PersonRepository personRepository,
            FamilyRepository familyRepository,
            FamilyMembershipRepository membershipRepository,
            LabReportRepository labReportRepository,
            BiomarkerValueRepository biomarkerValueRepository,
            PasswordEncoder passwordEncoder,
            AnalyticsSyncService analyticsSyncService
    ) {
        this.accountRepository = accountRepository;
        this.personRepository = personRepository;
        this.familyRepository = familyRepository;
        this.membershipRepository = membershipRepository;
        this.labReportRepository = labReportRepository;
        this.biomarkerValueRepository = biomarkerValueRepository;
        this.passwordEncoder = passwordEncoder;
        this.analyticsSyncService = analyticsSyncService;
    }

    @Transactional
    public DevSeedDtos.SeedResponse seedFor(AuthenticatedAccount principal) {
        Account account = accountRepository.findByIdWithPerson(principal.accountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        account.setUiMode(UiMode.advanced);
        accountRepository.save(account);

        int membersAdded = 0;
        int reportsAdded = 0;
        int scenariosLoaded = 0;

        for (FamilyScenario scenario : DevSeedCatalog.all()) {
            if (userAlreadyInFamilyByName(account, scenario.displayName())) {
                continue;
            }

            SeedCounts counts = seedScenario(account, scenario);
            membersAdded += counts.membersAdded;
            reportsAdded += counts.reportsAdded;
            scenariosLoaded++;
        }

        analyticsSyncService.rebuildAll();

        int familyCount = membershipRepository.findActiveByPersonId(account.getPersonId()).size();
        boolean alreadySeeded = scenariosLoaded == 0;

        String message = alreadySeeded
                ? "All " + familyCount + " demo scenarios already loaded — switch families in Settings"
                : "Loaded " + scenariosLoaded + " scenarios (" + familyCount + " families total) — "
                        + "sizes 2–11, all roles & relationships, all report statuses";

        return new DevSeedDtos.SeedResponse(
                alreadySeeded,
                familyCount,
                membersAdded,
                reportsAdded,
                scenariosLoaded,
                message
        );
    }

    private boolean userAlreadyInFamilyByName(Account account, String displayName) {
        return membershipRepository.findActiveByPersonId(account.getPersonId()).stream()
                .anyMatch(m -> m.getFamily().getDisplayName().equals(displayName));
    }

    private SeedCounts seedScenario(Account account, FamilyScenario scenario) {
        Family family = resolveFamily(account, scenario);
        Map<String, Person> people = indexPeople(family);

        int membersAdded = 0;
        for (MemberSpec member : scenario.extraMembers()) {
            if (people.containsKey(normalizeName(member.name()))) {
                continue;
            }
            Person person = createPerson(member.name());
            membershipRepository.save(new FamilyMembership(
                    family,
                    person,
                    member.relationship(),
                    member.role()
            ));
            if (member.withAccount()) {
                createDemoAccount(person, member.name());
            }
            people.put(normalizeName(member.name()), person);
            membersAdded++;
        }

        addMembershipIfMissing(
                family,
                account.getPerson(),
                scenario.userMembership().relationship(),
                scenario.userMembership().role()
        );

        String uploaderId = scenario.kind() == ScenarioKind.OWNED
                ? account.getId()
                : family.getCreatedBy().getId();

        int reportsAdded = 0;
        for (ReportSpec report : scenario.reports()) {
            Person subject = resolveReportPerson(account, people, report.personName());
            reportsAdded += addReportIfMissing(family, subject, uploaderId, report);
        }

        return new SeedCounts(membersAdded, reportsAdded);
    }

    private Family resolveFamily(Account account, FamilyScenario scenario) {
        if (scenario.kind() == ScenarioKind.OWNED) {
            Family owned = familyRepository.findByCreatedById(account.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "No owned family found — register first"
                    ));
            owned.setDisplayName(scenario.displayName());
            return familyRepository.save(owned);
        }

        Person ownerPerson = createPerson(scenario.externalOwnerName());
        String ownerEmail = uniqueOwnerEmail(account, scenario.externalOwnerEmail());
        Account ownerAccount = createDemoAccount(ownerPerson, ownerEmail);

        Family family = new Family(scenario.displayName(), ownerAccount);
        family = familyRepository.save(family);

        membershipRepository.save(new FamilyMembership(
                family,
                ownerPerson,
                Relationship.self,
                FamilyRole.admin
        ));

        return family;
    }

    private String uniqueOwnerEmail(Account account, String baseEmail) {
        int at = baseEmail.indexOf('@');
        if (at < 0) {
            return baseEmail + "+" + account.getId().substring(0, 8);
        }
        return baseEmail.substring(0, at) + "+" + account.getId().substring(0, 8) + baseEmail.substring(at);
    }

    private Map<String, Person> indexPeople(Family family) {
        Map<String, Person> people = new HashMap<>();
        for (FamilyMembership membership : membershipRepository.findActiveByFamilyId(family.getId())) {
            people.put(normalizeName(membership.getPerson().getDisplayName()), membership.getPerson());
        }
        return people;
    }

    private Person resolveReportPerson(Account account, Map<String, Person> people, String personName) {
        if (personName == null || personName.isBlank()) {
            return account.getPerson();
        }
        Person person = people.get(normalizeName(personName));
        if (person == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Demo seed person not found: " + personName
            );
        }
        return person;
    }

    private Person createPerson(String displayName) {
        return personRepository.save(Person.withProfile(
                displayName,
                LocalDate.of(1990, 6, 1),
                "other",
                null,
                null,
                null,
                null,
                null,
                "PHI demo data"
        ));
    }

    private Account createDemoAccount(Person person, String emailOrName) {
        String email = emailOrName.contains("@")
                ? emailOrName
                : "phi-demo-" + slugify(emailOrName) + "@internal.local";

        if (accountRepository.existsByEmailIgnoreCase(email)) {
            return accountRepository.findByEmailIgnoreCaseWithPerson(email).orElseThrow();
        }

        Account demoAccount = new Account(
                person,
                email,
                passwordEncoder.encode(DEMO_PASSWORD),
                null
        );
        return accountRepository.save(demoAccount);
    }

    private void addMembershipIfMissing(
            Family family,
            Person person,
            Relationship relationship,
            FamilyRole role
    ) {
        if (membershipRepository.findActiveMembership(family.getId(), person.getId()).isEmpty()) {
            membershipRepository.save(new FamilyMembership(family, person, relationship, role));
        }
    }

    private int addReportIfMissing(
            Family family,
            Person person,
            String uploadedByAccountId,
            ReportSpec spec
    ) {
        boolean exists = labReportRepository.findActiveByFamilyId(family.getId()).stream()
                .anyMatch(r -> r.getOriginalFilename().equals(spec.filename()));
        if (exists) {
            return 0;
        }

        LabReport report = new LabReport(
                person,
                family.getId(),
                uploadedByAccountId,
                spec.filename(),
                "demo/" + family.getId() + "/" + spec.filename(),
                null
        );
        applyStatus(report, spec);
        if (spec.reportDate() != null) {
            report.setReportDate(LocalDate.parse(spec.reportDate()));
            report.setReportDateSource(ReportDateSource.USER);
        } else if (spec.status() == ExtractionStatus.COMPLETED || spec.status() == ExtractionStatus.TEXT_ONLY) {
            report.setReportDate(LocalDate.of(2026, 1, 15));
            report.setReportDateSource(ReportDateSource.USER);
        }
        report = labReportRepository.save(report);

        for (BiomarkerSpec biomarker : spec.biomarkers()) {
            biomarkerValueRepository.save(BiomarkerValue.fromExtraction(
                    report,
                    biomarker.canonical(),
                    biomarker.rawName(),
                    new BigDecimal(biomarker.value()),
                    null,
                    biomarker.unit(),
                    biomarker.referenceRange(),
                    BigDecimal.valueOf(0.95),
                    1
            ));
        }

        analyticsSyncService.syncReport(report.getId());
        return 1;
    }

    private void applyStatus(LabReport report, ReportSpec spec) {
        switch (spec.status()) {
            case PENDING -> { /* default */ }
            case PROCESSING -> report.markProcessing();
            case TEXT_EXTRACTED -> report.markTextExtracted("Demo extracted text for " + spec.filename());
            case COMPLETED -> report.markCompleted("Demo extracted text for " + spec.filename());
            case TEXT_ONLY -> report.markTextOnly("Demo text-only extraction for " + spec.filename());
            case FAILED -> report.markFailed(
                    spec.failureMessage() != null ? spec.failureMessage() : "Demo extraction failed"
            );
        }
    }

    private String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private String slugify(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private record SeedCounts(int membersAdded, int reportsAdded) {
    }
}
