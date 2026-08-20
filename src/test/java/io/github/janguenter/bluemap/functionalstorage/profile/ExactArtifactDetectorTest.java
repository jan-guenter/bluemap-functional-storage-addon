/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.functionalstorage.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExactArtifactDetectorTest {

    @TempDir
    Path temporary;

    @Test
    void acceptsExactFunctionalStorageArtifactWhenProvided() {
        Path artifact = requiredProperty("functionalStorageJar");
        assertEquals(FunctionalStorageProfile.FUNCTIONAL_STORAGE_SIZE,
                artifact.toFile().length());
        assertEquals(artifact.toAbsolutePath().normalize().toString(),
                ExactArtifactDetector.find(
                        List.of(artifact),
                        FunctionalStorageProfile.FUNCTIONAL_STORAGE_SIZE,
                        FunctionalStorageProfile.FUNCTIONAL_STORAGE_SHA256
                ).orElseThrow().toString());
    }

    @Test
    void acceptsExactTitaniumArtifactWhenProvided() {
        Path artifact = requiredProperty("titaniumJar");
        assertEquals(FunctionalStorageProfile.TITANIUM_SIZE,
                artifact.toFile().length());
        assertTrue(ExactArtifactDetector.find(
                List.of(artifact),
                FunctionalStorageProfile.TITANIUM_SIZE,
                FunctionalStorageProfile.TITANIUM_SHA256
        ).isPresent());
    }

    @Test
    void rejectsSameSizeWrongBytes() throws IOException {
        Path wrong = temporary.resolve("wrong.jar");
        try (var output = Files.newOutputStream(wrong)) {
            output.write(new byte[8192]);
        }
        assertTrue(ExactArtifactDetector.find(
                List.of(wrong), 8192,
                FunctionalStorageProfile.FUNCTIONAL_STORAGE_SHA256
        ).isEmpty());
    }

    @Test
    void rejectsCrossArtifactAndMissingPath() {
        Path titanium = requiredProperty("titaniumJar");
        assertTrue(ExactArtifactDetector.find(
                List.of(titanium, temporary.resolve("missing")),
                FunctionalStorageProfile.FUNCTIONAL_STORAGE_SIZE,
                FunctionalStorageProfile.FUNCTIONAL_STORAGE_SHA256
        ).isEmpty());
    }

    private static Path requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null) {
            throw new AssertionError("missing test system property " + name);
        }
        return Path.of(value);
    }
}
