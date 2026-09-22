package com.phi.imaging;

import java.time.LocalDate;
import java.util.List;

public final class ImagingDtos {

    private ImagingDtos() {
    }

    public record ExtractedFinding(
            String findingText,
            String severity,
            String measurementValue,
            String measurementUnit
    ) {
    }

    public record ExtractedImagingStudy(
            String modality,
            String bodyRegion,
            LocalDate studyDate,
            String facility,
            String impression,
            List<ExtractedFinding> findings
    ) {
    }

    public record ImagingDraftResponse(
            Long reportId,
            String studyDate,
            ExtractedImagingStudy study
    ) {
    }

    public record ConfirmFindingItem(
            String findingText,
            String severity,
            String measurementValue,
            String measurementUnit
    ) {
    }

    public record ConfirmImagingRequest(
            String modality,
            String bodyRegion,
            LocalDate studyDate,
            String facility,
            String impression,
            List<ConfirmFindingItem> findings
    ) {
    }

    public record ConfirmImagingResponse(String imagingStudyId) {
    }

    public record FindingView(
            String id,
            String findingText,
            String severity,
            String measurementValue,
            String measurementUnit
    ) {
    }

    public record StudyView(
            String id,
            Long personId,
            String modality,
            String bodyRegion,
            String studyDate,
            String facility,
            String impression,
            Long sourceReportId,
            List<FindingView> findings
    ) {
    }
}
