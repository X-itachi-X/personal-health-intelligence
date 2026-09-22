package com.phi.dev;

public final class DevSeedDtos {

    private DevSeedDtos() {
    }

    public record SeedResponse(
            boolean alreadySeeded,
            int families,
            int membersAdded,
            int reportsAdded,
            int scenariosLoaded,
            String message
    ) {
    }
}
