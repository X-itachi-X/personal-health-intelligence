package com.phi.dev;

import com.phi.domain.ExtractionStatus;
import com.phi.domain.FamilyRole;
import com.phi.domain.Relationship;
import java.util.List;

/**
 * Declarative demo scenarios covering family-size archetypes, role/relationship
 * permutations, profile-only vs account-holding members, and report statuses.
 */
public final class DevSeedCatalog {

    private DevSeedCatalog() {
    }

    public record UserMembershipSpec(Relationship relationship, FamilyRole role) {
    }

    public record MemberSpec(
            String name,
            Relationship relationship,
            FamilyRole role,
            boolean withAccount
    ) {
    }

    public record BiomarkerSpec(
            String canonical,
            String rawName,
            String value,
            String unit,
            String referenceRange
    ) {
    }

    public record ReportSpec(
            String personName,
            String filename,
            ExtractionStatus status,
            List<BiomarkerSpec> biomarkers,
            String failureMessage,
            String reportDate
    ) {
    }

    public record FamilyScenario(
            String displayName,
            ScenarioKind kind,
            String externalOwnerName,
            String externalOwnerEmail,
            UserMembershipSpec userMembership,
            List<MemberSpec> extraMembers,
            List<ReportSpec> reports
    ) {
    }

    public enum ScenarioKind {
        /** User's own family (admin / self). */
        OWNED,
        /** Family owned by another demo account; user is added as a member. */
        EXTERNAL
    }

    /**
     * 12 scenarios — covers:
     * <ul>
     *   <li>Sizes: 1 (owned baseline), 2, 3, 4, 7, 9, 11 members</li>
     *   <li>User roles: admin, maintainer, member (all three)</li>
     *   <li>User relationships: self, spouse, parent, child, sibling, other (all six)</li>
     *   <li>Members with and without accounts</li>
     *   <li>Report statuses: PENDING, PROCESSING, COMPLETED, TEXT_ONLY, FAILED</li>
     * </ul>
     */
    public static List<FamilyScenario> all() {
        return List.of(
                ownedNuclearFamily(),
                coupleOnly(),
                patelParents(),
                chenGrandparents(),
                adultChildren(),
                siblingsFlat(),
                largeCareTeam(),
                relationshipMatrix(),
                inviteShowcase(),
                reportStatusLab(),
                mixedAccountsClub(),
                emptyFeedFamily()
        );
    }

    private static FamilyScenario ownedNuclearFamily() {
        return new FamilyScenario(
                "Home Family",
                ScenarioKind.OWNED,
                null,
                null,
                new UserMembershipSpec(Relationship.self, FamilyRole.admin),
                List.of(
                        member("Priya", Relationship.spouse, FamilyRole.maintainer, false),
                        member("Aarav", Relationship.child, FamilyRole.member, false),
                        member("Meera", Relationship.child, FamilyRole.member, false)
                ),
                List.of(
                        report("Priya", "priya-thyroid-check.pdf", ExtractionStatus.COMPLETED, thyroidPanel(), null),
                        report(null, "my-lipid-panel-2025.pdf", ExtractionStatus.COMPLETED, lipidPanel(), null),
                        report("Aarav", "aarav-growth-panel.pdf", ExtractionStatus.COMPLETED, cbcPanel(), null)
                )
        );
    }

    /** Size 2 — user is spouse (member). */
    private static FamilyScenario coupleOnly() {
        return new FamilyScenario(
                "Couple Only",
                ScenarioKind.EXTERNAL,
                "Arun Mehta",
                "phi-demo-arun-mehta@internal.local",
                new UserMembershipSpec(Relationship.spouse, FamilyRole.member),
                List.of(),
                List.of(
                        report("Arun Mehta", "arun-annual-physical.pdf", ExtractionStatus.COMPLETED, metabolicPanel(), null)
                )
        );
    }

