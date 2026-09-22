import { router } from "expo-router";
import { useCallback, useEffect, useState } from "react";
import { Alert, StyleSheet, Text, View } from "react-native";
import { deleteReport, DocumentType, listReports, ReportSummary } from "../../lib/api";
import { documentTypeBadgeLabel } from "../../lib/documentTypes";
import { useFamily } from "../../lib/FamilyContext";
import { extractionStatusBadgeLabel, extractionStatusVariant } from "../../lib/extraction";
import { useTheme } from "../../lib/ThemeContext";
import { useThemedStyles } from "../../lib/useThemedStyles";
import { spacing } from "../../lib/theme";
import { AnimatedPressable } from "../../components/ui/AnimatedPressable";
import { Badge } from "../../components/ui/Badge";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { FadeInView } from "../../components/ui/FadeInView";
import { Icon } from "../../components/ui/Icon";
import { Screen } from "../../components/ui/Screen";
import { SkeletonCard } from "../../components/ui/Skeleton";

function documentIcon(documentType?: DocumentType): "document-text" | "medkit-outline" | "scan-outline" {
  switch (documentType) {
    case "PRESCRIPTION":
      return "medkit-outline";
    case "IMAGING_REPORT":
      return "scan-outline";
    default:
      return "document-text";
  }
}

export default function ReportsScreen() {
  const { colors } = useTheme();
  const styles = useThemedStyles(({ colors, typography }) =>
    StyleSheet.create({
      errorCard: { borderColor: colors.dangerBorder },
      errorRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
      error: { color: colors.danger, flex: 1 },
      cardHeader: { flexDirection: "row", alignItems: "center", gap: spacing.md },
      fileIcon: {
        width: 44,
        height: 44,
        borderRadius: 22,
        backgroundColor: colors.primaryLight,
        alignItems: "center",
        justifyContent: "center",
      },
      cardInfo: { flex: 1 },
      filename: { ...typography.subtitle, fontSize: 16 },
      meta: { ...typography.caption, marginTop: 2 },
      deleteBtn: {
        flexDirection: "row",
        alignItems: "center",
        gap: 6,
        alignSelf: "flex-start",
        marginTop: spacing.sm,
        paddingTop: spacing.sm,
        borderTopWidth: 1,
        borderTopColor: colors.borderLight,
        width: "100%",
      },
      deleteText: { color: colors.danger, fontSize: 14, fontWeight: "600" },
    })
  );
  const { activeFamily } = useFamily();
  const [reports, setReports] = useState<ReportSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setError(null);
      const data = await listReports(activeFamily?.id);
      setReports(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load reports");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [activeFamily?.id]);

  useEffect(() => {
    setLoading(true);
    load();
  }, [load]);

  function confirmDelete(report: ReportSummary) {
    Alert.alert(
      "Delete report?",
      `Remove "${report.filename}"? This cannot be undone.`,
      [
        { text: "Cancel", style: "cancel" },
        {
          text: "Delete",
          style: "destructive",
          onPress: async () => {
            try {
              await deleteReport(report.reportId);
              setReports((prev) => prev.filter((r) => r.reportId !== report.reportId));
            } catch (err) {
              Alert.alert("Error", err instanceof Error ? err.message : "Delete failed");
            }
          },
        },
      ]
    );
  }

  if (loading && reports.length === 0) {
    return (
      <Screen>
        <SkeletonCard />
        <SkeletonCard />
        <SkeletonCard />
      </Screen>
    );
  }

  return (
    <Screen refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }}>
      {error && (
        <Card variant="outlined" style={styles.errorCard}>
          <View style={styles.errorRow}>
            <Icon name="alert-circle-outline" size="sm" color={colors.danger} />
            <Text style={styles.error}>{error}</Text>
          </View>
        </Card>
      )}

      {reports.length === 0 && !error && (
        <FadeInView>
          <EmptyState
            icon="document-text-outline"
            title="No reports yet"
            message="Upload a lab report, prescription, or scan from Home to get started."
            actionLabel="Go to Home"
            onAction={() => router.push("/(app)" as never)}
          />
        </FadeInView>
      )}

      {reports.map((report, index) => (
        <FadeInView key={report.reportId} delay={index * 50}>
          <Card>
            <AnimatedPressable
              onPress={() => router.push(`/(app)/report/${report.reportId}` as never)}
            >
              <View style={styles.cardHeader}>
                <View style={styles.fileIcon}>
                  <Icon name={documentIcon(report.documentType)} size="md" color={colors.primary} />
                </View>
                <View style={styles.cardInfo}>
                  <Text style={styles.filename} numberOfLines={1}>{report.filename}</Text>
                  <Text style={styles.meta}>
                    {documentTypeBadgeLabel(report.documentType)}
                    {" · "}
                    {report.personName}
                    {" · "}
                    {formatDate(report.uploadedAt)}
                  </Text>
                </View>
                <Badge
                  label={extractionStatusBadgeLabel(report.extractionStatus)}
                  variant={extractionStatusVariant(report.extractionStatus)}
                />
              </View>
            </AnimatedPressable>
            <AnimatedPressable style={styles.deleteBtn} onPress={() => confirmDelete(report)}>
              <Icon name="trash-outline" size="sm" color={colors.danger} />
              <Text style={styles.deleteText}>Delete</Text>
            </AnimatedPressable>
          </Card>
        </FadeInView>
      ))}
    </Screen>
  );
}

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
  } catch {
    return iso;
  }
}
