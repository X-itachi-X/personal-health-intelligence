package com.phi.golden;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GoldenDatasetLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<LoadedFixture> loadAll() throws IOException {
        List<LoadedFixture> fixtures = new ArrayList<>();
        for (Path path : GoldenDatasetPaths.reportFixtures()) {
            GoldenReportFixture fixture = objectMapper.readValue(path.toFile(), GoldenReportFixture.class);
            fixtures.add(new LoadedFixture(path, fixture));
        }
        return fixtures;
    }

    public record LoadedFixture(Path path, GoldenReportFixture fixture) {
    }
}
