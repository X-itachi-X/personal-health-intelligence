import { router } from "expo-router";
import { useCallback, useEffect, useState } from "react";
import { Platform, StyleSheet, Text, useWindowDimensions, View } from "react-native";
import {
  FamilyHealthSnapshot,
  fetchAvailableBiomarkers,
  fetchFamilySnapshot,
  fetchPersonInsights,
  fetchPersonReminders,
  fetchPersonTrend,
  InsightCard,
  PersonTrendResponse,
  RetestReminder,
} from "../../lib/api";
import { TrendLineChart } from "../../components/charts/TrendLineChart";
import { getSession, AuthSession } from "../../lib/auth";
import { hasAdvancedAccess } from "../../lib/session";
import { formatBiomarkerName } from "../../lib/biomarkers";
import { useFamily } from "../../lib/FamilyContext";
import { IconName } from "../../lib/icons";
import { colors, radii, spacing, typography } from "../../lib/theme";
import { AnimatedPressable } from "../../components/ui/AnimatedPressable";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { FadeInView } from "../../components/ui/FadeInView";
import { Icon } from "../../components/ui/Icon";
import { Screen } from "../../components/ui/Screen";
import { Skeleton, SkeletonCard } from "../../components/ui/Skeleton";
import { ViewingPersonBanner } from "../../components/ViewingPersonBanner";

const KEY_BIOMARKERS: string[] = [
  "vitamin_d",
  "hba1c",
  "ldl_cholesterol",
  "fasting_glucose",
  "tsh",
];

type TrendTile = {
  canonical: string;
  trend: PersonTrendResponse;
};

