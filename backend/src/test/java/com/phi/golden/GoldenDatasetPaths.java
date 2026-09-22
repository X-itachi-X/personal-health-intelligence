package com.phi.golden;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class GoldenDatasetPaths {

    private GoldenDatasetPaths() {
    }

    public static Path root() {
        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (cwd.endsWith("backend")) {
            return cwd.getParent().resolve("data/golden_dataset");
        }
        return cwd.resolve("data/golden_dataset");
    }

    public static List<Path> reportFixtures() throws IOException {
        return jsonFixtures("report_");
    }

    public static List<Path> prescriptionFixtures() throws IOException {
        return jsonFixturesInSubdir("prescriptions", "rx_");
    }

    public static List<Path> imagingFixtures() throws IOException {
        return jsonFixturesInSubdir("imaging", "usg_");
    }

    private static List<Path> jsonFixtures(String filenamePrefix) throws IOException {
        Path root = root();
        if (!Files.isDirectory(root)) {
            throw new IOException("Golden dataset directory not found: " + root);
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(filenamePrefix))
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    private static List<Path> jsonFixturesInSubdir(String subdir, String filenamePrefix) throws IOException {
        Path directory = root().resolve(subdir);
        if (!Files.isDirectory(directory)) {
            throw new IOException("Golden dataset subdirectory not found: " + directory);
        }
        try (Stream<Path> paths = Files.list(directory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(filenamePrefix))
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }
}
