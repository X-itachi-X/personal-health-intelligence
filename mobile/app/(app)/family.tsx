import { router } from "expo-router";
import { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Share,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import {
  addFamilyMember,
  createInvite,
  FamilyHealthSnapshot,
  fetchFamilySnapshot,
  FamilyMember,
  InviteResponse,
} from "../../lib/api";
import { getSession } from "../../lib/auth";
import { hasAdvancedAccess } from "../../lib/session";
import { useFamily } from "../../lib/FamilyContext";
import { colors, radii, spacing, typography } from "../../lib/theme";
import { AnimatedPressable } from "../../components/ui/AnimatedPressable";
import { BottomSheet } from "../../components/ui/BottomSheet";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { FadeInView } from "../../components/ui/FadeInView";
import { Icon } from "../../components/ui/Icon";
import { Screen } from "../../components/ui/Screen";
import { SkeletonCard } from "../../components/ui/Skeleton";

const RELATIONSHIPS = ["spouse", "parent", "child", "sibling", "other"] as const;
const ROLES = ["member", "maintainer"] as const;

export default function FamilyScreen() {
  const { familyDetail, activeFamily, refreshFamilies, isLoading, setViewedPerson, viewedPersonId } = useFamily();
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notAdvanced, setNotAdvanced] = useState(false);

  const [addOpen, setAddOpen] = useState(false);
  const [adding, setAdding] = useState(false);
  const [name, setName] = useState("");
  const [dob, setDob] = useState("1990-01-01");
  const [relationship, setRelationship] = useState<string>("child");
  const [role, setRole] = useState<string>("member");

  const [inviteOpen, setInviteOpen] = useState(false);
  const [invite, setInvite] = useState<InviteResponse | null>(null);
  const [invitingFor, setInvitingFor] = useState<string>("");
  const [inviting, setInviting] = useState(false);
  const [snapshot, setSnapshot] = useState<FamilyHealthSnapshot | null>(null);

  useEffect(() => {
    getSession().then((session) => {
      setNotAdvanced(!hasAdvancedAccess(session));
    });
  }, []);

  const load = useCallback(async () => {
    try {
      setError(null);
      const session = await getSession();
      if (!hasAdvancedAccess(session)) {
        setNotAdvanced(true);
        return;
      }
      setNotAdvanced(false);
      await refreshFamilies();
      if (activeFamily?.id) {
        fetchFamilySnapshot(activeFamily.id).then(setSnapshot).catch(() => setSnapshot(null));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load family");
    } finally {
      setRefreshing(false);
    }
  }, [refreshFamilies, activeFamily?.id]);

  async function handleAddMember() {
    if (!familyDetail || !name.trim()) {
      Alert.alert("Name required", "Enter the family member's name.");
      return;
    }
    setAdding(true);
    try {
      await addFamilyMember(familyDetail.id, {
        displayName: name.trim(),
        dateOfBirth: dob,
        sex: "other",
        relationship,
        role,
      });
      setAddOpen(false);
      setName("");
      setDob("1990-01-01");
      setRelationship("child");
      setRole("member");
      await refreshFamilies();
    } catch (err) {
      Alert.alert("Could not add member", err instanceof Error ? err.message : "Try again");
    } finally {
      setAdding(false);
    }
  }

  async function handleManageHealth(member: FamilyMember) {
    await setViewedPerson(member.personId, member.displayName);
    router.push("/(app)" as never);
  }

  async function handleInvite(member: FamilyMember) {
    if (!familyDetail) return;
    setInviting(true);
    setInvitingFor(member.displayName);
    try {
      const response = await createInvite(familyDetail.id, member.personId);
      setInvite(response);
      setInviteOpen(true);
    } catch (err) {
      Alert.alert("Invite failed", err instanceof Error ? err.message : "Try again");
    } finally {
      setInviting(false);
    }
  }

  async function shareInvite() {
    if (!invite) return;
    await Share.share({
      message: `Join my family on PHI!\n\nInvite code: ${invite.code}\n\nOpen the app → Create account → enter this code. Valid for 7 days.`,
    });
  }

  if (isLoading && !familyDetail) {
    return (
      <Screen>
        <SkeletonCard />
        <SkeletonCard />
      </Screen>
    );
  }

  if (notAdvanced) {
    return (
      <Screen scroll={false}>
        <EmptyState
          icon="people-outline"
          title="Advanced mode required"
          message="Switch to Advanced in Settings to add family members and send invites."
        />
      </Screen>
    );
  }

  const family = familyDetail;

  return (
    <>
      <Screen refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }}>
        {error && (
          <Card variant="outlined" style={styles.errorCard}>
            <Text style={styles.error}>{error}</Text>
          </Card>
        )}

        {family && (
          <>
            <FadeInView delay={0}>
              <View style={styles.header}>
                <Text style={styles.familyName}>{family.displayName}</Text>
                {activeFamily && activeFamily.id === family.id && (
                  <View style={styles.activeTag}>
                    <Icon name="checkmark-circle" size="sm" color={colors.primaryDark} />
                    <Text style={styles.activeTagText}>Currently active</Text>
                  </View>
                )}
                <Text style={styles.sectionTitle}>{family.members.length} members</Text>
              </View>
            </FadeInView>

            {snapshot && (
              <FadeInView delay={40}>
                <Card>
                  <View style={styles.snapshotHeader}>
                    <Icon name="pulse-outline" size="md" color={colors.primary} />
                    <Text style={styles.snapshotTitle}>Family health snapshot</Text>
                  </View>
                  <Text style={styles.snapshotMeta}>
                    {snapshot.outOfRangeMembers} of {snapshot.reportsWithData} members with recent labs have out-of-range results
                  </Text>
                  {snapshot.members
                    .filter((member) => member.outOfRangeCount > 0)
                    .map((member) => (
                      <Text key={member.personId} style={styles.snapshotRow}>
                        {member.personName}: {member.outOfRangeCount} flagged
                        {member.latestReportDate ? ` · ${member.latestReportDate}` : ""}
                      </Text>
                    ))}
                </Card>
              </FadeInView>
            )}

            <FadeInView delay={60}>
              <Button variant="primary" onPress={() => setAddOpen(true)}>
                Add family member
              </Button>
            </FadeInView>

            {family.members.map((member, index) => (
              <FadeInView key={member.personId} delay={120 + index * 50}>
                <Card>
                  <View style={styles.memberCard}>
                    <View style={styles.avatar}>
                      <Text style={styles.avatarText}>{member.displayName.charAt(0)}</Text>
                    </View>
                    <View style={styles.memberInfo}>
                      <Text style={styles.memberName}>{member.displayName}</Text>
                      <Text style={styles.memberMeta}>
                        {member.relationship} · {member.role}
                      </Text>
                      <View style={styles.accountRow}>
                        <Icon
                          name={member.hasAccount ? "checkmark-circle" : "person-add-outline"}
                          size="sm"
                          color={member.hasAccount ? colors.success : colors.textSoft}
                        />
                        <Text style={styles.accountStatus}>
                          {member.hasAccount
                            ? "Has their own account"
                            : "Dependent — no phone needed"}
                        </Text>
                      </View>
                    </View>
                  </View>
                  <View style={styles.memberActions}>
                    <AnimatedPressable
                      style={styles.manageBtn}
                      onPress={() => handleManageHealth(member)}
                    >
                      <Icon name="heart-outline" size="sm" color={colors.primaryDark} />
                      <Text style={styles.manageBtnText}>
                        {viewedPersonId === member.personId ? "Viewing" : "Manage health"}
                      </Text>
                    </AnimatedPressable>
                    {!member.hasAccount && (
                      <AnimatedPressable
                        style={styles.inviteBtn}
                        onPress={() => handleInvite(member)}
                        disabled={inviting}
                      >
                        <Icon name="mail-outline" size="sm" color={colors.primaryDark} />
                        <Text style={styles.inviteBtnText}>
                          {inviting && invitingFor === member.displayName ? "…" : "Invite later"}
                        </Text>
                      </AnimatedPressable>
                    )}
                  </View>
                </Card>
              </FadeInView>
            ))}

            <FadeInView delay={300}>
              <Card variant="muted">
                <View style={styles.helpHeader}>
                  <Icon name="help-circle-outline" size="md" color={colors.textMuted} />
                  <Text style={styles.helpTitle}>How invites work</Text>
                </View>
                <Text style={styles.helpText}>
                  Dependents without a phone: add their profile, tap Manage health, and upload reports for them — no account required.{'\n\n'}
                  If they later get a phone: tap Invite, share the code, and they link their own account.
                </Text>
              </Card>
            </FadeInView>
          </>
        )}
      </Screen>

      <BottomSheet visible={addOpen} onClose={() => setAddOpen(false)} title="Add family member">
        <Text style={styles.addHint}>
          For parents or elders without a smartphone, just add their profile. You can upload labs, prescriptions, and scans for them immediately.
        </Text>
        <TextInput
          style={styles.input}
          placeholder="Name (e.g. Mom)"
          placeholderTextColor={colors.textSoft}
          value={name}
          onChangeText={setName}
        />
        <TextInput
          style={styles.input}
          placeholder="Date of birth (YYYY-MM-DD)"
          placeholderTextColor={colors.textSoft}
          value={dob}
          onChangeText={setDob}
        />

        <Text style={styles.fieldLabel}>Relationship</Text>
        <View style={styles.chipRow}>
          {RELATIONSHIPS.map((r) => (
            <AnimatedPressable
              key={r}
              style={[styles.chip, relationship === r && styles.chipActive]}
              onPress={() => setRelationship(r)}
            >
              <Text style={[styles.chipText, relationship === r && styles.chipTextActive]}>{r}</Text>
            </AnimatedPressable>
          ))}
        </View>

        <Text style={styles.fieldLabel}>Role</Text>
        <View style={styles.chipRow}>
          {ROLES.map((r) => (
            <AnimatedPressable
              key={r}
              style={[styles.chip, role === r && styles.chipActive]}
              onPress={() => setRole(r)}
            >
              <Text style={[styles.chipText, role === r && styles.chipTextActive]}>{r}</Text>
            </AnimatedPressable>
          ))}
        </View>

        <Button variant="secondary" onPress={handleAddMember} loading={adding} style={styles.sheetBtn}>
          Add member
        </Button>
      </BottomSheet>

      <BottomSheet visible={inviteOpen} onClose={() => setInviteOpen(false)}>
        <View style={styles.inviteContent}>
          <View style={styles.inviteIconWrap}>
            <Icon name="key-outline" size="xl" color={colors.primary} />
          </View>
          <Text style={styles.inviteTitle}>Invite ready for {invitingFor}</Text>
          <Text style={styles.inviteCode}>{invite?.code}</Text>
          <Text style={styles.inviteHint}>
            Share this code. They enter it when creating an account. Expires in 7 days.
          </Text>
          <Button variant="primary" onPress={shareInvite}>Share code</Button>
          <Button variant="ghost" onPress={() => setInviteOpen(false)}>Done</Button>
        </View>
      </BottomSheet>
    </>
  );
}

