import { router, useLocalSearchParams } from "expo-router";
import { useCallback, useEffect, useState } from "react";
import { Alert, StyleSheet, Text, View } from "react-native";
import { ImagingConfirmCard, ImagingDraft } from "../../../components/report/ImagingConfirmCard";
import { MedicationConfirmCard, MedicationDraftItem } from "../../../components/report/MedicationConfirmCard";
import * as DocumentPicker from "expo-document-picker";
import {
  confirmImagingStudy,
  confirmPrescriptionMedications,
  deleteReport,
  fetchMe,
  fetchReport,
  fetchReportFindings,
  parseReport,
  pollReportUntilDone,
  replaceFailedReport,
  ReportDetail,
  ReportFinding,
  retryReport,
  submitReportDate,
} from "../../../lib/api";
import { AuthSession } from "../../../lib/auth";
import {
  imagingDraftToPayload,
  medicationDraftsToPayload,
  toImagingDraft,
  toMedicationDrafts,
} from "../../../lib/confirmDrafts";
import { documentTypeBadgeLabel } from "../../../lib/documentTypes";
import { extractionStatusBadgeLabel, extractionStatusVariant, formatExtractionStatus } from "../../../lib/extraction";
import { formatDisplayDate, isIsoDate } from "../../../lib/reportDate";
import {
  assessBiomarker,
  formatBiomarkerName,
  statusColor,
  statusText,
} from "../../../lib/biomarkers";
import { colors, radii, spacing, typography } from "../../../lib/theme";
import { Badge } from "../../../components/ui/Badge";
import { BottomSheet } from "../../../components/ui/BottomSheet";
import { Button } from "../../../components/ui/Button";
import { Card } from "../../../components/ui/Card";
import { FadeInView } from "../../../components/ui/FadeInView";
import { Icon } from "../../../components/ui/Icon";
import { ProgressSteps } from "../../../components/ui/ProgressSteps";
import { ReportDatePicker } from "../../../components/ui/ReportDatePicker";
import { Screen } from "../../../components/ui/Screen";
import { SkeletonCard } from "../../../components/ui/Skeleton";