    /** Size 3 — user is child (member). */
    private static FamilyScenario patelParents() {
        return new FamilyScenario(
                "Patel Parents",
                ScenarioKind.EXTERNAL,
                "Raj Patel",
                "phi-demo-raj-patel@internal.local",
                new UserMembershipSpec(Relationship.child, FamilyRole.member),
                List.of(member("Sunita Patel", Relationship.parent, FamilyRole.maintainer, false)),
                List.of(
                        report("Raj Patel", "raj-patel-cbc.pdf", ExtractionStatus.COMPLETED, cbcPanel(), null),
                        report("Sunita Patel", "sunita-vitamin-d.pdf", ExtractionStatus.COMPLETED, vitaminDPanel(), null)
                )
        );
    }

    /** Size 3 — user is other (member). */
    private static FamilyScenario chenGrandparents() {
        return new FamilyScenario(
                "Chen Grandparents",
                ScenarioKind.EXTERNAL,
                "Mei Chen",
                "phi-demo-mei-chen@internal.local",
                new UserMembershipSpec(Relationship.other, FamilyRole.member),
                List.of(member("Wei Chen", Relationship.parent, FamilyRole.maintainer, false)),
                List.of(
                        report("Mei Chen", "mei-chen-metabolic.pdf", ExtractionStatus.COMPLETED, metabolicPanel(), null),
                        report("Wei Chen", "wei-chen-lipid.pdf", ExtractionStatus.COMPLETED, lipidPanel(), null)
                )
        );
    }

    /** Size 4 — user is parent (member) in adult children's household. */
    private static FamilyScenario adultChildren() {
        return new FamilyScenario(
                "My Adult Children",
                ScenarioKind.EXTERNAL,
                "Ananya Rao",
                "phi-demo-ananya-rao@internal.local",
                new UserMembershipSpec(Relationship.parent, FamilyRole.member),
                List.of(
                        member("Vikram Rao", Relationship.child, FamilyRole.maintainer, true),
                        member("Isha Rao", Relationship.child, FamilyRole.member, false)
                ),
                List.of(
                        report("Ananya Rao", "ananya-wellness.pdf", ExtractionStatus.COMPLETED, thyroidPanel(), null),
                        report("Vikram Rao", "vikram-lipid.pdf", ExtractionStatus.COMPLETED, lipidPanel(), null)
                )
        );
    }

    /** Size 4 — user is sibling (member). */
    private static FamilyScenario siblingsFlat() {
        return new FamilyScenario(
                "Siblings Flat",
                ScenarioKind.EXTERNAL,
                "Rohan Desai",
                "phi-demo-rohan-desai@internal.local",
                new UserMembershipSpec(Relationship.sibling, FamilyRole.member),
                List.of(
                        member("Neha Desai", Relationship.sibling, FamilyRole.maintainer, false)
                ),
                List.of(
                        report("Rohan Desai", "rohan-cbc.pdf", ExtractionStatus.COMPLETED, cbcPanel(), null),
                        report("Neha Desai", "neha-iron-studies.pdf", ExtractionStatus.COMPLETED, ironPanel(), null)
                )
        );
    }

    /** Size 11 — user is maintainer managing many dependents. */
    private static FamilyScenario largeCareTeam() {
        return new FamilyScenario(
                "Large Care Team (11)",
                ScenarioKind.EXTERNAL,
                "Dr. Kavitha Nair",
                "phi-demo-kavitha-nair@internal.local",
                new UserMembershipSpec(Relationship.self, FamilyRole.maintainer),
                List.of(
                        member("Mom", Relationship.parent, FamilyRole.member, false),
                        member("Dad", Relationship.parent, FamilyRole.member, false),
                        member("Anand", Relationship.child, FamilyRole.member, false),
                        member("Kiran", Relationship.child, FamilyRole.member, false),
                        member("Riya", Relationship.child, FamilyRole.member, false),
                        member("Dev", Relationship.child, FamilyRole.member, false),
                        member("Aunt Lakshmi", Relationship.other, FamilyRole.member, false),
                        member("Uncle Ravi", Relationship.other, FamilyRole.member, false),
                        member("Grandma", Relationship.other, FamilyRole.member, false)
                ),
                List.of(
                        report("Mom", "mom-diabetes-panel.pdf", ExtractionStatus.COMPLETED, metabolicPanel(), null),
                        report("Dad", "dad-cardiac-markers.pdf", ExtractionStatus.COMPLETED, lipidPanel(), null),
                        report("Anand", "anand-sports-panel.pdf", ExtractionStatus.COMPLETED, cbcPanel(), null),
                        report("Grandma", "grandma-kidney-panel.pdf", ExtractionStatus.TEXT_ONLY, List.of(), null)
                )
        );
    }

