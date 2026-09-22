import { router } from "expo-router";
import { useCallback, useEffect, useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import { OpsBarChart } from "../../components/charts/OpsBarChart";
import {
  AuditEventView,
  ExtractionEventView,
  fetchOpsAudit,
  fetchOpsErrors,
  fetchOpsEvents,
  fetchOpsReports,
  fetchOpsSummary,
  fetchOpsTimeseries,
  fetchOpsFeedback,
  fetchMe,
  FeedbackView,
  OpsSummary,
  OpsTimeseries,
  ReportOpsRow,
  updateFeedbackStatus,
} from "../../lib/api";
import { AuthSession } from "../../lib/auth";
import {
  eventStatusColor,
  formatAuditAction,
  formatEventType,
  formatTokenCount,
} from "../../lib/ops";
import { colors, radii, spacing, typography } from "../../lib/theme";
import { AnimatedPressable } from "../../components/ui/AnimatedPressable";
import { Badge } from "../../components/ui/Badge";
import { Card } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { FadeInView } from "../../components/ui/FadeInView";
import { Icon } from "../../components/ui/Icon";
import { Screen } from "../../components/ui/Screen";
import { Skeleton } from "../../components/ui/Skeleton";

function formatTime(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function StatCard({ label, value, hint, accent }: { label: string; value: string; hint?: string; accent?: string }) {
  return (
    <View style={[styles.statCard, accent ? { borderColor: accent } : null]}>
      <Text style={styles.statLabel}>{label}</Text>
      <Text style={[styles.statValue, accent ? { color: accent } : null]}>{value}</Text>
      {hint ? <Text style={styles.statHint}>{hint}</Text> : null}
    </View>
  );
}

function EventRow({ event, onPress }: { event: ExtractionEventView; onPress?: () => void }) {
  const statusColor = eventStatusColor(event.status);
  const tokens =
    event.inputTokens != null || event.outputTokens != null
      ? `${event.inputTokens ?? 0} in / ${event.outputTokens ?? 0} out`
      : null;

  return (
    <AnimatedPressable style={styles.eventRow} onPress={onPress} disabled={!onPress}>
      <View style={[styles.eventDot, { backgroundColor: statusColor }]} />
      <View style={styles.eventBody}>
        <View style={styles.eventHeader}>
          <Text style={styles.eventType}>{formatEventType(event.eventType)}</Text>
          <Text style={styles.eventTime}>{formatTime(event.createdAt)}</Text>
        </View>
        {event.message ? <Text style={styles.eventMessage}>{event.message}</Text> : null}
        <View style={styles.eventMeta}>
          <Text style={styles.eventMetaText}>Report #{event.reportId}</Text>
          {event.charCount != null && <Text style={styles.eventMetaText}>{event.charCount} chars</Text>}
          {event.coverage != null && (
            <Text style={styles.eventMetaText}>{Math.round(event.coverage * 100)}% rules</Text>
          )}
          {tokens && <Text style={styles.eventMetaText}>{tokens} tokens</Text>}
        </View>
      </View>
      {onPress ? <Icon name="chevron-forward" size="sm" color={colors.textSoft} /> : null}
    </AnimatedPressable>
  );
}

export default function OpsScreen() {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [summary, setSummary] = useState<OpsSummary | null>(null);
  const [reports, setReports] = useState<ReportOpsRow[]>([]);
  const [events, setEvents] = useState<ExtractionEventView[]>([]);
  const [errors, setErrors] = useState<ExtractionEventView[]>([]);
  const [audit, setAudit] = useState<AuditEventView[]>([]);
  const [feedback, setFeedback] = useState<FeedbackView[]>([]);
  const [timeseries, setTimeseries] = useState<OpsTimeseries | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    const me = await fetchMe();
    setSession(me);
    if (!me.platformAdmin) {
      setError("Platform operator access only — testers do not see this console.");
      setLoading(false);
      return;
    }

    const [sum, series, rpt, ev, err, aud, fb] = await Promise.all([
      fetchOpsSummary(7),
      fetchOpsTimeseries(7),
      fetchOpsReports(30),
      fetchOpsEvents(30),
      fetchOpsErrors(7, 10),
      fetchOpsAudit(15),
      fetchOpsFeedback(),
    ]);
    setSummary(sum);
    setTimeseries(series);
    setReports(rpt);
    setEvents(ev);
    setErrors(err);
    setAudit(aud);
    setFeedback(fb);
    setError(null);
  }, []);

  useEffect(() => {
    setLoading(true);
    load()
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load ops"))
      .finally(() => setLoading(false));
  }, [load]);

  async function onRefresh() {
    setRefreshing(true);
    try {
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Refresh failed");
    } finally {
      setRefreshing(false);
    }
  }

  if (loading && !summary) {
    return (
      <Screen>
        <Skeleton height={120} style={{ marginBottom: spacing.md }} />
        <Skeleton height={200} />
      </Screen>
    );
  }

  if (error && !summary) {
    return (
      <Screen scroll={false}>
        <EmptyState
          icon="stats-chart-outline"
          title="Platform ops"
          message={error}
        />
      </Screen>
    );
  }

  const totalTokens = (summary?.totalInputTokens ?? 0) + (summary?.totalOutputTokens ?? 0);

  return (
    <Screen refreshing={refreshing} onRefresh={onRefresh}>
      <FadeInView delay={0}>
        <Text style={styles.lead}>
          App-wide console — all testers, all uploads, pipeline steps, and token spend (last 7 days).
        </Text>
      </FadeInView>

      {timeseries && timeseries.buckets.length > 0 && (
        <FadeInView delay={30}>
          <Card>
            <View style={styles.sectionHeader}>
              <Icon name="bar-chart-outline" size="md" color={colors.primary} />
              <Text style={styles.sectionTitle}>Activity (7 days)</Text>
            </View>
            <OpsBarChart
              title="AI tokens per day"
              data={timeseries.buckets.map((b) => ({ label: b.date, value: b.totalTokens }))}
              color={colors.warning}
            />
            <OpsBarChart
              title="Uploads per day"
              data={timeseries.buckets.map((b) => ({ label: b.date, value: b.uploads }))}
              color={colors.primary}
            />
            <OpsBarChart
              title="Pipeline failures per day"
              data={timeseries.buckets.map((b) => ({ label: b.date, value: b.failures }))}
              color={colors.danger}
            />
          </Card>
        </FadeInView>
      )}

      {summary && (
        <FadeInView delay={40}>
          <View style={styles.statGrid}>
            <StatCard
              label="AI tokens"
              value={formatTokenCount(totalTokens)}
              hint={`${formatTokenCount(summary.totalInputTokens)} in · ${formatTokenCount(summary.totalOutputTokens)} out`}
              accent={summary.aiParseCalls + summary.aiVisionCalls > 0 ? colors.warning : undefined}
            />
            <StatCard
              label="Claude parses"
              value={String(summary.aiParseCalls)}
              hint="Last-resort biomarker parse"
            />
            <StatCard
              label="Vision calls"
              value={String(summary.aiVisionCalls)}
              hint="Last-resort text OCR"
            />
            <StatCard
              label="Failures"
              value={String(summary.failures)}
              hint={`${summary.ruleSuccesses} rules-only wins`}
              accent={summary.failures > 0 ? colors.danger : colors.success}
            />
          </View>
        </FadeInView>
      )}

      <FadeInView delay={80}>
        <Card>
          <View style={styles.sectionHeader}>
            <Icon name="cloud-upload-outline" size="md" color={colors.primary} />
            <Text style={styles.sectionTitle}>Recent uploads (all users)</Text>
          </View>
          {reports.length === 0 ? (
            <Text style={styles.emptyHint}>No uploads yet.</Text>
          ) : (
            reports.map((report) => (
              <AnimatedPressable
                key={report.reportId}
                style={styles.reportRow}
                onPress={() => router.push(`/(app)/ops-pipeline/${report.reportId}` as never)}
              >
                <View style={styles.reportBody}>
                  <Text style={styles.reportName}>{report.personName}</Text>
                  <Text style={styles.reportFile}>{report.filename}</Text>
                  <View style={styles.reportMeta}>
                    <Badge
                      label={report.extractionStatus}
                      variant={report.extractionStatus === "COMPLETED" ? "success" : "default"}
                    />
                    <Text style={styles.eventMetaText}>#{report.reportId}</Text>
                  </View>
                  {report.extractionError && (
                    <Text style={styles.reportError}>{report.extractionError}</Text>
                  )}
                </View>
                <Icon name="chevron-forward" size="sm" color={colors.textSoft} />
              </AnimatedPressable>
            ))
          )}
        </Card>
      </FadeInView>

      {errors.length > 0 && (
        <FadeInView delay={80}>
          <Card>
            <View style={styles.sectionHeader}>
              <Icon name="alert-circle-outline" size="md" color={colors.danger} />
              <Text style={styles.sectionTitle}>Recent failures</Text>
            </View>
            {errors.map((event) => (
              <EventRow
                key={event.id}
                event={event}
                onPress={() => router.push(`/(app)/ops-pipeline/${event.reportId}` as never)}
              />
            ))}
          </Card>
        </FadeInView>
      )}

      <FadeInView delay={120}>
        <Card>
          <View style={styles.sectionHeader}>
            <Icon name="git-network-outline" size="md" color={colors.primary} />
            <Text style={styles.sectionTitle}>Pipeline events</Text>
          </View>
          {events.length === 0 ? (
            <Text style={styles.emptyHint}>Upload a report to see extraction steps here.</Text>
          ) : (
            events.map((event) => (
              <EventRow
                key={event.id}
                event={event}
                onPress={() => router.push(`/(app)/ops-pipeline/${event.reportId}` as never)}
              />
            ))
          )}
        </Card>
      </FadeInView>

      <FadeInView delay={140}>
        <Card>
          <View style={styles.sectionHeader}>
            <Icon name="chatbox-ellipses-outline" size="md" color={colors.primary} />
            <Text style={styles.sectionTitle}>Tester feedback</Text>
          </View>
          {feedback.length === 0 ? (
            <Text style={styles.emptyHint}>No feedback yet — share the Send feedback screen with testers.</Text>
          ) : (
            feedback.map((item) => (
              <View key={item.id} style={styles.feedbackRow}>
                <View style={styles.feedbackHeader}>
                  <Badge label={item.category} variant={item.category === "bug" ? "danger" : "default"} />
                  <Badge label={item.status} variant={item.status === "pending" ? "warning" : "default"} />
                  <Text style={styles.eventTime}>{formatTime(item.createdAt)}</Text>
                </View>
                <Text style={styles.feedbackUser}>
                  {item.submitterName} · {item.submitterEmail}
                  {item.rating ? ` · ${item.rating}/5` : ""}
                </Text>
                <Text style={styles.feedbackMessage}>{item.message}</Text>
                {item.screenContext || item.appPlatform ? (
                  <Text style={styles.eventMetaText}>
                    {[item.screenContext, item.appPlatform, item.appVersion].filter(Boolean).join(" · ")}
                  </Text>
                ) : null}
                {item.status !== "resolved" ? (
                  <View style={styles.feedbackActions}>
                    {item.status === "pending" ? (
                      <AnimatedPressable
                        style={styles.feedbackAction}
                        onPress={() =>
                          updateFeedbackStatus(item.id, "read").then((updated) =>
                            setFeedback((rows) => rows.map((row) => (row.id === updated.id ? updated : row)))
                          )
                        }
                      >
                        <Text style={styles.feedbackActionText}>Mark read</Text>
                      </AnimatedPressable>
                    ) : null}
                    <AnimatedPressable
                      style={styles.feedbackAction}
                      onPress={() =>
                        updateFeedbackStatus(item.id, "resolved").then((updated) =>
                          setFeedback((rows) => rows.map((row) => (row.id === updated.id ? updated : row)))
                        )
                      }
                    >
                      <Text style={styles.feedbackActionText}>Resolve</Text>
                    </AnimatedPressable>
                  </View>
                ) : null}
              </View>
            ))
          )}
        </Card>
      </FadeInView>

      <FadeInView delay={160}>
        <Card>
          <View style={styles.sectionHeader}>
            <Icon name="shield-checkmark-outline" size="md" color={colors.primary} />
            <Text style={styles.sectionTitle}>Audit log</Text>
          </View>
          {audit.length === 0 ? (
            <Text style={styles.emptyHint}>No audit events yet.</Text>
          ) : (
            audit.map((item) => (
              <View key={item.id} style={styles.auditRow}>
                <Badge label={formatAuditAction(item.action)} variant="default" />
                <Text style={styles.auditTime}>{formatTime(item.createdAt)}</Text>
                {item.metadata ? <Text style={styles.auditMeta}>{item.metadata}</Text> : null}
              </View>
            ))
          )}
        </Card>
      </FadeInView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  lead: { ...typography.body, marginBottom: spacing.md },
  statGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: spacing.sm,
    marginBottom: spacing.md,
  },
  statCard: {
    flexBasis: "47%",
    flexGrow: 1,
    backgroundColor: colors.surface,
    borderRadius: radii.md,
    padding: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
  },
  statLabel: { ...typography.label, marginBottom: 4 },
  statValue: { ...typography.data, fontSize: 22 },
  statHint: { ...typography.caption, marginTop: 4 },
  sectionHeader: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.sm,
    marginBottom: spacing.sm,
  },
  sectionTitle: { ...typography.subtitle, fontSize: 16 },
  eventRow: {
    flexDirection: "row",
    alignItems: "flex-start",
    gap: spacing.sm,
    paddingVertical: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.borderLight,
  },
  eventDot: { width: 8, height: 8, borderRadius: 4, marginTop: 6 },
  eventBody: { flex: 1 },
  eventHeader: { flexDirection: "row", justifyContent: "space-between", gap: spacing.sm },
  eventType: { ...typography.subtitle, fontSize: 14, flex: 1 },
  eventTime: { ...typography.caption, fontSize: 11 },
  eventMessage: { ...typography.caption, marginTop: 2, lineHeight: 18 },
  eventMeta: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm, marginTop: 4 },
  eventMetaText: { ...typography.caption, fontSize: 11, color: colors.textSoft },
  emptyHint: { ...typography.caption, paddingVertical: spacing.sm },
  reportRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.sm,
    paddingVertical: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.borderLight,
  },
  reportBody: { flex: 1, gap: 2 },
  reportName: { ...typography.subtitle, fontSize: 14 },
  reportFile: { ...typography.caption },
  reportMeta: { flexDirection: "row", alignItems: "center", gap: spacing.sm, marginTop: 4 },
  reportError: { ...typography.caption, color: colors.danger, marginTop: 4 },
  auditRow: {
    paddingVertical: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.borderLight,
    gap: 4,
  },
  auditTime: { ...typography.caption, fontSize: 11 },
  auditMeta: { ...typography.caption, fontFamily: "monospace", fontSize: 11 },
  feedbackRow: {
    paddingVertical: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.borderLight,
    gap: 4,
  },
  feedbackHeader: {
    flexDirection: "row",
    flexWrap: "wrap",
    alignItems: "center",
    gap: spacing.sm,
  },
  feedbackUser: { ...typography.caption, color: colors.textSoft },
  feedbackMessage: { ...typography.body, lineHeight: 22 },
  feedbackActions: { flexDirection: "row", gap: spacing.md, marginTop: 4 },
  feedbackAction: { paddingVertical: 4 },
  feedbackActionText: { ...typography.caption, color: colors.primaryDark, fontWeight: "600" },
});
