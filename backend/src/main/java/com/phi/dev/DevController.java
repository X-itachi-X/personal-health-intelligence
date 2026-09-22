package com.phi.dev;

import com.phi.analytics.AnalyticsDtos;
import com.phi.analytics.AnalyticsSyncService;
import com.phi.auth.AuthenticatedAccount;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dev")
@Profile("dev")
public class DevController {

    private final DevSeedService devSeedService;
    private final AnalyticsSyncService analyticsSyncService;

    public DevController(DevSeedService devSeedService, AnalyticsSyncService analyticsSyncService) {
        this.devSeedService = devSeedService;
        this.analyticsSyncService = analyticsSyncService;
    }

    @PostMapping("/seed")
    public ResponseEntity<DevSeedDtos.SeedResponse> seedDemoData(
            @AuthenticationPrincipal AuthenticatedAccount account
    ) {
        return ResponseEntity.ok(devSeedService.seedFor(account));
    }

    @PostMapping("/analytics-rebuild")
    public ResponseEntity<AnalyticsDtos.RebuildResponse> rebuildAnalytics(
            @AuthenticationPrincipal AuthenticatedAccount account
    ) {
        return ResponseEntity.ok(analyticsSyncService.rebuildAll());
    }
}