const styles = StyleSheet.create({
  errorCard: { borderColor: colors.dangerBorder },
  error: { color: colors.danger },
  header: { gap: spacing.xs },
  familyName: { ...typography.hero, fontSize: 24 },
  activeTag: { flexDirection: "row", alignItems: "center", gap: 6 },
  activeTagText: { fontSize: 13, fontWeight: "600", color: colors.primaryDark },
  sectionTitle: { ...typography.caption },
  snapshotHeader: { flexDirection: "row", alignItems: "center", gap: spacing.sm, marginBottom: 4 },
  snapshotTitle: { ...typography.subtitle, fontSize: 16 },
  snapshotMeta: { ...typography.caption, lineHeight: 20 },
  snapshotRow: { ...typography.body, marginTop: 6 },
  memberCard: { flexDirection: "row", alignItems: "center", gap: spacing.md, marginBottom: spacing.sm },
  memberActions: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm },
  manageBtn: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    backgroundColor: colors.primaryLight,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: radii.sm,
  },
  manageBtnText: { color: colors.primaryDark, fontWeight: "700", fontSize: 13 },
  addHint: { ...typography.caption, lineHeight: 20, marginBottom: spacing.sm },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: colors.primaryLight,
    alignItems: "center",
    justifyContent: "center",
  },
  avatarText: { fontSize: 18, fontWeight: "700", color: colors.primaryDark },
  memberInfo: { flex: 1 },
  memberName: { ...typography.subtitle, fontSize: 16 },
  memberMeta: { ...typography.caption, marginTop: 2, textTransform: "capitalize" },
  accountRow: { flexDirection: "row", alignItems: "center", gap: 4, marginTop: 4 },
  accountStatus: { fontSize: 12, color: colors.textSoft },
  inviteBtn: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    backgroundColor: colors.primaryLight,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: radii.sm,
  },
  inviteBtnText: { color: colors.primaryDark, fontWeight: "700", fontSize: 13 },
  helpHeader: { flexDirection: "row", alignItems: "center", gap: spacing.sm, marginBottom: 6 },
  helpTitle: { ...typography.subtitle, fontSize: 14 },
  helpText: { ...typography.caption, lineHeight: 22 },
  input: {
    backgroundColor: colors.bg,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radii.md,
    padding: 14,
    fontSize: 16,
    color: colors.text,
  },
  fieldLabel: { ...typography.label, marginTop: spacing.sm },
  chipRow: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: radii.sm,
    backgroundColor: colors.surfaceMuted,
    borderWidth: 1,
    borderColor: colors.border,
  },
  chipActive: { backgroundColor: colors.primaryLight, borderColor: colors.primary },
  chipText: { fontSize: 14, color: colors.textMuted, textTransform: "capitalize" },
  chipTextActive: { color: colors.primaryDark, fontWeight: "700" },
  sheetBtn: { marginTop: spacing.md },
  inviteContent: { alignItems: "center", gap: spacing.sm },
  inviteIconWrap: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: colors.primaryLight,
    alignItems: "center",
    justifyContent: "center",
    marginBottom: spacing.sm,
  },
  inviteTitle: { ...typography.subtitle, textAlign: "center" },
  inviteCode: { ...typography.mono, color: colors.primaryDark, marginVertical: spacing.md },
  inviteHint: { ...typography.body, textAlign: "center", marginBottom: spacing.sm },
});
