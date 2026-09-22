import { router, useLocalSearchParams } from "expo-router";
import { useEffect, useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import { fetchImagingStudy, ImagingStudy } from "../../../lib/api";
import { getSession } from "../../../lib/auth";
import { colors, spacing, typography } from "../../../lib/theme";
import { Button } from "../../../components/ui/Button";
import { Card } from "../../../components/ui/Card";
import { FadeInView } from "../../../components/ui/FadeInView";
import { Icon } from "../../../components/ui/Icon";
import { Screen } from "../../../components/ui/Screen";
import { SkeletonCard } from "../../../components/ui/Skeleton";

export default function ImagingStudyDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const [study, setStudy] = useState<ImagingStudy | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    getSession()
      .then(async (session) => {
        if (!session?.personId) {
          throw new Error("Sign in required");
        }
        return fetchImagingStudy(session.personId, id);
      })
      .then(setStudy)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load imaging study"));
  }, [id]);

  if (error) {
    return (
      <Screen>
        <Card variant="outlined" style={styles.errorCard}>
          <Text style={styles.error}>{error}</Text>
        </Card>
      </Screen>
    );
  }

  if (!study) {
    return (
      <Screen>
        <SkeletonCard />
        <SkeletonCard />
      </Screen>
    );
  }

  return (
    <Screen>
      <FadeInView delay={0}>
        <View style={styles.header}>
          <View style={styles.iconWrap}>
            <Icon name="scan-outline" size="lg" color={colors.primary} />
          </View>
          <Text style={styles.title}>
            {study.modality.replace(/_/g, " ")}
            {study.bodyRegion ? ` · ${study.bodyRegion}` : ""}
          </Text>
          <Text style={styles.meta}>{study.studyDate}</Text>
          {study.facility ? <Text style={styles.meta}>{study.facility}</Text> : null}
        </View>
      </FadeInView>

      {study.impression && (
        <FadeInView delay={40}>
          <Card>
            <Text style={styles.sectionTitle}>Impression</Text>
            <Text style={styles.body}>{study.impression}</Text>
          </Card>
        </FadeInView>
      )}

      {study.findings.length > 0 && (
        <FadeInView delay={80}>
          <Card>
            <Text style={styles.sectionTitle}>Findings</Text>
            {study.findings.map((finding) => (
              <View key={finding.id} style={styles.findingRow}>
                <Text style={styles.findingText}>{finding.findingText}</Text>
                {finding.severity ? (
                  <Text style={styles.findingMeta}>Severity: {finding.severity}</Text>
                ) : null}
              </View>
            ))}
          </Card>
        </FadeInView>
      )}

      {study.sourceReportId != null && (
        <FadeInView delay={120}>
          <Button
            variant="secondary"
            onPress={() => router.push(`/(app)/report/${study.sourceReportId}` as never)}
          >
            View source report
          </Button>
        </FadeInView>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { alignItems: "center", gap: spacing.sm, marginBottom: spacing.sm },
  iconWrap: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: colors.primaryLight,
    alignItems: "center",
    justifyContent: "center",
  },
  title: { ...typography.title, textAlign: "center", textTransform: "capitalize" },
  meta: { ...typography.caption },
  sectionTitle: { ...typography.subtitle, fontSize: 15, marginBottom: spacing.sm },
  body: { ...typography.body, lineHeight: 22 },
  findingRow: {
    paddingVertical: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  findingText: { ...typography.body, lineHeight: 20 },
  findingMeta: { ...typography.caption, marginTop: 4, color: colors.textMuted },
  errorCard: { borderColor: colors.dangerBorder },
  error: { color: colors.danger },
});
