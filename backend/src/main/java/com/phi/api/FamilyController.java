package com.phi.api;

import com.phi.auth.AuthenticatedAccount;
import com.phi.family.FamilyDtos;
import com.phi.family.FamilyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/family")
public class FamilyController {

    private final FamilyService familyService;

    public FamilyController(FamilyService familyService) {
        this.familyService = familyService;
    }

    @GetMapping
    public List<FamilyDtos.FamilySummary> listFamilies(@AuthenticationPrincipal AuthenticatedAccount account) {
        return familyService.listFamilies(account);
    }

    @GetMapping("/{familyId}")
    public FamilyDtos.FamilyDetail getFamily(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable String familyId
    ) {
        return familyService.getFamily(account, familyId);
    }

    @PatchMapping("/{familyId}")
    public FamilyDtos.FamilyDetail renameFamily(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable String familyId,
            @Valid @RequestBody FamilyDtos.RenameFamilyRequest request
    ) {
        return familyService.renameFamily(account, familyId, request.displayName());
    }

    @PostMapping("/{familyId}/members")
    public FamilyDtos.MemberSummary addMember(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable String familyId,
            @Valid @RequestBody FamilyDtos.AddMemberRequest request
    ) {
        return familyService.addMember(account, familyId, request);
    }

    @GetMapping("/feed")
    public List<FamilyDtos.FeedItem> familyFeed(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @RequestParam(required = false) String familyId
    ) {
        return familyService.familyFeed(account, familyId);
    }
}
