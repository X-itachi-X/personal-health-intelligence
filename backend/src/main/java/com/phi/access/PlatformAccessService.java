package com.phi.access;

import com.phi.auth.AuthenticatedAccount;
import com.phi.config.PhiProperties;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Platform operator access — for the app owner / deployer, not family testers.
 * Configured via {@code PHI_PLATFORM_ADMIN_EMAILS} (comma-separated).
 */
@Service
public class PlatformAccessService {

    private final Set<String> adminEmails;

    public PlatformAccessService(PhiProperties properties) {
        PhiProperties.Platform platform = properties.platform();
        this.adminEmails = platform != null ? platform.adminEmailSet() : Set.of();
    }

    public boolean isPlatformAdmin(AuthenticatedAccount account) {
        if (account == null || adminEmails.isEmpty()) {
            return false;
        }
        String email = account.getUsername();
        return email != null && adminEmails.contains(email.toLowerCase(Locale.ROOT));
    }

    public void requirePlatformAdmin(AuthenticatedAccount account) {
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (!isPlatformAdmin(account)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Platform operator access required");
        }
    }
}
