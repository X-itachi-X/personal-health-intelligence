import { router } from "expo-router";
import { useEffect, useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import * as DocumentPicker from "expo-document-picker";
import {
  Biomarker,
  FeedItem,
  fetchFamilyFeed,
  fetchHealth,
  fetchReport,
  fetchPersonChanges,
  fetchPersonInsights,
  fetchPersonReminders,
  RetestReminder,
  InsightCard,
  BiomarkerChange,
  listImagingStudies,
  listMedications,
  listReports,
  pollReportUntilDone,
  DuplicateReportError,
  DocumentType,
  submitReportDate,
  uploadReport,
} from "../../lib/api";
import { AuthSession, getSession } from "../../lib/auth";
import { hasAdvancedAccess } from "../../lib/session";
import {
  assessBiomarker,
  buildHealthSummary,
  formatBiomarkerName,
  statusColor,
} from "../../lib/biomarkers";
import { buildPersonHealthSummary } from "../../lib/healthSummary";
import { useFamily } from "../../lib/FamilyContext";
import {
  awaitingDatePhotoMessage,
  finishUploadMessage,
  parseStepLabel,
  photoDateRequiredHint,
  reportDatePickerLabel,
  reportDateRequiredCaption,
  uploadSheetTitle,
} from "../../lib/documentTypes";
import {
  extractionStatusBadgeLabel,
  formatExtractionStatus,
  uploadPhaseFromStatus,
  UploadPhase,
} from "../../lib/extraction";
import { isIsoDate } from "../../lib/reportDate";
import { colors, radii, spacing, typography } from "../../lib/theme";
import { AnimatedPressable } from "../../components/ui/AnimatedPressable";
import { BottomSheet } from "../../components/ui/BottomSheet";
import { Card } from "../../components/ui/Card";
import { FadeInView } from "../../components/ui/FadeInView";
import { Icon } from "../../components/ui/Icon";
import { Button } from "../../components/ui/Button";
import { ProgressSteps } from "../../components/ui/ProgressSteps";
import { ReportDatePicker } from "../../components/ui/ReportDatePicker";
import { Screen } from "../../components/ui/Screen";
import { Skeleton } from "../../components/ui/Skeleton";
import { ViewingPersonBanner } from "../../components/ViewingPersonBanner";

export default function HomeScreen() {
  const {
    activeFamily,
    members,
    effectivePersonId,
    isViewingOther,
    viewedPersonName,
    viewedMember,
    clearViewedPerson,
    viewedPersonId,
  } = useFamily();
  const [uploading, setUploading] = useState(false);
  const [uploadPhase, setUploadPhase] = useState<UploadPhase>("upload");
  const [status, setStatus] = useState<string | null>(null);
  const [backendStatus, setBackendStatus] = useState<"loading" | "ok" | "error">("loading");
  const [biomarkers, setBiomarkers] = useState<Biomarker[]>([]);
  const [session, setSession] = useState<AuthSession | null>(null);
  const [feed, setFeed] = useState<FeedItem[]>([]);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [healthSummary, setHealthSummary] = useState<string | null>(null);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [latestReportId, setLatestReportId] = useState<number | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [pendingAsset, setPendingAsset] = useState<DocumentPicker.DocumentPickerAsset | null>(null);
  const [pendingPersonId, setPendingPersonId] = useState<number | undefined>(undefined);
  const [pendingLabel, setPendingLabel] = useState("Myself");
  const [optionalReportDate, setOptionalReportDate] = useState("");
  const [awaitingDateReportId, setAwaitingDateReportId] = useState<number | null>(null);
  const [awaitingDateDocumentType, setAwaitingDateDocumentType] = useState<DocumentType>("LAB_REPORT");
  const [requiredReportDate, setRequiredReportDate] = useState("");
  const [submittingReportDate, setSubmittingReportDate] = useState(false);
  const [insightCards, setInsightCards] = useState<InsightCard[]>([]);
  const [recentChanges, setRecentChanges] = useState<BiomarkerChange[]>([]);
  const [insightsLoading, setInsightsLoading] = useState(false);
  const [retestReminders, setRetestReminders] = useState<RetestReminder[]>([]);
  const [remindersSummary, setRemindersSummary] = useState<string | null>(null);
  const [remindersLoading, setRemindersLoading] = useState(false);
  const [pendingDocumentType, setPendingDocumentType] = useState<DocumentType>("LAB_REPORT");

  useEffect(() => {
    getSession().then(setSession);
    fetchHealth()
      .then(() => setBackendStatus("ok"))
      .catch(() => setBackendStatus("error"));
  }, []);

  useEffect(() => {
    if (!activeFamily) {
      setFeed([]);
      return;
    }
    fetchFamilyFeed(activeFamily.id)
      .then(setFeed)
      .catch(() => setFeed([]));
  }, [activeFamily?.id]);

  const activePersonId = session?.personId ? effectivePersonId(session.personId) : null;
  const viewingOther = session?.personId ? isViewingOther(session.personId) : false;

  useEffect(() => {
    if (!activePersonId) {
      setHealthSummary(null);
      setLatestReportId(null);
      setInsightCards([]);
      setRecentChanges([]);
      setRetestReminders([]);
      setRemindersSummary(null);
      return;
    }

    setSummaryLoading(true);
    setInsightsLoading(true);
    setRemindersLoading(true);
    listReports(activeFamily?.id)
      .then(async (reports) => {
        const mine = reports
          .filter((r) => r.personId === activePersonId)
          .sort((a, b) => new Date(b.uploadedAt).getTime() - new Date(a.uploadedAt).getTime());

        const completedLabs = mine.filter(
          (r) =>
            (r.documentType ?? "LAB_REPORT") === "LAB_REPORT" &&
            (r.extractionStatus === "COMPLETED" || r.extractionStatus === "TEXT_ONLY")
        );

        const latest = mine[0] ?? null;
        setLatestReportId(latest?.reportId ?? null);

        const [latestLabDetail, medications, imagingStudies] = await Promise.all([
          completedLabs[0] ? fetchReport(completedLabs[0].reportId) : Promise.resolve(null),
          listMedications(activePersonId).catch(() => []),
          listImagingStudies(activePersonId).catch(() => []),
        ]);

        if (mine.length === 0 && medications.length === 0 && imagingStudies.length === 0) {
          setHealthSummary(null);
          setInsightCards([]);
          setRecentChanges([]);
          return;
        }

        setHealthSummary(
          buildPersonHealthSummary({
            latestLab: latestLabDetail,
            activeMedicationCount: medications.filter((med) => med.active).length,
            latestImaging: imagingStudies[0] ?? null,
          })
        );

        const [insights, changes, reminders] = await Promise.all([
          fetchPersonInsights(activePersonId).catch(() => ({ personId: activePersonId, cards: [] })),
          fetchPersonChanges(activePersonId).catch(() => null),
          fetchPersonReminders(activePersonId).catch(() => null),
        ]);
        setInsightCards(insights.cards);
        if (changes) {
          setRecentChanges(
            changes.changes
              .filter((c) => c.direction === "UP" || c.direction === "DOWN")
              .slice(0, 5)
          );
        } else {
          setRecentChanges([]);
        }
        if (reminders) {
          setRemindersSummary(reminders.summary);
          setRetestReminders(reminders.reminders);
        } else {
          setRemindersSummary(null);
          setRetestReminders([]);
        }
      })
      .catch(() => {
        setHealthSummary(null);
        setLatestReportId(null);
        setInsightCards([]);
        setRecentChanges([]);
        setRemindersSummary(null);
        setRetestReminders([]);
      })
      .finally(() => {
        setSummaryLoading(false);
        setInsightsLoading(false);
        setRemindersLoading(false);
      });
  }, [activePersonId, activeFamily?.id]);

  async function startUpload(personId?: number, label = "Myself", documentType: DocumentType = "LAB_REPORT") {
    setPendingDocumentType(documentType);
    setPickerOpen(false);

    const result = await DocumentPicker.getDocumentAsync({
      type: ["application/pdf", "image/*"],
      copyToCacheDirectory: true,
    });

    if (result.canceled || !result.assets?.[0]) return;

    setPendingAsset(result.assets[0]);
    setPendingPersonId(personId);
    setPendingLabel(label);
    setOptionalReportDate("");
    setConfirmOpen(true);
  }

  async function runUpload() {
    if (!pendingAsset) return;

    const asset = pendingAsset;
    const personId = pendingPersonId;
    const label = pendingLabel;
    const reportDate = optionalReportDate.trim();
    const isImage = asset.mimeType?.startsWith("image/") ?? false;

    if (isImage && !isIsoDate(reportDate)) {
      setStatus(
        pendingDocumentType === "PRESCRIPTION"
          ? "Photo uploads need the prescription date before we can continue."
          : "Photo uploads need the lab test date before we can continue."
      );
      return;
    }

    if (reportDate && !isIsoDate(reportDate)) {
      setStatus("Please choose a valid report date.");
      return;
    }

    setConfirmOpen(false);
    setUploading(true);
    setUploadPhase("upload");
    setStatus(`Uploading "${asset.name}" for ${label}…`);
    setBiomarkers([]);
    setAwaitingDateReportId(null);
    setRequiredReportDate("");

    try {
      const upload = await uploadReport(
        { uri: asset.uri, name: asset.name, mimeType: asset.mimeType, file: asset.file },
        {
          personId,
          familyId: activeFamily?.id,
          reportDate: reportDate || undefined,
          documentType: pendingDocumentType,
        }
      );

      setUploadPhase("extract");
      setStatus("Extracting text from PDF (local, no AI)…");

      let report: Awaited<ReturnType<typeof pollReportUntilDone>>;
      try {
        report = await pollReportUntilDone(upload.reportId, (update) => {
          const phase = uploadPhaseFromStatus(update.extractionStatus);
          setUploadPhase(phase);
          setStatus(formatExtractionStatus(update.extractionStatus));
        });
      } catch (pollError) {
        const partial = await fetchReport(upload.reportId);
        if (partial.extractionStatus === "TEXT_EXTRACTED") {
          setStatus("Text saved in database. Biomarker parsing not started (manual parse or auto-parse off).");
          setLatestReportId(upload.reportId);
          return;
        }
        throw pollError;
      }

      resolvePostUploadReport(report, pendingDocumentType, pendingAsset?.mimeType?.startsWith("image/") ?? false);
    } catch (error) {
      if (error instanceof DuplicateReportError) {
        if (error.existingExtractionStatus === "FAILED") {
          setStatus("Failed report found — reopening and retrying extraction…");
          setLatestReportId(error.existingReportId);
          router.push(`/(app)/report/${error.existingReportId}` as never);
        } else {
          setStatus(
            `Already uploaded as "${error.existingFilename}" (report #${error.existingReportId}). Open it from Reports.`
          );
          setLatestReportId(error.existingReportId);
        }
      } else {
        setStatus(error instanceof Error ? error.message : "Upload failed");
      }
    } finally {
      setUploading(false);
      setPendingAsset(null);
    }
  }

  function resolvePostUploadReport(
    report: Awaited<ReturnType<typeof pollReportUntilDone>>,
    documentType: DocumentType,
    isPhoto = false
  ) {
    if (
      report.extractionStatus === "AWAITING_MEDICATION_CONFIRMATION" ||
      report.extractionStatus === "AWAITING_IMAGING_CONFIRMATION"
    ) {
      setStatus(
        report.extractionStatus === "AWAITING_IMAGING_CONFIRMATION"
          ? "Review extracted imaging findings and confirm."
          : "Review extracted medications and confirm."
      );
      setLatestReportId(report.reportId);
      router.push(`/(app)/report/${report.reportId}` as never);
      return;
    }

    if (report.extractionStatus === "AWAITING_REPORT_DATE") {
      setAwaitingDateReportId(report.reportId);
      setAwaitingDateDocumentType(documentType);
      setLatestReportId(report.reportId);
      setRequiredReportDate("");
      setStatus(awaitingDatePhotoMessage(documentType, isPhoto));
      return;
    }

    finishSuccessfulUpload(report);
  }

  function finishSuccessfulUpload(report: Awaited<ReturnType<typeof pollReportUntilDone>>) {
    setStatus(finishUploadMessage(report));
    const isLab = (report.documentType ?? "LAB_REPORT") === "LAB_REPORT";
    if (isLab) {
      setBiomarkers(report.biomarkers.slice(0, 10));
      setHealthSummary(buildHealthSummary(report.biomarkers));
    } else {
      setBiomarkers([]);
      setHealthSummary(null);
    }
    setLatestReportId(report.reportId);
    if (activeFamily) {
      fetchFamilyFeed(activeFamily.id).then(setFeed).catch(() => undefined);
    }
  }

  async function handleSubmitRequiredReportDate() {
    if (!awaitingDateReportId || !isIsoDate(requiredReportDate)) {
      setStatus("Enter a valid report date (YYYY-MM-DD).");
      return;
    }

    setSubmittingReportDate(true);
    setUploading(true);
    setUploadPhase("parse");
    setStatus("Saving report date and continuing extraction…");

    try {
      await submitReportDate(awaitingDateReportId, requiredReportDate);
      const report = await pollReportUntilDone(awaitingDateReportId, (update) => {
        setUploadPhase(uploadPhaseFromStatus(update.extractionStatus));
        setStatus(formatExtractionStatus(update.extractionStatus));
      });
      setAwaitingDateReportId(null);
      setRequiredReportDate("");
      resolvePostUploadReport(report, awaitingDateDocumentType);
    } catch (error) {
      setStatus(error instanceof Error ? error.message : "Failed to save report date");
    } finally {
      setSubmittingReportDate(false);
      setUploading(false);
    }
  }

  function handleUploadPress(documentType: DocumentType = "LAB_REPORT") {
    if (viewingOther && viewedPersonId) {
      const member = members.find((m) => m.personId === viewedPersonId);
      startUpload(
        viewedPersonId,
        member?.displayName ?? viewedPersonName ?? "Family member",
        documentType
      );
      return;
    }
    if (hasAdvancedAccess(session) && members.length > 1) {
      setPendingDocumentType(documentType);
      setPickerOpen(true);
      return;
    }
    startUpload(undefined, "Myself", documentType);
  }

  const displayName = session?.displayName ?? "";
  const serverIcon = backendStatus === "ok" ? "checkmark-circle" : backendStatus === "error" ? "close-circle" : "ellipse";
  const serverColor = backendStatus === "ok" ? colors.success : backendStatus === "error" ? colors.danger : colors.warning;

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
        <View style={styles.hero}>
          <Text style={styles.greeting}>
            {viewingOther && viewedPersonName
              ? `${viewedPersonName.split(" ")[0]}'s health`
              : `Hello${displayName ? `, ${displayName.split(" ")[0]}` : ""}`}
          </Text>
          {activeFamily && (
            <View style={styles.familyChip}>
              <Icon name="people-outline" size="sm" color={colors.primaryDark} />
              <Text style={styles.familyLabel}>{activeFamily.displayName}</Text>
            </View>
          )}
          <Text style={styles.heroText}>
            {viewingOther
              ? "Upload labs, prescriptions, or scans for this family member."
              : "Upload a lab report to see your biomarkers in plain language."}
          </Text>
        </View>
      </FadeInView>

      <FadeInView delay={60}>
        <AnimatedPressable
          style={[styles.uploadCard, uploading && styles.uploadCardDisabled]}
          onPress={() => handleUploadPress("LAB_REPORT")}
          disabled={uploading}
        >
          <View style={styles.uploadIconWrap}>
            <Icon name="cloud-upload-outline" size="xl" color={colors.primary} />
          </View>
          <Text style={styles.uploadTitle}>{uploading ? "Processing your report" : "Upload lab report"}</Text>
          {uploading ? (
            <ProgressSteps
              steps={[
                {
                  id: "upload",
                  label: "Upload file",
                  state: uploadPhase === "upload" ? "active" : "done",
                },
                {
                  id: "extract",
                  label: "Extract text (PDFBox / OCR)",
                  state:
                    uploadPhase === "extract"
                      ? "active"
                      : uploadPhase === "upload"
                        ? "pending"
                        : "done",
                },
                {
                  id: "parse",
                  label: parseStepLabel(pendingDocumentType),
                  state: uploadPhase === "parse" ? "active" : "pending",
                },
              ]}
              caption={status}
            />
          ) : (
            <Text style={styles.uploadHint}>
              {hasAdvancedAccess(session) ? "Tap to choose whose report" : "PDF or image from your files"}
            </Text>
          )}
        </AnimatedPressable>
      </FadeInView>

      <FadeInView delay={75}>
        <AnimatedPressable
          style={[styles.rxCard, uploading && styles.uploadCardDisabled]}
          onPress={() => handleUploadPress("PRESCRIPTION")}
          disabled={uploading}
        >
          <Icon name="medkit-outline" size="lg" color={colors.primaryDark} />
          <View style={styles.rxCopy}>
            <Text style={styles.rxTitle}>Upload prescription</Text>
            <Text style={styles.rxHint}>Extract medications automatically — no typing names one by one</Text>
          </View>
          <Icon name="chevron-forward" size="sm" color={colors.textSoft} />
        </AnimatedPressable>
      </FadeInView>

      <FadeInView delay={90}>
        <AnimatedPressable
          style={[styles.rxCard, uploading && styles.uploadCardDisabled]}
          onPress={() => handleUploadPress("IMAGING_REPORT")}
          disabled={uploading}
        >
          <Icon name="scan-outline" size="lg" color={colors.primaryDark} />
          <View style={styles.rxCopy}>
            <Text style={styles.rxTitle}>Upload scan report</Text>
            <Text style={styles.rxHint}>USG, X-ray, MRI, CT — extract impression and findings for clearer context</Text>
          </View>
          <Icon name="chevron-forward" size="sm" color={colors.textSoft} />
        </AnimatedPressable>
      </FadeInView>

      {status && (
        <FadeInView delay={0}>
          <Card variant="muted" style={styles.statusCard}>
            <View style={styles.statusRow}>
              <Icon name="information-circle-outline" size="sm" color={colors.primaryDark} />
              <Text style={styles.statusText}>{status}</Text>
            </View>
          </Card>
        </FadeInView>
      )}

      {(summaryLoading || healthSummary) && (
        <FadeInView delay={120}>
          <AnimatedPressable
            onPress={() => latestReportId && router.push(`/(app)/report/${latestReportId}` as never)}
            disabled={!latestReportId}
          >
            <Card variant="accent" style={styles.summaryCard}>
              <View style={styles.summaryHeader}>
                <Icon name="heart-outline" size="md" color={colors.primary} />
                <Text style={styles.summaryTitle}>Your health summary</Text>
              </View>
              {summaryLoading ? (
                <View style={styles.skeletonWrap}>
                  <Skeleton height={14} width="90%" />
                  <Skeleton height={14} width="70%" style={{ marginTop: 8 }} />
                </View>
              ) : (
                <Text style={styles.summaryText}>{healthSummary}</Text>
              )}
              {latestReportId && !summaryLoading && (
                <View style={styles.summaryLinkRow}>
                  <Text style={styles.summaryLink}>View full report</Text>
                  <Icon name="chevron-forward" size="sm" color={colors.primaryDark} />
                </View>
              )}
            </Card>
          </AnimatedPressable>
        </FadeInView>
      )}

      {(remindersLoading || remindersSummary) && (
        <FadeInView delay={135}>
          <Card>
            <View style={styles.sectionHeader}>
              <Icon name="calendar-outline" size="md" color={colors.primary} />
              <Text style={styles.sectionTitle}>Follow-up tests</Text>
            </View>
            {remindersLoading ? (
              <Skeleton height={14} width="85%" />
            ) : (
              <>
                <Text style={styles.reminderSummary}>{remindersSummary}</Text>
                {retestReminders
                  .filter((reminder) => reminder.urgency === "OVERDUE" || reminder.urgency === "DUE_SOON")
                  .slice(0, 3)
                  .map((reminder) => (
                    <AnimatedPressable
                      key={reminder.id}
                      style={[
                        styles.reminderRow,
                        reminder.urgency === "OVERDUE" && styles.reminderRowOverdue,
                      ]}
                      onPress={() => reminder.reportId && router.push(`/(app)/report/${reminder.reportId}` as never)}
                      disabled={!reminder.reportId}
                    >
                      <View style={styles.reminderHeader}>
                        <Text style={styles.reminderLabel}>{reminder.label}</Text>
                        <Text
                          style={[
                            styles.reminderBadge,
                            reminder.urgency === "OVERDUE" ? styles.reminderBadgeOverdue : styles.reminderBadgeSoon,
                          ]}
                        >
                          {reminder.urgency === "OVERDUE" ? "Overdue" : "Due soon"}
                        </Text>
                      </View>
                      <Text style={styles.reminderMessage}>{reminder.message}</Text>
                    </AnimatedPressable>
                  ))}
              </>
            )}
          </Card>
        </FadeInView>
      )}

      {(insightsLoading || insightCards.length > 0) && (
        <FadeInView delay={150}>
          <Card>
            <View style={styles.sectionHeader}>
              <Icon name="bulb-outline" size="md" color={colors.primary} />
              <Text style={styles.sectionTitle}>Insights</Text>
            </View>
            {insightsLoading ? (
              <Skeleton height={14} width="90%" />
            ) : (
              insightCards.slice(0, 4).map((card) => (
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
              ))
            )}
          </Card>
        </FadeInView>
      )}

      {recentChanges.length > 0 && (
        <FadeInView delay={180}>
          <Card>
            <View style={styles.sectionHeader}>
              <Icon name="swap-vertical-outline" size="md" color={colors.textMuted} />
              <Text style={styles.sectionTitle}>What changed</Text>
            </View>
            {recentChanges.map((change) => (
              <View key={change.canonical} style={styles.changeRow}>
                <Text style={styles.changeName}>{formatBiomarkerName(change.canonical)}</Text>
                <Text
                  style={[
                    styles.changeValue,
                    { color: change.direction === "UP" ? colors.danger : colors.success },
                  ]}
                >
                  {change.latestValue?.toFixed(1)} {change.unit ?? ""}
                  {change.percentChange != null
                    ? ` (${change.direction === "UP" ? "+" : ""}${change.percentChange.toFixed(1)}%)`
                    : ""}
                </Text>
              </View>
            ))}
            <AnimatedPressable onPress={() => router.push("/(app)/trends" as never)}>
              <Text style={styles.summaryLink}>See all trends</Text>
            </AnimatedPressable>
          </Card>
        </FadeInView>
      )}

      {feed.length > 0 && (
        <FadeInView delay={180}>
          <Card>
            <View style={styles.sectionHeader}>
              <Icon name="time-outline" size="md" color={colors.textMuted} />
              <Text style={styles.sectionTitle}>Family activity</Text>
            </View>
            {feed.slice(0, 5).map((item, i) => (
              <AnimatedPressable
                key={item.reportId}
                style={[styles.feedRow, i === feed.slice(0, 5).length - 1 && styles.feedRowLast]}
                onPress={() => router.push(`/(app)/report/${item.reportId}` as never)}
              >
                <View style={styles.feedIcon}>
                  <Icon name="document-text-outline" size="sm" color={colors.primary} />
                </View>
                <View style={styles.feedContent}>
                  <Text style={styles.feedText}>
                    <Text style={styles.feedName}>{item.personName}</Text>
                    {" — "}
                    {item.extractionStatus === "COMPLETED"
                      ? "report ready"
                      : extractionStatusBadgeLabel(item.extractionStatus).toLowerCase()}
                  </Text>
                  <Text style={styles.feedFile} numberOfLines={1}>{item.filename}</Text>
                </View>
                <Icon name="chevron-forward" size="sm" color={colors.textSoft} />
              </AnimatedPressable>
            ))}
          </Card>
        </FadeInView>
      )}

      {biomarkers.length > 0 && (
        <FadeInView delay={240}>
          <Card>
            <View style={styles.sectionHeader}>
              <Icon name="analytics-outline" size="md" color={colors.textMuted} />
              <Text style={styles.sectionTitle}>Latest results</Text>
            </View>
            {biomarkers.map((b, i) => {
              const assessment = assessBiomarker(b);
              const flagged = assessment.status !== "unknown" && assessment.status !== "normal";
              return (
                <View
                  key={b.canonical}
                  style={[
                    styles.biomarkerRow,
                    flagged && styles.biomarkerRowFlagged,
                    i === biomarkers.length - 1 && styles.biomarkerRowLast,
                  ]}
                >
                  <Text style={styles.biomarkerName}>{formatBiomarkerName(b.canonical)}</Text>
                  <Text style={[styles.biomarkerValue, { color: statusColor(assessment.status) }]}>
                    {b.value ?? b.textValue ?? "—"} {b.unit ?? ""}
                  </Text>
                </View>
              );
            })}
          </Card>
        </FadeInView>
      )}

      <FadeInView delay={300}>
        <Card variant="muted" style={styles.infoCard}>
          <View style={styles.infoRow}>
            <Icon name={serverIcon} size="sm" color={serverColor} />
            <Text style={styles.infoText}>
              {backendStatus === "ok" ? "Server connected" : backendStatus === "error" ? "Server offline" : "Checking…"}
            </Text>
          </View>
        </Card>
      </FadeInView>

      <BottomSheet visible={confirmOpen} onClose={() => setConfirmOpen(false)} title={uploadSheetTitle(pendingDocumentType)}>
        <Text style={styles.sheetHint}>
          {pendingAsset ? `"${pendingAsset.name}" for ${pendingLabel}` : "Choose a file"}
        </Text>
        <ReportDatePicker
          value={optionalReportDate || null}
          onChange={setOptionalReportDate}
          required={pendingAsset?.mimeType?.startsWith("image/") ?? false}
          hint={
            pendingAsset?.mimeType?.startsWith("image/")
              ? photoDateRequiredHint(pendingDocumentType)
              : "Digital PDFs usually auto-detect. Add the date now if you already know it."
          }
        />
        <Button onPress={runUpload} fullWidth>Upload and extract</Button>
        <Pressable style={styles.modalCancel} onPress={() => setConfirmOpen(false)}>
          <Text style={styles.modalCancelText}>Cancel</Text>
        </Pressable>
      </BottomSheet>

      <BottomSheet
        visible={awaitingDateReportId != null}
        onClose={() => undefined}
        title="Report date required"
      >
        <Text style={styles.sheetCaption}>
          {reportDateRequiredCaption(awaitingDateDocumentType)}
        </Text>
        <ReportDatePicker
          value={requiredReportDate || null}
          onChange={setRequiredReportDate}
          label={reportDatePickerLabel(awaitingDateDocumentType)}
          required
        />
        <Button
          onPress={handleSubmitRequiredReportDate}
          loading={submittingReportDate}
          disabled={!isIsoDate(requiredReportDate)}
          fullWidth
        >
          Continue extraction
        </Button>
      </BottomSheet>

      <BottomSheet visible={pickerOpen} onClose={() => setPickerOpen(false)} title="Upload for who?">
        {members.map((m) => (
          <AnimatedPressable
            key={m.personId}
            style={styles.modalOption}
            onPress={() =>
              startUpload(
                m.personId === session?.personId ? undefined : m.personId,
                m.displayName,
                pendingDocumentType
              )
            }
          >
            <View style={styles.modalAvatar}>
              <Text style={styles.modalAvatarText}>{m.displayName.charAt(0)}</Text>
            </View>
            <View style={styles.modalOptionInfo}>
              <Text style={styles.modalOptionText}>{m.displayName}</Text>
              <Text style={styles.modalOptionMeta}>{m.relationship}</Text>
            </View>
            <Icon name="chevron-forward" size="sm" color={colors.textSoft} />
          </AnimatedPressable>
        ))}
        <Pressable style={styles.modalCancel} onPress={() => setPickerOpen(false)}>
          <Text style={styles.modalCancelText}>Cancel</Text>
        </Pressable>
      </BottomSheet>
    </Screen>
  );
}

