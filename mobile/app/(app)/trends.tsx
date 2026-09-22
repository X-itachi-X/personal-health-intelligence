import { router } from "expo-router";
import { useCallback, useEffect, useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import {
  FamilyCompareResponse,
  fetchAvailableBiomarkers,
  fetchFamilyCompare,
  fetchPersonTrend,
  ImagingStudy,
  listImagingStudies,
  PersonTrendResponse,
  TrendPoint,
} from "../../lib/api";
import { TrendLineChart } from "../../components/charts/TrendLineChart";
import { getSession, AuthSession } from "../../lib/auth";
import { hasAdvancedAccess } from "../../lib/session";
import { formatBiomarkerName } from "../../lib/biomarkers";
import { useFamily } from "../../lib/FamilyContext";
import { colors, radii, spacing, typography } from "../../lib/theme";
import { AnimatedPressable } from "../../components/ui/AnimatedPressable";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { FadeInView } from "../../components/ui/FadeInView";
import { Icon } from "../../components/ui/Icon";
import { Screen } from "../../components/ui/Screen";
import { SkeletonCard } from "../../components/ui/Skeleton";
import { ViewingPersonBanner } from "../../components/ViewingPersonBanner";

type ViewMode = "personal" | "family";

export default function TrendsScreen() {
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
  const [mode, setMode] = useState<ViewMode>("personal");
  const [canonicals, setCanonicals] = useState<string[]>([]);
  const [selectedCanonical, setSelectedCanonical] = useState<string | null>(null);
  const [personalTrend, setPersonalTrend] = useState<PersonTrendResponse | null>(null);
  const [familyCompare, setFamilyCompare] = useState<FamilyCompareResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [imagingStudies, setImagingStudies] = useState<ImagingStudy[]>([]);

  const load = useCallback(async () => {
    try {
      setError(null);
      const s = await getSession();
      setSession(s);

      if (!s || !hasAdvancedAccess(s) || !s.personId) {
        return;
      }

      const personId = effectivePersonId(s.personId);

      const [available, imaging] = await Promise.all([
        fetchAvailableBiomarkers(personId),
        listImagingStudies(personId).catch(() => []),
      ]);
      setImagingStudies(imaging);
      setCanonicals(available);
      const canonical = selectedCanonical && available.includes(selectedCanonical)
        ? selectedCanonical
        : available[0] ?? null;
      setSelectedCanonical(canonical);

      if (!canonical) {
        setPersonalTrend(null);
        setFamilyCompare(null);
        return;
      }

      if (mode === "personal") {
        const trend = await fetchPersonTrend(personId, canonical);
        setPersonalTrend(trend);
        setFamilyCompare(null);
      } else if (activeFamily) {
        const compare = await fetchFamilyCompare(activeFamily.id, canonical);
        setFamilyCompare(compare);
        setPersonalTrend(null);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load trends");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [activeFamily?.id, mode, selectedCanonical, viewedPersonId, effectivePersonId]);

  useEffect(() => {
    setLoading(true);
    load();
  }, [load]);

  if (!hasAdvancedAccess(session)) {
    return (
      <Screen scroll={false}>
        <EmptyState
          icon="trending-up-outline"
          title="Advanced mode required"
          message="Switch to Advanced in Settings to view biomarker trends."
        />
      </Screen>
    );
  }

  if (loading && canonicals.length === 0) {
    return (
      <Screen>
        <SkeletonCard />
        <SkeletonCard />
      </Screen>
    );
  }

  const viewingOther = session?.personId ? isViewingOther(session.personId) : false;

  return (
    <Screen refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }}>
      {viewingOther && viewedPersonName && (
        <ViewingPersonBanner
          name={viewedPersonName}
          profileOnly={viewedMember != null && !viewedMember.hasAccount}
          onClear={() => clearViewedPerson()}
        />
      )}

      {error && (
        <Card variant="outlined" style={styles.errorCard}>
          <Text style={styles.error}>{error}</Text>
        </Card>
      )}

      {imagingStudies.length > 0 && (
        <FadeInView delay={0}>
          <Card>
            <View style={styles.imagingHeader}>
              <Icon name="scan-outline" size="md" color={colors.primary} />
              <Text style={styles.imagingTitle}>Imaging timeline</Text>
            </View>
            {imagingStudies.slice(0, 4).map((study) => (
              <AnimatedPressable
                key={study.id}
                style={styles.imagingRow}
                onPress={() => router.push(`/(app)/imaging/${study.id}` as never)}
              >
                <Text style={styles.imagingMeta}>
                  {study.modality.replace(/_/g, " ")}
                  {study.bodyRegion ? ` · ${study.bodyRegion}` : ""} · {study.studyDate}
                </Text>
                <Text style={styles.imagingImpression}>{study.impression}</Text>
              </AnimatedPressable>
            ))}
          </Card>
        </FadeInView>
      )}

      {canonicals.length === 0 ? (
        imagingStudies.length === 0 ? (
          <FadeInView>
            <EmptyState
              icon="analytics-outline"
              title="No trend data yet"
              message="Load demo data in Settings or upload lab reports and scan reports. Analytics sync runs automatically after extraction."
            />
          </FadeInView>
        ) : null
      ) : (
        <>
          <FadeInView delay={0}>
            <View style={styles.modeRow}>
              <AnimatedPressable
                style={[styles.modeChip, mode === "personal" && styles.modeChipActive]}
                onPress={() => setMode("personal")}
              >
                <Icon
                  name="person-outline"
                  size="sm"
                  color={mode === "personal" ? colors.primaryDark : colors.textMuted}
                />
                <Text style={[styles.modeText, mode === "personal" && styles.modeTextActive]}>My trends</Text>
              </AnimatedPressable>
              <AnimatedPressable
                style={[styles.modeChip, mode === "family" && styles.modeChipActive]}
                onPress={() => setMode("family")}
              >
                <Icon
                  name="people-outline"
                  size="sm"
                  color={mode === "family" ? colors.primaryDark : colors.textMuted}
                />
                <Text style={[styles.modeText, mode === "family" && styles.modeTextActive]}>Family compare</Text>
              </AnimatedPressable>
            </View>
          </FadeInView>

          <FadeInView delay={60}>
            <Text style={styles.sectionLabel}>Biomarker</Text>
            <View style={styles.chipRow}>
              {canonicals.map((c) => (
                <AnimatedPressable
                  key={c}
                  style={[styles.chip, selectedCanonical === c && styles.chipActive]}
                  onPress={() => setSelectedCanonical(c)}
                >
                  <Text style={[styles.chipText, selectedCanonical === c && styles.chipTextActive]}>
                    {formatBiomarkerName(c)}
                  </Text>
                </AnimatedPressable>
              ))}
            </View>
          </FadeInView>

          {mode === "personal" && personalTrend && (
            <FadeInView delay={120}>
              <TrendCard
                title={`${formatBiomarkerName(personalTrend.canonical)} over time`}
                points={personalTrend.points}
                unit={personalTrend.unit}
                insight={personalTrend.insight}
              />
            </FadeInView>
          )}

          {mode === "family" && familyCompare && (
            <FadeInView delay={120}>
              <View style={styles.compareSection}>
                <Text style={styles.compareTitle}>
                  {formatBiomarkerName(familyCompare.canonical)} — family comparison
                </Text>
                {familyCompare.members.length === 0 ? (
                  <Text style={styles.hint}>No family data for this biomarker yet.</Text>
                ) : (
                  familyCompare.members.map((member, i) => (
                    <FadeInView key={member.personId} delay={i * 60}>
                      <Card>
                        <Text style={styles.memberName}>{member.personName}</Text>
                        <TrendLineChart
                          points={member.points.map((p) => ({ date: p.date, value: p.value }))}
                          unit={member.points[0]?.unit ?? ""}
                          height={180}
                          compact
                        />
                      </Card>
                    </FadeInView>
                  ))
                )}
              </View>
            </FadeInView>
          )}
        </>
      )}
    </Screen>
  );
}

function trendInsightLabel(direction: string): string {
  switch (direction) {
    case "INCREASING":
      return "Trending up";
    case "DECREASING":
      return "Trending down";
    case "STABLE":
      return "Holding steady";
    case "FLUCTUATING":
      return "Fluctuating";
    default:
      return "Need more readings";
  }
}

function TrendCard({
  title,
  points,
  unit,
  insight,
}: {
  title: string;
  points: TrendPoint[];
  unit: string | null;
  insight?: PersonTrendResponse["insight"];
}) {
  return (
    <Card>
      <View style={styles.cardTitleRow}>
        <Icon name="trending-up" size="md" color={colors.primary} />
        <Text style={styles.cardTitle}>{title}</Text>
      </View>
      {insight && insight.direction !== "INSUFFICIENT_DATA" && (
        <View style={styles.insightBanner}>
          <Text style={styles.insightLabel}>{trendInsightLabel(insight.direction)}</Text>
          <Text style={styles.insightSummary}>{insight.summary}</Text>
        </View>
      )}
      {points.length === 0 ? (
        <Text style={styles.hint}>No data points yet.</Text>
      ) : (
        <>
          <TrendLineChart
            points={points.map((p) => ({ date: p.date, value: p.value }))}
            unit={unit}
            height={220}
          />
          {points.map((p, i) => (
            <View key={p.reportId} style={[styles.pointRow, i === 0 && styles.pointRowFirst]}>
              <Text style={styles.pointDate}>{p.date}</Text>
              <Text style={styles.pointValue}>{p.value} {p.unit ?? unit ?? ""}</Text>
            </View>
          ))}
        </>
      )}
    </Card>
  );
}

const styles = StyleSheet.create({
  errorCard: { borderColor: colors.dangerBorder },
  error: { color: colors.danger, textAlign: "center" },
  hint: { ...typography.body, textAlign: "center" },
  imagingHeader: { flexDirection: "row", alignItems: "center", gap: spacing.sm, marginBottom: spacing.sm },
  imagingTitle: { ...typography.subtitle, fontSize: 16 },
  imagingRow: { borderTopWidth: 1, borderTopColor: colors.border, paddingTop: spacing.sm, marginTop: spacing.sm },
  imagingMeta: { ...typography.caption, textTransform: "uppercase" },
  imagingImpression: { ...typography.body, marginTop: 4, lineHeight: 20 },
  modeRow: { flexDirection: "row", gap: spacing.sm },
  modeChip: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 6,
    paddingVertical: 12,
    borderRadius: radii.md,
    backgroundColor: colors.surfaceMuted,
  },
  modeChipActive: { backgroundColor: colors.primaryLight },
  modeText: { fontSize: 14, fontWeight: "600", color: colors.textMuted },
  modeTextActive: { color: colors.primaryDark },
  sectionLabel: { ...typography.label },
  chipRow: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm, paddingVertical: spacing.xs },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: radii.sm,
    backgroundColor: colors.surfaceMuted,
    borderWidth: 1,
    borderColor: colors.border,
  },
  chipActive: { backgroundColor: colors.primaryLight, borderColor: colors.primary },
  chipText: { fontSize: 14, color: colors.textMuted },
  chipTextActive: { color: colors.primaryDark, fontWeight: "700" },
  cardTitleRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm, marginBottom: spacing.sm },
  cardTitle: { ...typography.subtitle, fontSize: 16 },
  insightBanner: {
    backgroundColor: colors.primaryLight,
    borderRadius: radii.md,
    padding: spacing.md,
    gap: 4,
    marginBottom: spacing.sm,
  },
  insightLabel: { fontSize: 13, fontWeight: "700", color: colors.primaryDark },
  insightSummary: { ...typography.caption, lineHeight: 20, color: colors.primaryDark },
  pointRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    paddingVertical: 8,
    borderTopWidth: 1,
    borderTopColor: colors.borderLight,
  },
  pointRowFirst: { borderTopWidth: 0 },
  pointDate: { ...typography.caption },
  pointValue: { ...typography.data },
  compareSection: { gap: spacing.md },
  compareTitle: { ...typography.subtitle, fontSize: 16 },
  memberName: { ...typography.subtitle, fontSize: 15, marginBottom: spacing.sm },
});
