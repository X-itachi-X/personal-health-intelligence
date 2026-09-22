package com.phi.api;

import com.phi.auth.AuthenticatedAccount;
import com.phi.invite.InviteDtos;
import com.phi.invite.InviteService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invites")
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @PostMapping
    public InviteDtos.InviteResponse createInvite(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @Valid @RequestBody InviteDtos.CreateInviteRequest request
    ) {
        return inviteService.createInvite(account, request.familyId(), request.personId());
    }

    @GetMapping("/{code}")
    public InviteDtos.InvitePreview preview(@PathVariable String code) {
        return inviteService.preview(code);
    }
}