const styles = StyleSheet.create({
  hero: { gap: spacing.sm },
  greeting: { ...typography.hero },
  familyChip: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    alignSelf: "flex-start",
    backgroundColor: colors.primaryLight,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: radii.sm,
  },
  familyLabel: { fontSize: 14, fontWeight: "700", color: colors.primaryDark },
  heroText: { ...typography.body },
  uploadCard: {
    backgroundColor: colors.surface,
    borderRadius: radii.lg,
    padding: spacing.lg,
    alignItems: "center",
    borderWidth: 2,
    borderColor: colors.primaryLight,
    borderStyle: "dashed",
    gap: spacing.sm,
  },
  uploadCardDisabled: { opacity: 0.7 },
  rxCard: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.md,
    backgroundColor: colors.surface,
    borderRadius: radii.lg,
    padding: spacing.md,
    marginTop: spacing.sm,
    borderWidth: 1,
    borderColor: colors.border,
  },
  rxCopy: { flex: 1, gap: 2 },
  rxTitle: { ...typography.subtitle, fontSize: 16 },
  rxHint: { ...typography.caption },
  uploadIconWrap: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: colors.primaryLight,
    alignItems: "center",
    justifyContent: "center",
  },
  uploadTitle: { fontSize: 18, fontWeight: "700", color: colors.primaryDark },
  uploadHint: { ...typography.caption },
  sheetHint: { ...typography.body, marginBottom: spacing.sm },
  sheetCaption: { ...typography.caption, marginTop: spacing.xs, marginBottom: spacing.sm },
  statusCard: { padding: spacing.md },
  statusRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  statusText: { color: colors.primaryDark, fontSize: 15, flex: 1 },
  summaryCard: { gap: spacing.sm },
  summaryHeader: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  summaryTitle: { ...typography.subtitle },
  skeletonWrap: { paddingVertical: spacing.xs },
  summaryText: { ...typography.body },
  summaryLinkRow: { flexDirection: "row", alignItems: "center", gap: 4 },
  summaryLink: { fontSize: 14, fontWeight: "600", color: colors.primaryDark },
  reminderSummary: { ...typography.body, marginBottom: spacing.sm },
  reminderRow: {
    borderTopWidth: 1,
    borderTopColor: colors.border,
    paddingVertical: spacing.sm,
    gap: 4,
  },
  reminderRowOverdue: {
    backgroundColor: colors.warningLight,
    marginHorizontal: -spacing.md,
    paddingHorizontal: spacing.md,
  },
  reminderHeader: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", gap: spacing.sm },
  reminderLabel: { fontSize: 14, fontWeight: "700", color: colors.text, flex: 1 },
  reminderBadge: { fontSize: 11, fontWeight: "700", paddingHorizontal: 8, paddingVertical: 3, borderRadius: radii.sm },
  reminderBadgeOverdue: { backgroundColor: colors.danger, color: colors.white },
  reminderBadgeSoon: { backgroundColor: colors.warning, color: colors.white },
  reminderMessage: { ...typography.caption, lineHeight: 20 },
  insightRow: {
    borderTopWidth: 1,
    borderTopColor: colors.border,
    paddingVertical: spacing.sm,
    gap: 4,
  },
  insightRowWarning: { backgroundColor: colors.warningLight, marginHorizontal: -spacing.md, paddingHorizontal: spacing.md },
  insightTitle: { fontSize: 14, fontWeight: "700", color: colors.text },
  insightMessage: { ...typography.caption, lineHeight: 20 },
  changeRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    gap: spacing.sm,
    paddingVertical: 8,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  changeName: { ...typography.body, flex: 1 },
  changeValue: { fontSize: 14, fontWeight: "700" },
  sectionHeader: { flexDirection: "row", alignItems: "center", gap: spacing.sm, marginBottom: spacing.sm },
  sectionTitle: { ...typography.subtitle, fontSize: 16 },
  feedRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.sm,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: colors.borderLight,
  },
  feedRowLast: { borderBottomWidth: 0 },
  feedIcon: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: colors.primaryLight,
    alignItems: "center",
    justifyContent: "center",
  },
  feedContent: { flex: 1 },
  feedText: { fontSize: 14, color: colors.text },
  feedName: { fontWeight: "700" },
  feedFile: { fontSize: 12, color: colors.textMuted, marginTop: 2 },
  biomarkerRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: colors.borderLight,
  },
  biomarkerRowLast: { borderBottomWidth: 0 },
  biomarkerRowFlagged: {
    backgroundColor: colors.dangerLight,
    marginHorizontal: -spacing.sm,
    paddingHorizontal: spacing.sm,
    borderRadius: radii.sm,
    borderBottomWidth: 0,
  },
  biomarkerName: { fontSize: 14, color: colors.text, fontWeight: "500", flex: 1 },
  biomarkerValue: { ...typography.data },
  infoCard: { padding: spacing.md },
  infoRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  infoText: { ...typography.caption, color: colors.textMuted },
  modalOption: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.md,
    padding: spacing.md,
    backgroundColor: colors.surfaceMuted,
    borderRadius: radii.md,
  },
  modalAvatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.primaryLight,
    alignItems: "center",
    justifyContent: "center",
  },
  modalAvatarText: { fontSize: 16, fontWeight: "700", color: colors.primaryDark },
  modalOptionInfo: { flex: 1 },
  modalOptionText: { fontSize: 16, fontWeight: "600", color: colors.text },
  modalOptionMeta: { fontSize: 13, color: colors.textMuted, marginTop: 2, textTransform: "capitalize" },
  modalCancel: { padding: spacing.md, alignItems: "center" },
  modalCancelText: { color: colors.textMuted, fontSize: 16 },
});