export default function ReportDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const [report, setReport] = useState<ReportDetail | null>(null);
  const [session, setSession] = useState<AuthSession | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [actionStatus, setActionStatus] = useState<string | null>(null);
  const [retrying, setRetrying] = useState(false);
  const [replacing, setReplacing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [reportDateInput, setReportDateInput] = useState("");
  const [savingReportDate, setSavingReportDate] = useState(false);
  const [editingReportDate, setEditingReportDate] = useState(false);
  const [replaceAsset, setReplaceAsset] = useState<DocumentPicker.DocumentPickerAsset | null>(null);
  const [replaceDate, setReplaceDate] = useState("");
  const [replaceConfirmOpen, setReplaceConfirmOpen] = useState(false);
  const [findings, setFindings] = useState<ReportFinding[]>([]);
  const [confirmingMeds, setConfirmingMeds] = useState(false);
  const [confirmingImaging, setConfirmingImaging] = useState(false);
  const [parsing, setParsing] = useState(false);
  const [medDrafts, setMedDrafts] = useState<MedicationDraftItem[]>([]);
  const [imagingDraft, setImagingDraft] = useState<ImagingDraft | null>(null);

  const loadReport = useCallback(async () => {
    const reportId = Number(id);
    if (!reportId) return;
    const detail = await fetchReport(reportId);
    setReport(detail);
    return detail;
  }, [id]);

  useEffect(() => {
    const reportId = Number(id);
    if (!reportId) return;
    fetchMe().then(setSession).catch(() => undefined);
    loadReport()
      .then((detail) => {
        if (detail && detail.extractionStatus === "COMPLETED" && detail.documentType !== "PRESCRIPTION") {
          fetchReportFindings(reportId)
            .then((response) => setFindings(response.findings))
            .catch(() => setFindings([]));
        }
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load"));
  }, [id, loadReport]);

  useEffect(() => {
    if (!report) return;

    const awaitingMeds =
      report.extractionStatus === "AWAITING_MEDICATION_CONFIRMATION" || report.needsMedicationConfirmation;
    const awaitingImaging =
      report.extractionStatus === "AWAITING_IMAGING_CONFIRMATION" || report.needsImagingConfirmation;
    const fallbackDate = report.reportDate ?? new Date().toISOString().slice(0, 10);

    if (awaitingMeds) {
      setMedDrafts(toMedicationDrafts(report.prescriptionItems ?? [], fallbackDate));
    } else {
      setMedDrafts([]);
    }

    if (awaitingImaging && report.imagingStudy) {
      setImagingDraft(toImagingDraft(report.imagingStudy, report.reportDate));
    } else {
      setImagingDraft(null);
    }
  }, [report]);

  async function handleRetry() {
    if (!report) return;
    setRetrying(true);
    setActionStatus("Retrying extraction…");
    setError(null);
    try {
      await retryReport(report.reportId);
      const updated = await pollReportUntilDone(report.reportId, (update) => {
        setActionStatus(formatExtractionStatus(update.extractionStatus));
      });
      setReport(updated);
      setActionStatus(
        updated.extractionStatus === "COMPLETED"
          ? `Done — ${updated.biomarkerCount} biomarkers found.`
          : formatExtractionStatus(updated.extractionStatus)
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Retry failed");
      setActionStatus(null);
      await loadReport().catch(() => undefined);
    } finally {
      setRetrying(false);
    }
  }

  async function handleReplace() {
    if (!report) return;
    const result = await DocumentPicker.getDocumentAsync({
      type: ["application/pdf", "image/*"],
      copyToCacheDirectory: true,
    });
    if (result.canceled || !result.assets?.[0]) return;

    setReplaceAsset(result.assets[0]);
    setReplaceDate("");
    setReplaceConfirmOpen(true);
  }

  async function runReplace() {
    if (!report || !replaceAsset) return;
    const isImage = replaceAsset.mimeType?.startsWith("image/") ?? false;
    if (isImage && !isIsoDate(replaceDate)) {
      setError("Photo uploads need the lab test date before we can continue.");
      return;
    }

    setReplaceConfirmOpen(false);
    setReplacing(true);
    setActionStatus(`Replacing with "${replaceAsset.name}"…`);
    setError(null);
    try {
      const upload = await replaceFailedReport(
        report.reportId,
        {
          uri: replaceAsset.uri,
          name: replaceAsset.name,
          mimeType: replaceAsset.mimeType,
          file: replaceAsset.file,
        },
        replaceDate || undefined
      );
      const updated = await pollReportUntilDone(upload.reportId, (update) => {
        setActionStatus(formatExtractionStatus(update.extractionStatus));
      });
      if (updated.extractionStatus === "AWAITING_REPORT_DATE") {
        setReport(updated);
        setReportDateInput("");
        setActionStatus("Report date required before extraction can continue.");
        return;
      }
      setReport(updated);
      setActionStatus(
        updated.extractionStatus === "COMPLETED"
          ? `Done — ${updated.biomarkerCount} biomarkers found.`
          : formatExtractionStatus(updated.extractionStatus)
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : "Replace failed");
      setActionStatus(null);
      await loadReport().catch(() => undefined);
    } finally {
      setReplacing(false);
      setReplaceAsset(null);
    }
  }

  function handleDelete() {
    if (!report) return;
    Alert.alert(
      "Delete failed report?",
      `Remove "${report.filename}" so you can upload a fresh copy.`,
      [
        { text: "Cancel", style: "cancel" },
        {
          text: "Delete",
          style: "destructive",
          onPress: async () => {
            setDeleting(true);
            try {
              await deleteReport(report.reportId);
              router.back();
            } catch (err) {
              setError(err instanceof Error ? err.message : "Delete failed");
            } finally {
              setDeleting(false);
            }
          },
        },
      ]
    );
  }

  if (error && !report) {
    return (
      <Screen scroll={false}>
        <Card variant="outlined" style={styles.errorCard}>
          <View style={styles.errorRow}>
            <Icon name="alert-circle-outline" size="md" color={colors.danger} />
            <Text style={styles.error}>{error}</Text>
          </View>
        </Card>
      </Screen>
    );
  }

  if (!report) {
    return (
      <Screen>
        <SkeletonCard />
        <SkeletonCard />
        <SkeletonCard />
      </Screen>
    );
  }

  async function handleSubmitReportDate(correct = false) {
    if (!report || !isIsoDate(reportDateInput)) {
      setError("Please choose a valid report date.");
      return;
    }

    setSavingReportDate(true);
    setActionStatus(
      correct ? "Updating report date…" : "Saving report date and continuing extraction…"
    );
    setError(null);
    try {
      await submitReportDate(report.reportId, reportDateInput, { correct });
      if (needsReportDate) {
        const updated = await pollReportUntilDone(report.reportId, (update) => {
          setActionStatus(formatExtractionStatus(update.extractionStatus));
        });
        setReport(updated);
        setActionStatus(
          updated.extractionStatus === "COMPLETED"
            ? `Done — ${updated.biomarkerCount} biomarkers found.`
            : formatExtractionStatus(updated.extractionStatus)
        );
      } else {
        const updated = await loadReport();
        if (updated) {
          setActionStatus(`Report date updated to ${formatDisplayDate(reportDateInput)}.`);
        }
      }
      setEditingReportDate(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save report date");
      setActionStatus(null);
      await loadReport().catch(() => undefined);
    } finally {
      setSavingReportDate(false);
    }
  }

  const isFailed = report.extractionStatus === "FAILED";
  const needsMedicationConfirmation =
    report.extractionStatus === "AWAITING_MEDICATION_CONFIRMATION" || report.needsMedicationConfirmation;
  const needsImagingConfirmation =
    report.extractionStatus === "AWAITING_IMAGING_CONFIRMATION" || report.needsImagingConfirmation;
  const needsReportDate = report.extractionStatus === "AWAITING_REPORT_DATE" || report.needsReportDate;
  const showDateCorrection =
    !needsReportDate &&
    (report.canCorrectReportDate || report.reportDateSource === "EXTRACTED") &&
    report.reportDate;
  const isWorking = retrying || replacing || savingReportDate || confirmingMeds || confirmingImaging || parsing;
  const docType = report.documentType ?? "LAB_REPORT";
  const isLabReport = docType === "LAB_REPORT";
  const isPrescription = docType === "PRESCRIPTION";
  const isImaging = docType === "IMAGING_REPORT";
  const isCompleted = report.extractionStatus === "COMPLETED";
  const needsManualParse =
    report.extractionStatus === "TEXT_EXTRACTED" && report.hasStoredText && report.reportDate;

  async function handleParse() {
    if (!report) return;
    setParsing(true);
    setActionStatus("Starting biomarker parsing…");
    setError(null);
    try {
      await parseReport(report.reportId);
      const updated = await pollReportUntilDone(report.reportId, (update) => {
        setActionStatus(formatExtractionStatus(update.extractionStatus));
      });
      setReport(updated);
      if (
        updated.extractionStatus === "AWAITING_MEDICATION_CONFIRMATION" ||
        updated.extractionStatus === "AWAITING_IMAGING_CONFIRMATION"
      ) {
        setActionStatus("Review extracted details and confirm.");
      } else if (updated.extractionStatus === "COMPLETED") {
        setActionStatus(`Done — ${updated.biomarkerCount} biomarkers found.`);
        if (updated.documentType === "LAB_REPORT") {
          fetchReportFindings(updated.reportId)
            .then((response) => setFindings(response.findings))
            .catch(() => setFindings([]));
        }
      } else {
        setActionStatus(formatExtractionStatus(updated.extractionStatus));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Parse failed");
      setActionStatus(null);
      await loadReport().catch(() => undefined);
    } finally {
      setParsing(false);
    }
  }

  async function handleConfirmImaging() {
    if (!report || !imagingDraft) {
      setError("No imaging study to confirm.");
      return;
    }
    const payload = imagingDraftToPayload(imagingDraft);
    if (!payload.impression) {
      setError("Impression is required before saving.");
      return;
    }
    if (!isIsoDate(payload.studyDate)) {
      setError("Enter a valid study date (YYYY-MM-DD).");
      return;
    }

    setConfirmingImaging(true);
    setError(null);
    try {
      await confirmImagingStudy(report.reportId, payload);
      setActionStatus("Imaging study saved to your health timeline.");
      await loadReport();
      router.replace("/(app)/trends" as never);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save imaging study");
    } finally {
      setConfirmingImaging(false);
    }
  }

  async function handleConfirmMedications() {
    if (!report) return;

    const fallbackStartedOn = report.reportDate ?? new Date().toISOString().slice(0, 10);
    const medications = medicationDraftsToPayload(medDrafts, fallbackStartedOn);
    if (medications.length === 0) {
      setError("Add at least one medication with a name.");
      return;
    }
    if (medications.some((item) => !isIsoDate(item.startedOn))) {
      setError("Each medication needs a valid start date (YYYY-MM-DD).");
      return;
    }

    setConfirmingMeds(true);
    setError(null);
    try {
      const result = await confirmPrescriptionMedications(report.reportId, medications);
      setActionStatus(`Saved ${result.savedCount} medication${result.savedCount === 1 ? "" : "s"}.`);
      await loadReport();
      router.replace("/(app)/settings" as never);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save medications");
    } finally {
      setConfirmingMeds(false);
    }
  }

  return (
    <Screen>
      <FadeInView delay={0}>
        <View style={styles.header}>
          <View style={styles.fileIcon}>
            <Icon name="document-text" size="lg" color={colors.primary} />
          </View>
          <Text style={styles.title}>{report.filename}</Text>
          <View style={styles.metaRow}>
            <Badge
              label={extractionStatusBadgeLabel(report.extractionStatus)}
              variant={extractionStatusVariant(report.extractionStatus)}
            />
            <Text style={styles.meta}>{documentTypeBadgeLabel(docType)}</Text>
            {isLabReport && (
              <Text style={styles.meta}>{report.biomarkerCount} biomarkers</Text>
            )}
          </View>
          {report.reportDate && (
            <Text style={styles.meta}>
              Report date: {formatDisplayDate(report.reportDate)}
              {report.reportDateSource === "EXTRACTED" ? " (auto-detected)" : ""}
            </Text>
          )}
          {report.extractionError && (
            <Text style={styles.error}>{report.extractionError}</Text>
          )}
          {session?.platformAdmin && (
            <Button
              variant="secondary"
              onPress={() => router.push(`/(app)/ops-pipeline/${report.reportId}` as never)}
              style={styles.pipelineBtn}
            >
              View pipeline trace
            </Button>
          )}
        </View>
      </FadeInView>

      {needsManualParse && (
        <FadeInView delay={30}>
          <Card variant="outlined" style={styles.recoveryCard}>
            <View style={styles.recoveryHeader}>
              <Icon name="sparkles-outline" size="md" color={colors.primary} />
              <Text style={styles.recoveryTitle}>Ready to parse</Text>
            </View>
            <Text style={styles.recoveryHint}>
              Text is saved in the database ({report.extractedTextLength?.toLocaleString() ?? 0} characters).
              {isLabReport
                ? " Auto-parse is off — tap below to run rules and Claude from stored text."
                : " Tap below to continue extraction from stored text."}
            </Text>
            {actionStatus && parsing && (
              <Text style={styles.actionStatus}>{actionStatus}</Text>
            )}
            <Button variant="primary" onPress={handleParse} loading={parsing} disabled={isWorking && !parsing}>
              {isLabReport ? "Parse biomarkers" : "Continue parsing"}
            </Button>
          </Card>
        </FadeInView>
      )}

      {needsImagingConfirmation && imagingDraft && (
        <FadeInView delay={35}>
          <ImagingConfirmCard
            draft={imagingDraft}
            onChange={setImagingDraft}
            onConfirm={handleConfirmImaging}
            loading={confirmingImaging}
          />
        </FadeInView>
      )}

      {needsMedicationConfirmation && medDrafts.length > 0 && (
        <FadeInView delay={35}>
          <MedicationConfirmCard
            items={medDrafts}
            onChange={setMedDrafts}
            onConfirm={handleConfirmMedications}
            loading={confirmingMeds}
          />
        </FadeInView>
      )}

      {needsReportDate && (
        <FadeInView delay={40}>
          <Card variant="outlined" style={styles.recoveryCard}>
            <View style={styles.recoveryHeader}>
              <Icon name="calendar-outline" size="md" color={colors.warning} />
              <Text style={styles.recoveryTitle}>Report date required</Text>
            </View>
            <Text style={styles.recoveryHint}>
              {isImaging
                ? "We could not read the scan date from this file. Enter when the study was done to continue extraction."
                : isPrescription
                  ? "We could not read the prescription date from this file. Enter when it was written to continue extraction."
                  : "We could not read the lab date from this file. Enter when the test was done to continue extraction and power trends correctly."}
            </Text>
            <ReportDatePicker
              value={reportDateInput || null}
              onChange={setReportDateInput}
              label={
                isImaging
                  ? "When was this scan done?"
                  : isPrescription
                    ? "When was this prescription written?"
                    : "When was this test done?"
              }
              required
            />
            <Button
              variant="primary"
              onPress={() => handleSubmitReportDate(false)}
              loading={savingReportDate}
              disabled={!isIsoDate(reportDateInput) || retrying || replacing}
            >
              Continue extraction
            </Button>
          </Card>
        </FadeInView>
      )}

      {showDateCorrection && (
        <FadeInView delay={50}>
          <Card variant="outlined" style={styles.recoveryCard}>
            <View style={styles.recoveryHeader}>
              <Icon name="calendar-outline" size="md" color={colors.warning} />
              <Text style={styles.recoveryTitle}>Verify report date</Text>
            </View>
            <Text style={styles.recoveryHint}>
              {report.reportDateSource === "EXTRACTED"
                ? "This date was read from the file. Correct it if the lab test was on a different day — trends use this date."
                : "Update the lab test date if it is wrong. Trends and dashboards will refresh."}
            </Text>
            {editingReportDate ? (
              <>
                <ReportDatePicker
                  value={reportDateInput || report.reportDate || null}
                  onChange={setReportDateInput}
                  label="Correct report date"
                  required
                />
                <View style={styles.recoveryActions}>
                  <Button
                    variant="primary"
                    onPress={() => handleSubmitReportDate(true)}
                    loading={savingReportDate}
                    disabled={!isIsoDate(reportDateInput)}
                  >
                    Save corrected date
                  </Button>
                  <Button variant="ghost" onPress={() => setEditingReportDate(false)}>
                    Cancel
                  </Button>
                </View>
              </>
            ) : (
              <Button variant="secondary" onPress={() => {
                setReportDateInput(report.reportDate ?? "");
                setEditingReportDate(true);
              }}>
                Edit report date
              </Button>
            )}
          </Card>
        </FadeInView>
      )}

      {isFailed && (
        <FadeInView delay={40}>
          <Card variant="outlined" style={styles.recoveryCard}>
            <View style={styles.recoveryHeader}>
              <Icon name="construct-outline" size="md" color={colors.warning} />
              <Text style={styles.recoveryTitle}>This report failed — you are not stuck</Text>
            </View>
            <Text style={styles.recoveryHint}>
              {report.hasReadableFile
                ? "The original file is still on the server. Retry extraction first."
                : report.hasStoredText
                  ? `Partial text is saved (${report.extractedTextLength?.toLocaleString() ?? 0} chars). Retry will continue from stored text.`
                  : "Replace with a better scan, or delete this report and upload again."}
            </Text>
            {isWorking && (
              <ProgressSteps
                steps={[
                  { id: "action", label: replacing ? "Replacing file" : "Retrying extraction", state: "active" },
                  { id: "parse", label: "Extract and parse", state: "pending" },
                ]}
                caption={actionStatus}
              />
            )}
            {actionStatus && !isWorking && (
              <Text style={styles.actionStatus}>{actionStatus}</Text>
            )}
            <View style={styles.recoveryActions}>
              {report.canRetry && (
                <Button variant="primary" onPress={handleRetry} loading={retrying} disabled={replacing || deleting}>
                  Retry extraction
                </Button>
              )}
              <Button variant="secondary" onPress={handleReplace} loading={replacing} disabled={retrying || deleting}>
                Replace with new scan
              </Button>
              <Button variant="ghost" onPress={handleDelete} loading={deleting} disabled={retrying || replacing}>
                Delete report
              </Button>
            </View>
          </Card>
        </FadeInView>
      )}

      {error && (
        <FadeInView delay={0}>
          <Card variant="outlined" style={styles.errorCard}>
            <View style={styles.errorRow}>
              <Icon name="alert-circle-outline" size="md" color={colors.danger} />
              <Text style={styles.error}>{error}</Text>
            </View>
          </Card>
        </FadeInView>
      )}

      {isPrescription && isCompleted && (report.prescriptionItems?.length ?? 0) > 0 && (
        <FadeInView delay={60}>
          <Card>
            <View style={styles.alertHeader}>
              <Icon name="medkit-outline" size="md" color={colors.primary} />
              <Text style={styles.alertTitle}>Medications saved</Text>
            </View>
            {(report.prescriptionItems ?? []).map((item) => (
              <View key={item.medicationName} style={styles.medConfirmRow}>
                <Text style={styles.medConfirmName}>{item.medicationName}</Text>
                <Text style={styles.medConfirmMeta}>
                  {item.courseType === "CHRONIC" ? "Ongoing" : item.courseType === "ACUTE" ? "Short course" : "Medication"}
                  {item.scheduleText ? ` · ${item.scheduleText}` : ""}
                </Text>
              </View>
            ))}
            <Button variant="secondary" onPress={() => router.push("/(app)/settings" as never)} style={styles.pipelineBtn}>
              View medication log
            </Button>
          </Card>
        </FadeInView>
      )}

      {isImaging && isCompleted && report.imagingStudy?.impression && (
        <FadeInView delay={60}>
          <Card>
            <View style={styles.alertHeader}>
              <Icon name="scan-outline" size="md" color={colors.primary} />
              <Text style={styles.alertTitle}>
                {report.imagingStudy.modality.replace(/_/g, " ")}
                {report.imagingStudy.bodyRegion ? ` · ${report.imagingStudy.bodyRegion}` : ""}
              </Text>
            </View>
            <Text style={styles.imagingImpression}>{report.imagingStudy.impression}</Text>
            {(report.imagingStudy.findings ?? []).map((finding) => (
              <View key={finding.findingText} style={styles.medConfirmRow}>
                <Text style={styles.medConfirmName}>{finding.findingText}</Text>
              </View>
            ))}
            <Button variant="secondary" onPress={() => router.push("/(app)/trends" as never)} style={styles.pipelineBtn}>
              View imaging timeline
            </Button>
          </Card>
        </FadeInView>
      )}

      {isLabReport && findings.length > 0 && (
        <FadeInView delay={60}>
          <Card style={styles.alertCard}>
            <View style={styles.alertHeader}>
              <Icon name="bulb-outline" size="md" color={colors.warning} />
              <Text style={styles.alertTitle}>Findings ({findings.length})</Text>
            </View>
            {findings.map((finding) => (
              <View key={`${finding.kind}-${finding.ruleId ?? finding.canonical}`} style={styles.findingRow}>
                <Text style={styles.findingTitle}>
                  {finding.kind === "CLINICAL_RULE"
                    ? finding.canonical?.replace(/_/g, " ") ?? "Clinical signal"
                    : finding.testName ?? finding.canonical}
                </Text>
                <Text style={styles.findingMessage}>{finding.message}</Text>
              </View>
            ))}
          </Card>
        </FadeInView>
      )}

      {isLabReport && report.biomarkers.map((b, index) => {
        const assessment = assessBiomarker(b);
        const isFlagged = assessment.status === "high" || assessment.status === "low";

        return (
          <FadeInView key={b.canonical} delay={120 + index * 30}>
            <Card
              style={{
                ...styles.row,
                ...(isFlagged ? styles.rowFlagged : {}),
                ...(assessment.status === "normal" ? styles.rowNormal : {}),
              }}
            >
              <View style={styles.rowLeft}>
                <Text style={styles.canonical}>{formatBiomarkerName(b.canonical)}</Text>
                {b.testName !== b.canonical && (
                  <Text style={styles.testName}>{b.testName}</Text>
                )}
              </View>
              <View style={styles.rowRight}>
                <Text style={[styles.value, { color: statusColor(assessment.status) }]}>
                  {b.value ?? b.textValue ?? "—"} {b.unit ?? ""}
                </Text>
                {b.referenceRange && (
                  <Text style={styles.range}>Ref: {b.referenceRange}</Text>
                )}
                {statusText(assessment.status) && (
                  <Text style={[styles.statusBadge, { color: statusColor(assessment.status) }]}>
                    {statusText(assessment.status)}
                  </Text>
                )}
              </View>
            </Card>
          </FadeInView>
        );
      })}

      <BottomSheet
        visible={replaceConfirmOpen}
        onClose={() => setReplaceConfirmOpen(false)}
        title="Replace report file"
      >
        <Text style={styles.recoveryHint}>
          {replaceAsset ? `Replace with "${replaceAsset.name}"` : "Choose a replacement file"}
        </Text>
        <ReportDatePicker
          value={replaceDate || null}
          onChange={setReplaceDate}
          required={replaceAsset?.mimeType?.startsWith("image/") ?? false}
          hint={
            replaceAsset?.mimeType?.startsWith("image/")
              ? "Required for photos — we do not auto-detect dates from camera scans."
              : "Optional for PDFs — we try to read the lab date from the file."
          }
        />
        <Button onPress={runReplace} loading={replacing} fullWidth>
          Replace and extract
        </Button>
      </BottomSheet>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { alignItems: "center", gap: spacing.sm, marginBottom: spacing.sm },
  fileIcon: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: colors.primaryLight,
    alignItems: "center",
    justifyContent: "center",
  },
  title: { ...typography.title, textAlign: "center" },
  metaRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  meta: { ...typography.caption },
  errorCard: { borderColor: colors.dangerBorder },
  errorRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  error: { color: colors.danger, fontSize: 14, flex: 1 },
  pipelineBtn: { marginTop: spacing.md, alignSelf: "stretch" },
  recoveryCard: { borderColor: colors.warning, gap: spacing.sm },
  recoveryHeader: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  recoveryTitle: { ...typography.subtitle, fontSize: 15, flex: 1 },
  recoveryHint: { ...typography.caption, lineHeight: 20 },
  recoveryActions: { gap: spacing.sm, marginTop: spacing.sm },
  actionStatus: { ...typography.caption, color: colors.primaryDark },
  alertCard: {
    backgroundColor: colors.dangerLight,
    borderWidth: 1,
    borderColor: colors.dangerBorder,
  },
  alertHeader: { flexDirection: "row", alignItems: "center", gap: spacing.sm },
  alertTitle: { fontSize: 14, fontWeight: "700", color: colors.danger },
  alertText: { ...typography.caption, marginTop: 4, lineHeight: 20 },
  findingRow: { marginTop: spacing.sm, paddingTop: spacing.sm, borderTopWidth: 1, borderTopColor: colors.border },
  findingTitle: { ...typography.subtitle, fontSize: 15 },
  findingMessage: { ...typography.caption, marginTop: 4, lineHeight: 20 },
  medConfirmRow: {
    paddingVertical: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  medConfirmName: { ...typography.subtitle, fontSize: 15 },
  medConfirmMeta: { ...typography.caption, marginTop: 2 },
  imagingImpression: { ...typography.body, lineHeight: 22, marginBottom: spacing.sm },
  row: { flexDirection: "row", gap: spacing.sm },
  rowFlagged: {
    backgroundColor: colors.dangerLight,
    borderColor: colors.dangerBorder,
  },
  rowNormal: {
    borderColor: colors.successBorder,
  },
  rowLeft: { flex: 1 },
  rowRight: { alignItems: "flex-end" },
  canonical: { ...typography.subtitle, fontSize: 15 },
  testName: { fontSize: 12, color: colors.textSoft, marginTop: 2 },
  value: { ...typography.data },
  range: { fontSize: 11, color: colors.textMuted, marginTop: 2 },
  statusBadge: { fontSize: 11, fontWeight: "700", marginTop: 4 },
});
