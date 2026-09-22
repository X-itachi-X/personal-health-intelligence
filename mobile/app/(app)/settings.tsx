import { router } from "expo-router";
import type { Href } from "expo-router";
import { useEffect, useState } from "react";
import { Alert, Linking, Platform, Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import {
  createMedication,
  deleteMedication,
  endMedication,
  fetchMe,
  listMedications,
  Medication,
  MedicationCourseType,
  seedDemoData,
  updateUiMode,
} from "../../lib/api";
import { checkForApkUpdate } from "../../lib/apkUpdate";
import { useConfirmFormStyles } from "../../components/report/confirmFormStyles";
import { getSession, saveSession, AuthSession } from "../../lib/auth";
import { hasAdvancedAccess, isPlatformAdmin } from "../../lib/session";
import { useFamily } from "../../lib/FamilyContext";
import { useTheme } from "../../lib/ThemeContext";
import { useThemedStyles } from "../../lib/useThemedStyles";
import { radii, spacing } from "../../lib/theme";
import { AnimatedPressable } from "../../components/ui/AnimatedPressable";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { FadeInView } from "../../components/ui/FadeInView";
import { Icon } from "../../components/ui/Icon";
import { Screen } from "../../components/ui/Screen";
import { Skeleton } from "../../components/ui/Skeleton";
import { ThemeModePicker } from "../../components/ui/ThemeModePicker";
import { ViewingPersonBanner } from "../../components/ViewingPersonBanner";

export default function SettingsScreen() {
  const { colors, typography } = useTheme();
  const confirmFormStyles = useConfirmFormStyles();
  const styles = useThemedStyles(({ colors, typography }) =>
    StyleSheet.create({
      skeletonProfile: { alignItems: "center", paddingVertical: spacing.lg },
      profile: { alignItems: "center", marginBottom: spacing.sm },
      avatar: {
        width: 80,
        height: 80,
        borderRadius: 40,
        backgroundColor: colors.primary,
        alignItems: "center",
        justifyContent: "center",
        marginTop: spacing.md,
      },
      avatarText: { fontSize: 32, fontWeight: "700", color: colors.white },
      name: { ...typography.hero, fontSize: 24, textAlign: "center", marginTop: spacing.md },
      email: { ...typography.body, textAlign: "center" },
      cardHeader: { flexDirection: "row", alignItems: "center", gap: spacing.sm, marginBottom: 4 },
      cardTitle: { ...typography.label, textTransform: "none", fontSize: 13, color: colors.textSoft },
      value: { ...typography.subtitle, fontSize: 17 },
      hint: { ...typography.caption, lineHeight: 20, marginTop: 4 },
      familyList: { gap: spacing.sm, marginTop: spacing.sm },
      emptyFamilies: { ...typography.caption, marginTop: spacing.sm },
      familyOption: {
        flexDirection: "row",
        alignItems: "center",
        gap: spacing.md,
        padding: spacing.md,
        borderRadius: radii.md,
        backgroundColor: colors.surfaceMuted,
        borderWidth: 2,
        borderColor: "transparent",
      },
      familyOptionActive: {
        borderColor: colors.primary,
        backgroundColor: colors.primaryLight,
      },
      familyIcon: {
        width: 44,
        height: 44,
        borderRadius: 22,
        backgroundColor: colors.surface,
        alignItems: "center",
        justifyContent: "center",
      },
      familyIconText: { fontSize: 18, fontWeight: "800", color: colors.primaryDark },
      familyInfo: { flex: 1 },
      familyName: { ...typography.subtitle, fontSize: 16 },
      familyMeta: { ...typography.caption, marginTop: 2 },
      activeBadge: {
        width: 32,
        height: 32,
        borderRadius: 16,
        backgroundColor: colors.primaryDark,
        alignItems: "center",
        justifyContent: "center",
      },
      actionBtn: { marginTop: spacing.md },
      seedRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm, marginTop: spacing.sm },
      seedMessage: { ...typography.caption, color: colors.success, flex: 1 },
      modeValueRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
      modeBadge: {
        backgroundColor: colors.primaryLight,
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: radii.sm,
      },
      modeBadgeAdvanced: { backgroundColor: colors.adminBadge },
      modeBadgeText: { fontSize: 11, fontWeight: "700", color: colors.primaryDark },
      medList: { gap: spacing.sm, marginTop: spacing.sm, marginBottom: spacing.sm },
      medPastTitle: { ...typography.caption, color: colors.textMuted, marginBottom: spacing.xs },
      medRow: {
        padding: spacing.sm,
        borderRadius: radii.sm,
        backgroundColor: colors.surfaceMuted,
      },
      medRowMuted: {
        padding: spacing.sm,
        borderRadius: radii.sm,
        backgroundColor: colors.surface,
        borderWidth: 1,
        borderColor: colors.border,
      },
      medActions: { flexDirection: "row", gap: spacing.md, marginTop: spacing.sm },
      medActionBtn: { paddingVertical: 2 },
      medActionEnd: { fontSize: 13, color: colors.primary, fontWeight: "600" },
      medActionDelete: { fontSize: 13, color: colors.danger, fontWeight: "600" },
      medName: { ...typography.subtitle, fontSize: 15 },
      medMeta: { ...typography.caption, marginTop: 2 },
      medSchedule: { ...typography.caption, marginTop: 4, color: colors.textSoft },
      medFieldLabel: { ...typography.caption, color: colors.textMuted, marginTop: spacing.sm },
      input: {
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radii.sm,
        paddingHorizontal: spacing.md,
        paddingVertical: 10,
        fontSize: 15,
        color: colors.text,
        backgroundColor: colors.surfaceInset,
        marginTop: spacing.sm,
      },
    })
  );

  const [session, setSession] = useState<AuthSession | null>(null);
  const [saving, setSaving] = useState(false);
  const [seeding, setSeeding] = useState(false);
  const [seedMessage, setSeedMessage] = useState<string | null>(null);
  const [medications, setMedications] = useState<Medication[]>([]);
  const [medName, setMedName] = useState("");
  const [medStartedOn, setMedStartedOn] = useState("");
  const [medSchedule, setMedSchedule] = useState("");
  const [medCourseType, setMedCourseType] = useState<MedicationCourseType>("CHRONIC");
  const [medSaving, setMedSaving] = useState(false);
  const [medActionId, setMedActionId] = useState<string | null>(null);
  const {
    families,
    activeFamily,
    isSwitching,
    switchFamily,
    isLoading,
    refreshFamilies,
    effectivePersonId,
    isViewingOther,
    viewedPersonName,
    viewedMember,
    clearViewedPerson,
    viewedPersonId,
  } = useFamily();

  useEffect(() => {
    getSession().then(setSession);
    fetchMe().then(setSession).catch(() => undefined);
  }, []);

  const activePersonId = session?.personId ? effectivePersonId(session.personId) : null;
  const viewingOther = session?.personId ? isViewingOther(session.personId) : false;

  useEffect(() => {
    if (!activePersonId) {
      setMedications([]);
      return;
    }
    listMedications(activePersonId)
      .then(setMedications)
      .catch(() => setMedications([]));
  }, [activePersonId]);

  async function toggleMode() {
    if (!session || saving) return;
    const next = session.uiMode === "advanced" ? "basic" : "advanced";
    setSaving(true);
    try {
      const updated = await updateUiMode(next);
      await saveSession(updated);
      setSession(updated);
    } catch {
      // keep current mode on error
    } finally {
      setSaving(false);
    }
  }

  async function handleFamilySelect(familyId: string) {
    if (activeFamily?.id === familyId || isSwitching) return;
    await switchFamily(familyId);
    router.replace("/(app)" as never);
  }

  function confirmEndMedication(med: Medication) {
    Alert.alert(
      "Mark medication ended?",
      `${med.medicationName} will move to past medications with today's end date.`,
      [
        { text: "Cancel", style: "cancel" },
        {
          text: "Mark ended",
          onPress: () => handleEndMedication(med),
        },
      ]
    );
  }

  function confirmDeleteMedication(med: Medication) {
    Alert.alert(
      "Delete medication?",
      `Remove ${med.medicationName} from your log? This cannot be undone.`,
      [
        { text: "Cancel", style: "cancel" },
        {
          text: "Delete",
          style: "destructive",
          onPress: () => handleDeleteMedication(med),
        },
      ]
    );
  }

  async function handleEndMedication(med: Medication) {
    if (!activePersonId || medActionId) return;
    setMedActionId(med.id);
    try {
      const updated = await endMedication(activePersonId, med.id);
      setMedications((prev) => prev.map((item) => (item.id === med.id ? updated : item)));
    } catch (err) {
      Alert.alert("Could not update", err instanceof Error ? err.message : "Try again");
    } finally {
      setMedActionId(null);
    }
  }

  async function handleDeleteMedication(med: Medication) {
    if (!activePersonId || medActionId) return;
    setMedActionId(med.id);
    try {
      await deleteMedication(activePersonId, med.id);
      setMedications((prev) => prev.filter((item) => item.id !== med.id));
    } catch (err) {
      Alert.alert("Could not delete", err instanceof Error ? err.message : "Try again");
    } finally {
      setMedActionId(null);
    }
  }

  async function handleAddMedication() {
    if (!activePersonId || medSaving) return;
    const name = medName.trim();
    const started = medStartedOn.trim();
    if (!name || !started) {
      Alert.alert("Missing fields", "Enter medication name and start date (YYYY-MM-DD).");
      return;
    }
    setMedSaving(true);
    try {
      const created = await createMedication(activePersonId, {
        medicationName: name,
        startedOn: started,
        scheduleText: medSchedule.trim() || undefined,
        courseType: medCourseType,
      });
      setMedications((prev) => [created, ...prev]);
      setMedName("");
      setMedStartedOn("");
      setMedSchedule("");
      setMedCourseType("CHRONIC");
    } catch (err) {
      Alert.alert("Could not save", err instanceof Error ? err.message : "Try again");
    } finally {
      setMedSaving(false);
    }
  }

  async function handleLoadDemoData() {
    if (seeding) return;
    setSeeding(true);
    setSeedMessage(null);
    try {
      const result = await seedDemoData();
      setSeedMessage(result.message);
      await refreshFamilies();
      const me = await fetchMe();
      await saveSession(me);
      setSession(me);
      if (!result.alreadySeeded) {
        Alert.alert(
          "Demo data ready",
          `${result.scenariosLoaded} scenarios · ${result.families} families · ${result.reportsAdded} reports.\n\nSwitch families below to explore each variation.`
        );
      }
    } catch (err) {
      Alert.alert("Could not load demo data", err instanceof Error ? err.message : "Try again");
    } finally {
      setSeeding(false);
    }
  }

  if (!session) {
    return (
      <Screen>
        <View style={styles.skeletonProfile}>
          <Skeleton width={80} height={80} borderRadius={40} />
          <Skeleton width="50%" height={24} style={{ marginTop: 16 }} />
          <Skeleton width="70%" height={16} style={{ marginTop: 8 }} />
        </View>
      </Screen>
    );
  }

  const isAdvanced = hasAdvancedAccess(session);
  const uiModeAdvanced = session.uiMode === "advanced";

  return (
    <Screen>
      {viewingOther && viewedPersonName && (
        <FadeInView delay={0}>
          <ViewingPersonBanner
            name={viewedPersonName}
            profileOnly={viewedMember != null && !viewedMember.hasAccount}
            onClear={() => clearViewedPerson()}
          />
        </FadeInView>
      )}

      <FadeInView delay={viewingOther ? 40 : 0}>
        <View style={styles.profile}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>{session.displayName.charAt(0).toUpperCase()}</Text>
          </View>
          <Text style={styles.name}>{session.displayName}</Text>
          <Text style={styles.email}>{session.email}</Text>
        </View>
      </FadeInView>

      <FadeInView delay={60}>
        <Card>
          <View style={styles.cardHeader}>
            <Icon name="color-palette-outline" size="md" color={colors.primary} />
            <Text style={styles.cardTitle}>Appearance</Text>
          </View>
          <Text style={styles.hint}>Matte palette with light, dark, or match your device setting.</Text>
          <ThemeModePicker />
        </Card>
      </FadeInView>

      <FadeInView delay={90}>
        <Card>
          <View style={styles.cardHeader}>
            <Icon name="people" size="md" color={colors.primary} />
            <Text style={styles.cardTitle}>Active family</Text>
          </View>
          <Text style={styles.hint}>
            Switch families to see a completely different home, reports, and roster.
          </Text>

          {isLoading && families.length === 0 ? (
            <Skeleton height={60} style={{ marginTop: spacing.sm }} />
          ) : families.length === 0 ? (
            <Text style={styles.emptyFamilies}>No families yet.</Text>
          ) : (
            <View style={styles.familyList}>
              {families.map((family) => {
                const active = family.id === activeFamily?.id;
                return (
                  <AnimatedPressable
                    key={family.id}
                    style={[styles.familyOption, active && styles.familyOptionActive]}
                    onPress={() => handleFamilySelect(family.id)}
                    disabled={isSwitching}
                  >
                    <View style={styles.familyIcon}>
                      <Text style={styles.familyIconText}>{family.displayName.charAt(0)}</Text>
                    </View>
                    <View style={styles.familyInfo}>
                      <Text style={styles.familyName}>{family.displayName}</Text>
                      <Text style={styles.familyMeta}>
                        {family.memberCount} members · {family.myRelationship}
                      </Text>
                    </View>
                    {active ? (
                      <View style={styles.activeBadge}>
                        <Icon name="checkmark" size="sm" color={colors.white} />
                      </View>
                    ) : (
                      <Icon name="swap-horizontal" size="md" color={colors.primaryDark} />
                    )}
                  </AnimatedPressable>
                );
              })}
            </View>
          )}
        </Card>
      </FadeInView>

      <FadeInView delay={120}>
        <Card>
          <View style={styles.cardHeader}>
            <Icon name="flask-outline" size="md" color={colors.primary} />
            <Text style={styles.cardTitle}>Demo data</Text>
          </View>
          <Text style={styles.hint}>
            Load 12 demo scenarios: solo to 11-member households, every role & relationship, mixed accounts, and all report statuses.
          </Text>
          <Button variant="primary" onPress={handleLoadDemoData} loading={seeding} style={styles.actionBtn}>
            Load demo families & reports
          </Button>
          {seedMessage && (
            <View style={styles.seedRow}>
              <Icon name="checkmark-circle" size="sm" color={colors.success} />
              <Text style={styles.seedMessage}>{seedMessage}</Text>
            </View>
          )}
        </Card>
      </FadeInView>

      {session.platformAdmin && (
        <FadeInView delay={180}>
          <Card>
            <View style={styles.cardHeader}>
              <Icon name="stats-chart-outline" size="md" color={colors.primary} />
              <Text style={styles.cardTitle}>Platform ops console</Text>
            </View>
            <Text style={styles.hint}>
              App-wide monitoring for all testers — uploads, pipeline traces, AI usage, and failures.
            </Text>
            <Button
              variant="secondary"
              onPress={() => router.push("/(app)/ops" as Href)}
              style={styles.actionBtn}
            >
              Open platform console
            </Button>
          </Card>
        </FadeInView>
      )}

      <FadeInView delay={210}>
        <Card>
          <View style={styles.cardHeader}>
            <Icon name="medkit-outline" size="md" color={colors.primary} />
            <Text style={styles.cardTitle}>Medication log</Text>
          </View>
          <Text style={styles.hint}>
            {viewingOther && viewedPersonName
              ? `Medications for ${viewedPersonName}. Upload a prescription from Home or add manually below.`
              : "Upload a prescription from Home to extract medications automatically. Manual entry below is a fallback."}
            {" "}No dosing advice — log and correlate with lab trends only.
          </Text>
          {medications.filter((m) => m.active).length === 0 ? (
            <Text style={styles.emptyFamilies}>No active medications logged.</Text>
          ) : (
            <View style={styles.medList}>
              {medications
                .filter((m) => m.active)
                .map((med) => (
                  <View key={med.id} style={styles.medRow}>
                    <Text style={styles.medName}>{med.medicationName}</Text>
                    <Text style={styles.medMeta}>
                      {med.courseType === "CHRONIC" ? "Ongoing" : med.courseType === "ACUTE" ? "Short course" : "Medication"}
                      {med.startedOn ? ` · since ${med.startedOn}` : ""}
                      {med.dosage ? ` · ${med.dosage}` : ""}
                    </Text>
                    {med.scheduleText ? (
                      <Text style={styles.medSchedule}>{med.scheduleText}</Text>
                    ) : null}
                    <View style={styles.medActions}>
                      <Pressable
                        onPress={() => confirmEndMedication(med)}
                        disabled={medActionId === med.id}
                        style={styles.medActionBtn}
                      >
                        <Text style={styles.medActionEnd}>
                          {medActionId === med.id ? "Updating…" : "Mark ended"}
                        </Text>
                      </Pressable>
                      <Pressable
                        onPress={() => confirmDeleteMedication(med)}
                        disabled={medActionId === med.id}
                        style={styles.medActionBtn}
                      >
                        <Text style={styles.medActionDelete}>Delete</Text>
                      </Pressable>
                    </View>
                  </View>
                ))}
            </View>
          )}
          {medications.filter((m) => !m.active).length > 0 && (
            <View style={styles.medList}>
              <Text style={styles.medPastTitle}>Past medications</Text>
              {medications
                .filter((m) => !m.active)
                .map((med) => (
                  <View key={med.id} style={styles.medRowMuted}>
                    <Text style={styles.medName}>{med.medicationName}</Text>
                    <Text style={styles.medMeta}>
                      {med.startedOn ? `${med.startedOn}` : "—"}
                      {med.endedOn ? ` → ${med.endedOn}` : ""}
                    </Text>
                    <Pressable
                      onPress={() => confirmDeleteMedication(med)}
                      disabled={medActionId === med.id}
                      style={styles.medActionBtn}
                    >
                      <Text style={styles.medActionDelete}>Delete</Text>
                    </Pressable>
                  </View>
                ))}
            </View>
          )}
          <TextInput
            style={styles.input}
            placeholder="Medication name"
            placeholderTextColor={colors.textMuted}
            value={medName}
            onChangeText={setMedName}
          />
          <TextInput
            style={styles.input}
            placeholder="Started on (YYYY-MM-DD)"
            placeholderTextColor={colors.textMuted}
            value={medStartedOn}
            onChangeText={setMedStartedOn}
            autoCapitalize="none"
          />
          <TextInput
            style={styles.input}
            placeholder="Schedule (e.g. 1 tablet after breakfast)"
            placeholderTextColor={colors.textMuted}
            value={medSchedule}
            onChangeText={setMedSchedule}
          />
          <Text style={styles.medFieldLabel}>Course type</Text>
          <View style={confirmFormStyles.chipRow}>
            {(
              [
                { value: "CHRONIC" as MedicationCourseType, label: "Ongoing" },
                { value: "ACUTE" as MedicationCourseType, label: "Short course" },
                { value: "UNKNOWN" as MedicationCourseType, label: "Unsure" },
              ] as const
            ).map((option) => (
              <Pressable
                key={option.value}
                style={[confirmFormStyles.chip, medCourseType === option.value && confirmFormStyles.chipActive]}
                onPress={() => setMedCourseType(option.value)}
              >
                <Text
                  style={[
                    confirmFormStyles.chipText,
                    medCourseType === option.value && confirmFormStyles.chipTextActive,
                  ]}
                >
                  {option.label}
                </Text>
              </Pressable>
            ))}
          </View>
          <Button variant="secondary" onPress={handleAddMedication} loading={medSaving} style={styles.actionBtn}>
            Add medication
          </Button>
        </Card>
      </FadeInView>

      <FadeInView delay={200}>
        <Card>
          <View style={styles.cardHeader}>
            <Icon name="download-outline" size="md" color={colors.primary} />
            <Text style={styles.cardTitle}>App updates</Text>
          </View>
          <Text style={styles.hint}>
            {Platform.OS === "android"
              ? "Checks your edge server for a newer APK and prompts to install."
              : "On Android, install from the downloads page; web updates when you refresh."}
          </Text>
          {Platform.OS === "android" ? (
            <Button variant="secondary" onPress={() => checkForApkUpdate(true)} style={styles.actionBtn}>
              Check for update
            </Button>
          ) : (
            <Button
              variant="secondary"
              onPress={() =>
                Linking.openURL(`${process.env.EXPO_PUBLIC_API_URL ?? ""}/downloads/`).catch(() =>
                  Alert.alert("Could not open downloads page")
                )
              }
              style={styles.actionBtn}
            >
              Open APK downloads
            </Button>
          )}
        </Card>
      </FadeInView>

      <FadeInView delay={220}>
        <Card>
          <View style={styles.cardHeader}>
            <Icon name="chatbox-ellipses-outline" size="md" color={colors.primary} />
            <Text style={styles.cardTitle}>Tester feedback</Text>
          </View>
          <Text style={styles.hint}>
            Share what's working, what's broken, and ideas for improvement — it goes straight to the product team.
          </Text>
          <Button
            variant="secondary"
            onPress={() => router.push({ pathname: "/(app)/feedback", params: { from: "settings" } })}
            style={styles.actionBtn}
          >
            Send feedback
          </Button>
        </Card>
      </FadeInView>

      <FadeInView delay={240}>
        <Card>
          <View style={styles.cardHeader}>
            <Icon name="options-outline" size="md" color={colors.primary} />
            <Text style={styles.cardTitle}>App mode</Text>
          </View>
          <View style={styles.modeValueRow}>
            <Text style={styles.value}>
              {isPlatformAdmin(session) ? "Platform admin" : uiModeAdvanced ? "Advanced" : "Basic"}
            </Text>
            <View style={[styles.modeBadge, isAdvanced && styles.modeBadgeAdvanced]}>
              <Text style={styles.modeBadgeText}>
                {isPlatformAdmin(session) ? "ADM" : uiModeAdvanced ? "ADV" : "BASIC"}
              </Text>
            </View>
          </View>
          <Text style={styles.hint}>
            {isPlatformAdmin(session)
              ? "All dashboards, family tools, trends, and platform ops are enabled for your account."
              : isAdvanced
                ? "Family management, upload for others, charts, and full reports."
                : "Simple home view with your own reports and family feed."}
          </Text>
          <Button variant="secondary" onPress={toggleMode} loading={saving} style={styles.actionBtn}>
            Switch UI to {uiModeAdvanced ? "Basic" : "Advanced"}
          </Button>
        </Card>
      </FadeInView>
    </Screen>
  );
}
