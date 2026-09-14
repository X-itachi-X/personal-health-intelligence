package com.phi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "phi")
public record PhiProperties(Storage storage, Claude claude) {

    public record Storage(String reportsDir) {
    }

    public record Claude(String apiKey, String model, String workspaceId) {
    }
}