export default function DashboardScreen() {
  const { width } = useWindowDimensions();
  const wide = width >= 960;
  const {
    activeFamily,
    effectivePersonId,
    isViewingOther,
    viewedPersonName,
    viewedMember,
    clearViewedPerson,
    viewedPersonId,
  } = useFamily();

  const [session, setSession] = useState<AuthSession | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [snapshot, setSnapshot] = useState<FamilyHealthSnapshot | null>(null);
  const [remindersSummary, setRemindersSummary] = useState<string | null>(null);
  const [reminders, setReminders] = useState<RetestReminder[]>([]);
  const [insights, setInsights] = useState<InsightCard[]>([]);
  const [trendTiles, setTrendTiles] = useState<TrendTile[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setError(null);
      const s = await getSession();
      setSession(s);

      if (!s || !hasAdvancedAccess(s) || !s.personId) {
        return;
      }

      const personId = effectivePersonId(s.personId);
      const familyId = activeFamily?.id;

      const [snapshotResult, remindersResult, insightsResult, available] = await Promise.all([
        familyId ? fetchFamilySnapshot(familyId).catch(() => null) : Promise.resolve(null),
        fetchPersonReminders(personId).catch(() => null),
        fetchPersonInsights(personId).catch(() => ({ personId, cards: [] })),
        fetchAvailableBiomarkers(personId).catch(() => [] as string[]),
      ]);

      setSnapshot(snapshotResult);
      if (remindersResult) {
        setRemindersSummary(remindersResult.summary);
        setReminders(remindersResult.reminders);
      } else {
        setRemindersSummary(null);
        setReminders([]);
      }
      setInsights(insightsResult.cards);

      const canonicals = KEY_BIOMARKERS.filter((canonical) => available.includes(canonical)).slice(0, 4);
      const trends = await Promise.all(
        canonicals.map(async (canonical) => ({
          canonical,
          trend: await fetchPersonTrend(personId, canonical),
        }))
      );
      setTrendTiles(trends.filter((tile) => tile.trend.points.length > 0));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load dashboard");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [activeFamily?.id, effectivePersonId, viewedPersonId]);

  useEffect(() => {
    setLoading(true);
    load();
  }, [load]);

  if (!hasAdvancedAccess(session)) {
    return (
      <Screen scroll={false}>
        <EmptyState
          icon="grid-outline"
          title="Advanced mode required"
          message="Switch to Advanced in Settings to open the analytics dashboard."
        />
      </Screen>
    );
  }

  if (loading) {
    return (
      <Screen contentStyle={wide ? styles.wideContainer : undefined}>
        <View style={styles.statsRow}>
          <Skeleton height={88} style={styles.statSkeleton} />
          <Skeleton height={88} style={styles.statSkeleton} />
          <Skeleton height={88} style={styles.statSkeleton} />
        </View>
        <SkeletonCard />
        <SkeletonCard />
      </Screen>
    );
  }

  const viewingOther = session?.personId ? isViewingOther(session.personId) : false;
  const actionableReminders = reminders.filter(
    (reminder) => reminder.urgency === "OVERDUE" || reminder.urgency === "DUE_SOON"
  );

  return (
    <Screen
      refreshing={refreshing}
      onRefresh={() => {
        setRefreshing(true);
        load();
      }}
      contentStyle={wide ? styles.wideContainer : undefined}
    >
      {viewingOther && viewedPersonName && (
        <ViewingPersonBanner
          name={viewedPersonName}
          profileOnly={viewedMember != null && !viewedMember.hasAccount}
          onClear={() => clearViewedPerson()}
        />
      )}

      <FadeInView delay={0}>
        <View style={styles.hero}>
          <View>
            <Text style={styles.heroTitle}>
              {viewingOther && viewedPersonName ? `${viewedPersonName}'s dashboard` : "Health dashboard"}
            </Text>
            <Text style={styles.heroSubtitle}>
              Structured analytics from DuckDB — line charts, family snapshot, and follow-ups on mobile and web.
            </Text>
          </View>
          {activeFamily && (
            <View style={styles.familyPill}>
              <Icon name="people-outline" size="sm" color={colors.primaryDark} />
              <Text style={styles.familyPillText}>{activeFamily.displayName}</Text>
            </View>
          )}
        </View>
      </FadeInView>

      {error && (
        <Card variant="outlined" style={styles.errorCard}>
          <Text style={styles.errorText}>{error}</Text>
        </Card>
      )}

      {snapshot && (
        <FadeInView delay={40}>
          <View style={[styles.statsRow, wide && styles.statsRowWide]}>
            <StatCard
              label="Family members"
              value={String(snapshot.memberCount)}
              icon="people-outline"
            />
            <StatCard
              label="Reports with data"
              value={String(snapshot.reportsWithData)}
              icon="document-text-outline"
            />
            <StatCard
              label="Need attention"
              value={String(snapshot.outOfRangeMembers)}
              icon="alert-circle-outline"
              accent={snapshot.outOfRangeMembers > 0}
            />
          </View>
        </FadeInView>
      )}

      <View style={wide ? styles.columns : styles.stack}>
        <View style={wide ? styles.mainColumn : styles.stack}>
          {snapshot && snapshot.members.length > 0 && (
            <FadeInView delay={80}>
              <Card>
                <View style={styles.cardHeader}>
                  <Icon name="people" size="md" color={colors.primary} />
                  <Text style={styles.cardTitle}>Family roster</Text>
                </View>
                <View style={styles.tableHeader}>
                  <Text style={[styles.tableHead, styles.tableNameCol]}>Member</Text>
                  <Text style={styles.tableHead}>Latest lab</Text>
                  <Text style={styles.tableHead}>Out of range</Text>
                </View>
                {snapshot.members.map((member) => (
                  <View key={member.personId} style={styles.tableRow}>
                    <Text style={[styles.tableCell, styles.tableNameCol]}>{member.personName}</Text>
                    <Text style={styles.tableCell}>{member.latestReportDate ?? "—"}</Text>
                    <Text
                      style={[
                        styles.tableCell,
                        member.outOfRangeCount > 0 && styles.tableCellWarning,
                      ]}
                    >
                      {member.outOfRangeCount > 0 ? member.outOfRangeCount : "0"}
                    </Text>
                  </View>
                ))}
              </Card>
            </FadeInView>
          )}

          {trendTiles.length > 0 && (
            <FadeInView delay={120}>
              <View style={[styles.trendGrid, wide && styles.trendGridWide]}>
                {trendTiles.map((tile) => (
                  <Card key={tile.canonical} style={wide ? styles.trendCardWide : undefined}>
                    <Text style={styles.trendLabel}>{formatBiomarkerName(tile.canonical)}</Text>
                    <Text style={styles.trendLatest}>
                      {tile.trend.insight?.latestValue ?? tile.trend.points.at(-1)?.value}{" "}
                      {tile.trend.unit ?? tile.trend.points.at(-1)?.unit ?? ""}
                    </Text>
                    {tile.trend.insight && tile.trend.insight.direction !== "INSUFFICIENT_DATA" && (
                      <Text style={styles.trendInsight}>{tile.trend.insight.summary}</Text>
                    )}
                    <TrendLineChart
                      points={tile.trend.points.map((p) => ({ date: p.date, value: p.value }))}
                      unit={tile.trend.unit}
                      height={140}
                      compact
                    />
                    <Text style={styles.trendPoints}>{tile.trend.points.length} readings on file</Text>
                  </Card>
                ))}
              </View>
              <AnimatedPressable style={styles.linkRow} onPress={() => router.push("/(app)/trends" as never)}>
                <Text style={styles.linkText}>Open full trends</Text>
                <Icon name="chevron-forward" size="sm" color={colors.primaryDark} />
              </AnimatedPressable>
            </FadeInView>
          )}
        </View>

        <View style={wide ? styles.sideColumn : styles.stack}>
          <FadeInView delay={100}>
            <Card>
              <View style={styles.cardHeader}>
                <Icon name="calendar-outline" size="md" color={colors.primary} />
                <Text style={styles.cardTitle}>Follow-up tests</Text>
              </View>
              {remindersSummary ? (
                <Text style={styles.summaryText}>{remindersSummary}</Text>
              ) : (
                <Skeleton height={14} width="80%" />
              )}
              {actionableReminders.slice(0, 4).map((reminder) => (
                <View
                  key={reminder.id}
                  style={[
                    styles.reminderRow,
                    reminder.urgency === "OVERDUE" && styles.reminderRowOverdue,
                  ]}
                >
                  <Text style={styles.reminderLabel}>{reminder.label}</Text>
                  <Text style={styles.reminderMeta}>Due {reminder.dueDate}</Text>
                </View>
              ))}
            </Card>
          </FadeInView>

          {insights.length > 0 && (
            <FadeInView delay={140}>
              <Card>
                <View style={styles.cardHeader}>
                  <Icon name="bulb-outline" size="md" color={colors.primary} />
                  <Text style={styles.cardTitle}>Insights</Text>
                </View>
                {insights.slice(0, 5).map((card) => (
                  <AnimatedPressable
                    key={card.id}
                    style={[
                      styles.insightRow,
                      card.severity === "warning" && styles.insightRowWarning,
                    ]}
                    onPress={() => card.reportId && router.push(`/(app)/report/${card.reportId}` as never)}
                    disabled={!card.reportId}
                  >
                    <Text style={styles.insightTitle}>{card.title}</Text>
                    <Text style={styles.insightMessage}>{card.message}</Text>
                  </AnimatedPressable>
                ))}
              </Card>
            </FadeInView>
          )}

          <FadeInView delay={180}>
            <Card>
              <View style={styles.cardHeader}>
                <Icon name="flash-outline" size="md" color={colors.primary} />
                <Text style={styles.cardTitle}>Quick actions</Text>
              </View>
              <View style={styles.actionGrid}>
                <QuickAction label="Trends" icon="trending-up-outline" route="/(app)/trends" />
                <QuickAction label="Ask" icon="chatbubbles-outline" route="/(app)/ask" />
                <QuickAction label="Reports" icon="document-text-outline" route="/(app)/reports" />
                <QuickAction label="Family" icon="people-outline" route="/(app)/family" />
              </View>
            </Card>
          </FadeInView>
        </View>
      </View>
    </Screen>
  );
}

function StatCard({
  label,
  value,
  icon,
  accent = false,
}: {
  label: string;
  value: string;
  icon: IconName;
  accent?: boolean;
}) {
  return (
    <Card style={StyleSheet.flatten([styles.statCard, accent && styles.statCardAccent])}>
      <Icon name={icon} size="md" color={accent ? colors.danger : colors.primary} />
      <Text style={styles.statValue}>{value}</Text>
      <Text style={styles.statLabel}>{label}</Text>
    </Card>
  );
}

function QuickAction({
  label,
  icon,
  route,
}: {
  label: string;
  icon: IconName;
  route: string;
}) {
  return (
    <AnimatedPressable style={styles.actionBtn} onPress={() => router.push(route as never)}>
      <Icon name={icon} size="md" color={colors.primaryDark} />
      <Text style={styles.actionLabel}>{label}</Text>
    </AnimatedPressable>
  );
}

const styles = StyleSheet.create({
  wideContainer: {
    maxWidth: 1200,
    alignSelf: "center",
    width: "100%",
  },
  hero: {
    flexDirection: "row",
    alignItems: "flex-start",
    justifyContent: "space-between",
    gap: spacing.md,
    flexWrap: "wrap",
  },
  heroTitle: { ...typography.hero, fontSize: 28 },
  heroSubtitle: { ...typography.body, marginTop: 6, maxWidth: 560 },
  familyPill: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    backgroundColor: colors.primaryLight,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: radii.sm,
  },
  familyPillText: { fontWeight: "700", color: colors.primaryDark },
  errorCard: { borderColor: colors.dangerBorder },
  errorText: { color: colors.danger },
  statsRow: { gap: spacing.sm },
  statsRowWide: { flexDirection: "row" },
  statSkeleton: { flex: 1, borderRadius: radii.md },
  statCard: { gap: 4, flex: 1 },
  statCardAccent: { backgroundColor: colors.dangerLight },
  statValue: { fontSize: 28, fontWeight: "800", color: colors.text, marginTop: 4 },
  statLabel: { ...typography.caption },
  columns: { flexDirection: "row", gap: spacing.md },
  stack: { gap: spacing.md },
  mainColumn: { flex: 1.6, gap: spacing.md },
  sideColumn: { flex: 1, gap: spacing.md },
  cardHeader: { flexDirection: "row", alignItems: "center", gap: spacing.sm, marginBottom: spacing.sm },
  cardTitle: { ...typography.subtitle, fontSize: 16 },
  tableHeader: {
    flexDirection: "row",
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
    paddingBottom: spacing.xs,
    marginBottom: spacing.xs,
  },
  tableHead: { ...typography.caption, fontWeight: "700", flex: 1 },
  tableNameCol: { flex: 1.4 },
  tableRow: {
    flexDirection: "row",
    paddingVertical: 10,
    borderTopWidth: 1,
    borderTopColor: colors.borderLight,
  },
  tableCell: { ...typography.body, flex: 1, fontSize: 14 },
  tableCellWarning: { color: colors.danger, fontWeight: "700" },
  trendGrid: { gap: spacing.sm },
  trendGridWide: { flexDirection: "row", flexWrap: "wrap" },
  trendCardWide: { flexBasis: "48%", flexGrow: 1 },
  trendLabel: { ...typography.subtitle, fontSize: 15 },
  trendLatest: { fontSize: 22, fontWeight: "800", color: colors.primaryDark, marginTop: 4 },
  trendInsight: { ...typography.caption, marginTop: 6, lineHeight: 18 },
  trendPoints: { ...typography.caption, marginTop: 8, color: colors.textSoft },
  linkRow: { flexDirection: "row", alignItems: "center", gap: 4, marginTop: spacing.sm },
  linkText: { color: colors.primaryDark, fontWeight: "600" },
  summaryText: { ...typography.body, marginBottom: spacing.sm },
  reminderRow: {
    borderTopWidth: 1,
    borderTopColor: colors.border,
    paddingVertical: spacing.sm,
    gap: 2,
  },
  reminderRowOverdue: { backgroundColor: colors.warningLight, marginHorizontal: -spacing.md, paddingHorizontal: spacing.md },
  reminderLabel: { fontSize: 14, fontWeight: "700", color: colors.text },
  reminderMeta: { ...typography.caption },
  insightRow: { borderTopWidth: 1, borderTopColor: colors.border, paddingVertical: spacing.sm, gap: 4 },
  insightRowWarning: { backgroundColor: colors.warningLight, marginHorizontal: -spacing.md, paddingHorizontal: spacing.md },
  insightTitle: { fontSize: 14, fontWeight: "700", color: colors.text },
  insightMessage: { ...typography.caption, lineHeight: 18 },
  actionGrid: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm },
  actionBtn: {
    flexBasis: "47%",
    flexGrow: 1,
    alignItems: "center",
    gap: 6,
    backgroundColor: colors.surfaceMuted,
    borderRadius: radii.md,
    paddingVertical: spacing.md,
  },
  actionLabel: { fontSize: 13, fontWeight: "600", color: colors.primaryDark },
});
