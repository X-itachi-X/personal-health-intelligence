package com.phi.api;

import com.phi.auth.AuthenticatedAccount;
import com.phi.imaging.ImagingDtos;
import com.phi.imaging.ImagingQueryService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/persons/{personId}/imaging-studies")
public class ImagingController {

    private final ImagingQueryService imagingQueryService;

    public ImagingController(ImagingQueryService imagingQueryService) {
        this.imagingQueryService = imagingQueryService;
    }

    @GetMapping
    public List<ImagingDtos.StudyView> list(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId
    ) {
        return imagingQueryService.listForPerson(account, personId);
    }

    @GetMapping("/{studyId}")
    public ImagingDtos.StudyView get(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId,
            @PathVariable String studyId
    ) {
        return imagingQueryService.getForPerson(account, personId, studyId);
    }
}