    /** Size 9 — every relationship enum value represented among members. */
    private static FamilyScenario relationshipMatrix() {
        return new FamilyScenario(
                "Relationship Matrix",
                ScenarioKind.EXTERNAL,
                "Household Anchor",
                "phi-demo-household-anchor@internal.local",
                new UserMembershipSpec(Relationship.self, FamilyRole.maintainer),
                List.of(
                        member("Partner", Relationship.spouse, FamilyRole.member, false),
                        member("Father", Relationship.parent, FamilyRole.member, false),
                        member("Mother", Relationship.parent, FamilyRole.member, false),
                        member("Son", Relationship.child, FamilyRole.member, false),
                        member("Daughter", Relationship.child, FamilyRole.member, false),
                        member("Brother", Relationship.sibling, FamilyRole.member, false),
                        member("Neighbor", Relationship.other, FamilyRole.member, false)
                ),
                List.of(
                        report("Father", "father-psa-screen.pdf", ExtractionStatus.COMPLETED, cbcPanel(), null),
                        report("Mother", "mother-b12.pdf", ExtractionStatus.COMPLETED, vitaminDPanel(), null)
                )
        );
    }

    /** Size 6 — all profile-only members (invite UI). */
    private static FamilyScenario inviteShowcase() {
        return new FamilyScenario(
                "Invite Showcase",
                ScenarioKind.EXTERNAL,
                "Invite Host",
                "phi-demo-invite-host@internal.local",
                new UserMembershipSpec(Relationship.other, FamilyRole.maintainer),
                List.of(
                        member("Grandma Rose", Relationship.other, FamilyRole.member, false),
                        member("Uncle Joe", Relationship.other, FamilyRole.member, false),
                        member("Cousin Mia", Relationship.other, FamilyRole.member, false),
                        member("Aunt Sue", Relationship.other, FamilyRole.member, false)
                ),
                List.of()
        );
    }

    /** Size 5 — one report per extraction status. */
    private static FamilyScenario reportStatusLab() {
        return new FamilyScenario(
                "Report Status Lab",
                ScenarioKind.EXTERNAL,
                "Lab QA Owner",
                "phi-demo-lab-qa@internal.local",
                new UserMembershipSpec(Relationship.self, FamilyRole.admin),
                List.of(
                        member("QA Subject A", Relationship.other, FamilyRole.member, false),
                        member("QA Subject B", Relationship.other, FamilyRole.member, false),
                        member("QA Subject C", Relationship.other, FamilyRole.member, false),
                        member("QA Subject D", Relationship.other, FamilyRole.member, false)
                ),
                List.of(
                        report(null, "status-pending.pdf", ExtractionStatus.PENDING, List.of(), null),
                        report("QA Subject A", "status-processing.pdf", ExtractionStatus.PROCESSING, List.of(), null),
                        report("QA Subject B", "status-completed.pdf", ExtractionStatus.COMPLETED, cbcPanel(), null),
                        report("QA Subject C", "status-text-only.pdf", ExtractionStatus.TEXT_ONLY, List.of(), null),
                        report("QA Subject D", "status-failed.pdf", ExtractionStatus.FAILED, List.of(), "Demo: unreadable scan")
                )
        );
    }

