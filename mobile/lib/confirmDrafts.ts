import { ExtractedImagingStudy, ExtractedPrescriptionItem } from "./api";
import { ImagingDraft, ImagingFindingDraft } from "../components/report/ImagingConfirmCard";
import { MedicationDraftItem } from "../components/report/MedicationConfirmCard";

export function toMedicationDrafts(
  items: ExtractedPrescriptionItem[],
  fallbackStartedOn: string
): MedicationDraftItem[] {
  if (items.length === 0) {
    return [
      {
        id: "med-0",
        medicationName: "",
        dosage: "",
        scheduleText: "",
        startedOn: fallbackStartedOn,
        durationDays: "",
        courseType: "UNKNOWN",
      },
    ];
  }

  return items.map((item, index) => ({
    id: `med-${index}`,
    medicationName: item.medicationName,
    dosage: item.dosage ?? "",
    scheduleText: item.scheduleText ?? "",
    startedOn: item.startedOn ?? fallbackStartedOn,
    durationDays: item.durationDays != null ? String(item.durationDays) : "",
    courseType: item.courseType ?? "UNKNOWN",
  }));
}

export function toImagingDraft(study: ExtractedImagingStudy, fallbackStudyDate?: string | null): ImagingDraft {
  const findings: ImagingFindingDraft[] = (study.findings ?? []).map((finding, index) => ({
    id: `finding-${index}`,
    findingText: finding.findingText,
    severity: finding.severity ?? "",
    measurementValue: finding.measurementValue ?? "",
    measurementUnit: finding.measurementUnit ?? "",
  }));

  return {
    modality: study.modality || "OTHER",
    bodyRegion: study.bodyRegion ?? "",
    studyDate: study.studyDate ?? fallbackStudyDate ?? new Date().toISOString().slice(0, 10),
    facility: study.facility ?? "",
    impression: study.impression ?? "",
    findings,
  };
}

export function medicationDraftsToPayload(items: MedicationDraftItem[], fallbackStartedOn: string) {
  return items
    .map((item) => ({
      medicationName: item.medicationName.trim(),
      dosage: item.dosage.trim() || undefined,
      scheduleText: item.scheduleText.trim() || undefined,
      startedOn: item.startedOn.trim() || fallbackStartedOn,
      durationDays: item.durationDays.trim() ? Number.parseInt(item.durationDays, 10) : undefined,
      courseType: item.courseType,
    }))
    .filter((item) => item.medicationName.length > 0);
}

export function imagingDraftToPayload(draft: ImagingDraft) {
  return {
    modality: draft.modality,
    bodyRegion: draft.bodyRegion.trim() || undefined,
    studyDate: draft.studyDate.trim(),
    facility: draft.facility.trim() || undefined,
    impression: draft.impression.trim(),
    findings: draft.findings
      .filter((finding) => finding.findingText.trim().length > 0)
      .map((finding) => ({
        findingText: finding.findingText.trim(),
        severity: finding.severity.trim() || null,
        measurementValue: finding.measurementValue.trim() || null,
        measurementUnit: finding.measurementUnit.trim() || null,
      })),
  };
}
