package com.phi.api;

import com.phi.auth.AuthDtos;
import com.phi.auth.AuthService;
import com.phi.auth.AuthenticatedAccount;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/google")
    public AuthDtos.AuthResponse loginWithGoogle(@Valid @RequestBody AuthDtos.GoogleAuthRequest request) {
        return authService.loginWithGoogle(request);
    }

    @GetMapping("/me")
    public AuthDtos.MeResponse me(@AuthenticationPrincipal AuthenticatedAccount principal) {
        return authService.me(principal);
    }

    @PutMapping("/me/ui-mode")
    public AuthDtos.MeResponse updateUiMode(
            @AuthenticationPrincipal AuthenticatedAccount principal,
            @Valid @RequestBody AuthDtos.UiModeRequest request
    ) {
        return authService.updateUiMode(principal, request.uiMode());
    }
}