    /** Size 5 — mix of account holders and profile-only members. */
    private static FamilyScenario mixedAccountsClub() {
        return new FamilyScenario(
                "Mixed Accounts Club",
                ScenarioKind.EXTERNAL,
                "Club Organizer",
                "phi-demo-club-organizer@internal.local",
                new UserMembershipSpec(Relationship.sibling, FamilyRole.member),
                List.of(
                        member("Active Member Sam", Relationship.sibling, FamilyRole.maintainer, true),
                        member("Active Member Kim", Relationship.other, FamilyRole.member, true),
                        member("Profile Only Pat", Relationship.other, FamilyRole.member, false),
                        member("Profile Only Lin", Relationship.other, FamilyRole.member, false)
                ),
                List.of(
                        report("Active Member Sam", "sam-liver-panel.pdf", ExtractionStatus.COMPLETED, metabolicPanel(), null),
                        report("Profile Only Pat", "pat-allergy-panel.pdf", ExtractionStatus.COMPLETED, cbcPanel(), null)
                )
        );
    }

    /** Size 2 — no reports (empty feed / reports list). */
    private static FamilyScenario emptyFeedFamily() {
        return new FamilyScenario(
                "Empty Feed Family",
                ScenarioKind.EXTERNAL,
                "New Household",
                "phi-demo-new-household@internal.local",
                new UserMembershipSpec(Relationship.spouse, FamilyRole.maintainer),
                List.of(member("New Partner", Relationship.spouse, FamilyRole.member, false)),
                List.of()
        );
    }

    private static MemberSpec member(
            String name,
            Relationship relationship,
            FamilyRole role,
            boolean withAccount
    ) {
        return new MemberSpec(name, relationship, role, withAccount);
    }

    private static ReportSpec report(
            String personName,
            String filename,
            ExtractionStatus status,
            List<BiomarkerSpec> biomarkers,
            String failureMessage
    ) {
        return new ReportSpec(personName, filename, status, biomarkers, failureMessage, null);
    }

    private static ReportSpec report(
            String personName,
            String filename,
            ExtractionStatus status,
            List<BiomarkerSpec> biomarkers,
            String failureMessage,
            String reportDate
    ) {
        return new ReportSpec(personName, filename, status, biomarkers, failureMessage, reportDate);
    }

    private static List<BiomarkerSpec> lipidPanel() {
        return List.of(
                biomarker("ldl_cholesterol", "LDL Cholesterol", "118", "mg/dL", "0-100"),
                biomarker("hdl_cholesterol", "HDL Cholesterol", "52", "mg/dL", "40-60"),
                biomarker("triglycerides", "Triglycerides", "145", "mg/dL", "0-150")
        );
    }

    private static List<BiomarkerSpec> thyroidPanel() {
        return List.of(
                biomarker("tsh", "TSH", "2.1", "mIU/L", "0.4-4.0"),
                biomarker("free_t4", "Free T4", "1.2", "ng/dL", "0.8-1.8")
        );
    }

    private static List<BiomarkerSpec> cbcPanel() {
        return List.of(
                biomarker("hemoglobin", "Hemoglobin", "14.2", "g/dL", "13.5-17.5"),
                biomarker("wbc", "WBC", "6.8", "10^3/uL", "4.5-11.0"),
                biomarker("platelets", "Platelets", "245", "10^3/uL", "150-400")
        );
    }

    private static List<BiomarkerSpec> metabolicPanel() {
        return List.of(
                biomarker("glucose_fasting", "Fasting Glucose", "98", "mg/dL", "70-99"),
                biomarker("hba1c", "HbA1c", "5.6", "%", "4.0-5.6"),
                biomarker("creatinine", "Creatinine", "0.9", "mg/dL", "0.6-1.2")
        );
    }

    private static List<BiomarkerSpec> vitaminDPanel() {
        return List.of(
                biomarker("vitamin_d", "Vitamin D", "22", "ng/mL", "30-100"),
                biomarker("calcium", "Calcium", "9.4", "mg/dL", "8.6-10.2")
        );
    }

    private static List<BiomarkerSpec> ironPanel() {
        return List.of(
                biomarker("ferritin", "Ferritin", "45", "ng/mL", "12-300"),
                biomarker("iron", "Serum Iron", "88", "ug/dL", "60-170")
        );
    }

    private static BiomarkerSpec biomarker(
            String canonical,
            String rawName,
            String value,
            String unit,
            String referenceRange
    ) {
        return new BiomarkerSpec(canonical, rawName, value, unit, referenceRange);
    }
}
