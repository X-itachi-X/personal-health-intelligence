import { useLocalSearchParams } from "expo-router";
import { useEffect, useMemo, useState } from "react";
import { Alert, Platform, StyleSheet, Text, View } from "react-native";
import {
  downloadOpsReportFile,
  fetchFreeToolComparison,
  fetchReportPipeline,
  FreeToolComparisonView,
  FreeToolResult,
  ReportPipelineView,
} from "../../../lib/api";
import {
  eventStatusColor,
  formatDuration,
  formatEventType,
  formatFileSize,
  formatTokenCount,
  isFreeToolEvent,
} from "../../../lib/ops";
import { FREE_TOOL_STEPS } from "../../../lib/extraction";
import { colors, radii, spacing, typography } from "../../../lib/theme";
import { Badge } from "../../../components/ui/Badge";
import { Button } from "../../../components/ui/Button";
import { Card } from "../../../components/ui/Card";
import { FadeInView } from "../../../components/ui/FadeInView";
import { Icon } from "../../../components/ui/Icon";
import { ProgressStep, ProgressSteps } from "../../../components/ui/ProgressSteps";
import { Screen } from "../../../components/ui/Screen";
import { SkeletonCard } from "../../../components/ui/Skeleton";

function formatTime(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

function FreeToolCard({ tool }: { tool: FreeToolResult }) {
  const color = eventStatusColor(tool.status);
  const duration = formatDuration(tool.durationMs);
  const sourceLabel = tool.sourceToolId ? `from ${formatEventType(tool.sourceToolId)}` : null;

  return (
    <View style={styles.freeToolCard}>
      <View style={styles.freeToolHeader}>
        <View style={[styles.freeToolDot, { backgroundColor: color }]} />
        <View style={styles.freeToolTitleBlock}>
          <Text style={styles.freeToolTitle}>{tool.label}</Text>
          {sourceLabel ? <Text style={styles.freeToolSource}>{sourceLabel}</Text> : null}
        </View>
        <Badge label={tool.status} variant={tool.status === "success" ? "success" : "default"} />
      </View>
      {tool.error ? <Text style={styles.errorInline}>{tool.error}</Text> : null}
      <View style={styles.stepDetails}>
        {duration && <Text style={styles.stepDetail}>{duration}</Text>}
        {tool.charCount != null && <Text style={styles.stepDetail}>{tool.charCount} chars</Text>}
        {tool.biomarkerCount != null && <Text style={styles.stepDetail}>{tool.biomarkerCount} biomarkers</Text>}
        {tool.coverage != null && (
          <Text style={styles.stepDetail}>{Math.round(tool.coverage * 100)}% coverage</Text>
        )}
        {tool.labFormat && <Text style={styles.stepDetail}>{tool.labFormat}</Text>}
      </View>
      {tool.text ? (
        <View style={styles.previewBox}>
          <Text style={styles.previewLabel}>
            Extracted text{tool.charCount != null ? ` (${tool.charCount.toLocaleString()} chars)` : ""}
          </Text>
          <Text style={styles.previewText} selectable>
            {tool.text}
          </Text>
        </View>
      ) : null}
      {tool.biomarkers.length > 0 ? (
        <View style={styles.previewBox}>
          <Text style={styles.previewLabel}>Biomarkers found</Text>
          {tool.biomarkers.map((marker, index) => (
            <Text key={`${marker.testName}-${index}`} style={styles.biomarkerLine} selectable>
              {marker.testName}: {marker.value ?? "—"} {marker.unit ?? ""}
            </Text>
          ))}
        </View>
      ) : null}
    </View>
  );
}

export default function OpsPipelineScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const [pipeline, setPipeline] = useState<ReportPipelineView | null>(null);
  const [comparison, setComparison] = useState<FreeToolComparisonView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [compareLoading, setCompareLoading] = useState(false);
  const [compareStartedAt, setCompareStartedAt] = useState<number | null>(null);
  const [compareElapsedSec, setCompareElapsedSec] = useState(0);
  const [downloadLoading, setDownloadLoading] = useState(false);

  useEffect(() => {
    const reportId = Number(id);
    if (!reportId) return;
    fetchReportPipeline(reportId)
      .then(setPipeline)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load"));
    fetchFreeToolComparison(reportId, false)
      .then(setComparison)
      .catch(() => undefined);
  }, [id]);

  useEffect(() => {
    if (!compareLoading || compareStartedAt == null) {
      return;
    }
    const timer = setInterval(() => {
      setCompareElapsedSec(Math.floor((Date.now() - compareStartedAt) / 1000));
    }, 1000);
    return () => clearInterval(timer);
  }, [compareLoading, compareStartedAt]);

  const compareSteps = useMemo((): ProgressStep[] => {
    if (!compareLoading) {
      return [];
    }
    const activeIndex = Math.min(
      Math.floor(compareElapsedSec / 8),
      FREE_TOOL_STEPS.length - 1
    );
    return FREE_TOOL_STEPS.map((label, index) => ({
      id: `tool-${index}`,
      label,
      state: (index < activeIndex ? "done" : index === activeIndex ? "active" : "pending") as ProgressStep["state"],
    }));
  }, [compareLoading, compareElapsedSec]);

  async function runFreeToolComparison() {
    const reportId = Number(id);
    if (!reportId) return;
    setCompareLoading(true);
    setCompareStartedAt(Date.now());
    setCompareElapsedSec(0);
    setError(null);
    try {
      const result = await fetchFreeToolComparison(reportId, true);
      setComparison(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Comparison failed");
    } finally {
      setCompareLoading(false);
      setCompareStartedAt(null);
    }
  }

  async function downloadOriginalFile() {
    if (!pipeline) return;
    setDownloadLoading(true);
    setError(null);
    try {
      await downloadOpsReportFile(pipeline.reportId, pipeline.filename);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Download failed";
      if (Platform.OS !== "web") {
        Alert.alert("Download", message);
      } else {
        setError(message);
      }
    } finally {
      setDownloadLoading(false);
    }
  }

  if (error) {
    return (
      <Screen scroll={false}>
        <Card variant="outlined">
          <Text style={styles.error}>{error}</Text>
        </Card>
      </Screen>
    );
  }

  if (!pipeline) {
    return (
      <Screen>
        <SkeletonCard />
        <SkeletonCard />
      </Screen>
    );
  }

  const totalTokens = pipeline.totalInputTokens + pipeline.totalOutputTokens;

  return (
    <Screen>
      <FadeInView delay={0}>
        <View style={styles.header}>
          <Text style={styles.filename}>{pipeline.filename}</Text>
          <View style={styles.headerMeta}>
            <Badge
              label={pipeline.extractionStatus}
              variant={pipeline.extractionStatus === "COMPLETED" ? "success" : "default"}
            />
            <Text style={styles.reportId}>Report #{pipeline.reportId}</Text>
          </View>
          {pipeline.extractionError && <Text style={styles.error}>{pipeline.extractionError}</Text>}
        </View>
      </FadeInView>

      <FadeInView delay={40}>
        <Card>
          <View style={styles.toolsHeader}>
            <Icon name="document-attach-outline" size="md" color={colors.primary} />
            <Text style={styles.timelineTitle}>Original upload</Text>
          </View>
          {pipeline.originalFileAvailable ? (
            <>
              <Text style={styles.textMeta}>
                {pipeline.originalContentType ?? "file"} · {formatFileSize(pipeline.originalFileSizeBytes)}
              </Text>
              <Button
                variant="secondary"
                onPress={downloadOriginalFile}
                loading={downloadLoading}
                style={styles.actionButton}
              >
                Download original file
              </Button>
            </>
          ) : (
            <Text style={styles.empty}>
              File purged after text extraction. Set PHI_RETAIN_FILES_DAYS to 7+ during tester phase to keep uploads.
            </Text>
          )}
        </Card>
      </FadeInView>

      <FadeInView delay={50}>
        <Card>
          <View style={styles.toolsHeader}>
            <Icon name="git-compare-outline" size="md" color={colors.primary} />
            <Text style={styles.timelineTitle}>Compare free tools</Text>
          </View>
          <Text style={styles.textMeta}>
            Runs every free tool on the same file: PDFBox text layer, Tesseract OCR (all pages), and rules parse for
            each. No Claude. Large PDFs can take several minutes — results are saved on the server.
          </Text>
          <Button
            variant="secondary"
            onPress={runFreeToolComparison}
            loading={compareLoading}
            style={styles.actionButton}
          >
            Run free-tool comparison
          </Button>
          {compareLoading && (
            <Card variant="muted" style={styles.progressCard}>
              <ProgressSteps
                steps={compareSteps}
                caption={`Running on server… ${compareElapsedSec}s elapsed. Scanned PDFs can take 1–3 minutes.`}
              />
            </Card>
          )}
          {comparison?.fileUnavailableReason && !comparison.originalFileAvailable ? (
            <Text style={styles.warning}>{comparison.fileUnavailableReason}</Text>
          ) : null}
          {comparison && comparison.tools.length > 0 && (
            <Text style={styles.cachedNote}>
              {compareLoading ? "Refreshing comparison…" : "Saved on server — results persist when you leave this screen."}
            </Text>
          )}
          {comparison?.tools.map((tool) => <FreeToolCard key={`${tool.toolId}-${tool.sourceToolId ?? "root"}`} tool={tool} />)}
        </Card>
      </FadeInView>

      <FadeInView delay={60}>
        <Card>
          <View style={styles.toolsHeader}>
            <Icon name="construct-outline" size="md" color={colors.primary} />
            <Text style={styles.timelineTitle}>Tools used (production run)</Text>
          </View>
          <View style={styles.toolRow}>
            <Text style={styles.toolLabel}>Text extraction</Text>
            <Text style={styles.toolValue}>{pipeline.textExtractionTool}</Text>
          </View>
          <View style={styles.toolRow}>
            <Text style={styles.toolLabel}>Biomarker parse</Text>
            <Text style={styles.toolValue}>{pipeline.biomarkerParseTool}</Text>
          </View>
        </Card>
      </FadeInView>

      <FadeInView delay={90}>
        <Card>
          <View style={styles.toolsHeader}>
            <Icon name="document-text-outline" size="md" color={colors.primary} />
            <Text style={styles.timelineTitle}>Extracted text</Text>
          </View>
          <Text style={styles.textMeta}>
            {pipeline.extractedTextLength > 0
              ? `${pipeline.extractedTextLength.toLocaleString()} characters saved to database`
              : "No text stored yet"}
          </Text>
          {pipeline.extractedTextLength > 0 ? (
            <View style={styles.textBox}>
              <Text style={styles.extractedText} selectable>
                {pipeline.extractedText}
              </Text>
            </View>
          ) : (
            <Text style={styles.empty}>Text appears after Phase 1 extraction completes.</Text>
          )}
        </Card>
      </FadeInView>

      <FadeInView delay={120}>
        <Card>
          <Text style={styles.summaryLabel}>Token usage (this report)</Text>
          <Text style={styles.tokenTotal}>{formatTokenCount(totalTokens)} total</Text>
          <Text style={styles.tokenDetail}>
            {formatTokenCount(pipeline.totalInputTokens)} input · {formatTokenCount(pipeline.totalOutputTokens)} output
          </Text>
        </Card>
      </FadeInView>

      <FadeInView delay={150}>
        <Card>
          <View style={styles.timelineHeader}>
            <Icon name="list-outline" size="md" color={colors.primary} />
            <Text style={styles.timelineTitle}>Pipeline timeline</Text>
          </View>
          {pipeline.events.length === 0 ? (
            <Text style={styles.empty}>No telemetry events yet — upload may predate monitoring.</Text>
          ) : (
            pipeline.events
              .filter((event) => isFreeToolEvent(event.eventType))
              .map((event, index, freeEvents) => {
              const color = eventStatusColor(event.status);
              const duration = formatDuration(event.durationMs);
              return (
                <View key={event.id} style={styles.step}>
                  <View style={styles.stepRail}>
                    <View style={[styles.stepDot, { backgroundColor: color }]} />
                    {index < freeEvents.length - 1 && <View style={styles.stepLine} />}
                  </View>
                  <View style={styles.stepContent}>
                    <View style={styles.stepTitleRow}>
                      <Text style={styles.stepTitle}>{formatEventType(event.eventType)}</Text>
                      <Text style={styles.stepTime}>{formatTime(event.createdAt)}</Text>
                    </View>
                    {event.message ? <Text style={styles.stepMessage}>{event.message}</Text> : null}
                    {event.textPreview ? (
                      <View style={styles.previewBox}>
                        <Text style={styles.previewLabel}>Text from this step</Text>
                        <Text style={styles.previewText} selectable numberOfLines={8}>
                          {event.textPreview}
                        </Text>
                      </View>
                    ) : null}
                    <View style={styles.stepDetails}>
                      {duration && <Text style={styles.stepDetail}>{duration}</Text>}
                      {event.charCount != null && <Text style={styles.stepDetail}>{event.charCount} chars</Text>}
                      {event.biomarkerCount != null && (
                        <Text style={styles.stepDetail}>{event.biomarkerCount} biomarkers</Text>
                      )}
                      {event.coverage != null && (
                        <Text style={styles.stepDetail}>{Math.round(event.coverage * 100)}% coverage</Text>
                      )}
                      {(event.inputTokens != null || event.outputTokens != null) && (
                        <Text style={[styles.stepDetail, styles.tokenDetailStep]}>
                          {event.inputTokens ?? 0}↓ {event.outputTokens ?? 0}↑ tokens
                        </Text>
                      )}
                      {event.model && <Text style={styles.stepDetail}>{event.model}</Text>}
                    </View>
                  </View>
                </View>
              );
            })
          )}
        </Card>
      </FadeInView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { marginBottom: spacing.sm },
  filename: { ...typography.title, fontSize: 18 },
  headerMeta: { flexDirection: "row", alignItems: "center", gap: spacing.sm, marginTop: spacing.sm },
  reportId: { ...typography.caption },
  error: { ...typography.body, color: colors.danger, marginTop: spacing.sm },
  toolsHeader: { flexDirection: "row", alignItems: "center", gap: spacing.sm, marginBottom: spacing.sm },
  toolRow: { marginTop: spacing.sm, gap: 4 },
  toolLabel: { ...typography.label },
  toolValue: { ...typography.subtitle, fontSize: 14, lineHeight: 20 },
  actionButton: { marginTop: spacing.sm },
  progressCard: { marginTop: spacing.md, padding: spacing.md },
  cachedNote: { ...typography.caption, color: colors.success, marginTop: spacing.sm },
  warning: { ...typography.caption, color: colors.warning, marginTop: spacing.sm, lineHeight: 18 },
  errorInline: { ...typography.caption, color: colors.danger, marginTop: spacing.xs },
  freeToolCard: {
    marginTop: spacing.md,
    paddingTop: spacing.md,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.border,
  },
  freeToolHeader: { flexDirection: "row", alignItems: "flex-start", gap: spacing.sm },
  freeToolDot: { width: 8, height: 8, borderRadius: 4, marginTop: 6 },
  freeToolTitleBlock: { flex: 1 },
  freeToolTitle: { ...typography.subtitle, fontSize: 14 },
  freeToolSource: { ...typography.caption, marginTop: 2 },
  biomarkerLine: {
    fontSize: 12,
    lineHeight: 18,
    color: colors.text,
    fontFamily: Platform.select({ ios: "Menlo", android: "monospace", default: "monospace" }),
  },
  textMeta: { ...typography.caption, marginBottom: spacing.sm },
  textBox: {
    backgroundColor: colors.surfaceMuted,
    borderRadius: radii.md,
    padding: spacing.md,
    maxHeight: 320,
  },
  extractedText: {
    fontSize: 12,
    lineHeight: 18,
    color: colors.text,
    fontFamily: Platform.select({ ios: "Menlo", android: "monospace", default: "monospace" }),
  },
  previewBox: {
    marginTop: spacing.sm,
    backgroundColor: colors.surfaceMuted,
    borderRadius: radii.sm,
    padding: spacing.sm,
  },
  previewLabel: { ...typography.label, fontSize: 10, marginBottom: 4 },
  previewText: {
    fontSize: 11,
    lineHeight: 16,
    color: colors.textMuted,
    fontFamily: Platform.select({ ios: "Menlo", android: "monospace", default: "monospace" }),
  },
  summaryLabel: { ...typography.label },
  tokenTotal: { ...typography.data, fontSize: 28, marginTop: 4 },
  tokenDetail: { ...typography.caption, marginTop: 4 },
  timelineHeader: { flexDirection: "row", alignItems: "center", gap: spacing.sm, marginBottom: spacing.md },
  timelineTitle: { ...typography.subtitle, fontSize: 16 },
  empty: { ...typography.caption },
  step: { flexDirection: "row", gap: spacing.md },
  stepRail: { alignItems: "center", width: 16 },
  stepDot: { width: 10, height: 10, borderRadius: 5 },
  stepLine: { flex: 1, width: 2, backgroundColor: colors.border, marginVertical: 2 },
  stepContent: { flex: 1, paddingBottom: spacing.md },
  stepTitleRow: { flexDirection: "row", justifyContent: "space-between", gap: spacing.sm },
  stepTitle: { ...typography.subtitle, fontSize: 14, flex: 1 },
  stepTime: { ...typography.caption, fontSize: 11 },
  stepMessage: { ...typography.caption, marginTop: 4, lineHeight: 18 },
  stepDetails: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm, marginTop: 6 },
  stepDetail: {
    ...typography.caption,
    fontSize: 11,
    backgroundColor: colors.surfaceMuted,
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: radii.sm,
  },
  tokenDetailStep: { color: colors.warning },
});
