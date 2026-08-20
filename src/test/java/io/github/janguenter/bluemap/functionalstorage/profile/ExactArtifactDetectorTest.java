/*
 * SPDX-License-Identifier: MIT
 */

package io.github.janguenter.bluemap.functionalstorage.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExactArtifactDetectorTest {

    private static final String FUNCTIONAL_STORAGE_DESCRIPTOR = """
            modLoader="javafml"
            [[mods]]
            modId="functionalstorage"
            version="1.21.1-1.5.8"
            """;

    @TempDir
    Path temporary;

    @Test
    void acceptsExactFunctionalStorageArtifactWhenProvided() throws IOException {
        Path artifact = requiredProperty("functionalStorageJar");
        assertEquals(FunctionalStorageProfile.FUNCTIONAL_STORAGE_SIZE,
                artifact.toFile().length());
        assertEquals(artifact.toRealPath(), findFunctional(List.of(artifact))
                .orElseThrow());
    }

    @Test
    void acceptsExactTitaniumArtifactWhenProvided() {
        Path artifact = requiredProperty("titaniumJar");
        assertEquals(FunctionalStorageProfile.TITANIUM_SIZE,
                artifact.toFile().length());
        assertTrue(ExactArtifactDetector.find(
                List.of(artifact), "titanium",
                FunctionalStorageProfile.TITANIUM_SIZE,
                FunctionalStorageProfile.TITANIUM_SHA256
        ).isPresent());
    }

    @Test
    void rejectsUnsupportedDeclaringArtifact() throws IOException {
        Path wrong = writeJar(
                "wrong.jar", FUNCTIONAL_STORAGE_DESCRIPTOR, "wrong bytes"
        );
        assertTrue(findFunctional(List.of(wrong)).isEmpty());
    }

    @Test
    void rejectsCrossArtifactAndMissingPath() {
        Path titanium = requiredProperty("titaniumJar");
        assertTrue(findFunctional(List.of(
                titanium, temporary.resolve("missing.jar")
        )).isEmpty());
    }

    @Test
    void rejectsExactPlusDistinctWrongSameModInEitherOrder() throws IOException {
        Path exact = requiredProperty("functionalStorageJar");
        Path wrong = writeJar(
                "second-functional-storage.jar",
                FUNCTIONAL_STORAGE_DESCRIPTOR,
                "different-size declaring artifact"
        );

        assertTrue(findFunctional(List.of(exact, wrong)).isEmpty());
        assertTrue(findFunctional(List.of(wrong, exact)).isEmpty());
    }

    @Test
    void rejectsDistinctByteIdenticalCopies() throws IOException {
        Path exact = requiredProperty("functionalStorageJar");
        Path duplicate = temporary.resolve("duplicate.jar");
        Files.copy(exact, duplicate);

        assertTrue(findFunctional(List.of(exact, duplicate)).isEmpty());
        assertTrue(findFunctional(List.of(duplicate, exact)).isEmpty());
    }

    @Test
    void acceptsRepeatedAndSymlinkedAliasesOfOneRealArtifact() throws IOException {
        Path exact = requiredProperty("functionalStorageJar");
        Path aliases = temporary.resolve("aliases");
        Files.createDirectories(aliases);
        Path alias = aliases.resolve("renamed-alias.jar");
        Files.createSymbolicLink(alias, exact.toAbsolutePath());

        assertEquals(exact.toRealPath(), findFunctional(List.of(
                exact, exact, alias
        )).orElseThrow());
    }

    @Test
    void ignoresUnrelatedAndMultilineDecoyDescriptors() throws IOException {
        Path exact = requiredProperty("functionalStorageJar");
        Path unrelated = writeJar(
                "unrelated.jar",
                "[[mods]]\nmodId=\"another_mod\"\n",
                "unrelated"
        );
        Path decoy = writeJar(
                "decoy.jar",
                "hint=\"modId='functionalstorage'\"\n"
                        + "[[mods]]\nmodId=\"another_mod\"\n"
                        + "description=\"\"\"\n"
                        + "[[mods]]\nmodId=\"functionalstorage\"\n"
                        + "\"\"\"\n"
                        + "# [[mods]]\n# modId=\"functionalstorage\"\n",
                "decoy"
        );

        assertEquals(exact.toRealPath(), findFunctional(List.of(
                unrelated, decoy, exact
        )).orElseThrow());
    }

    @Test
    void malformedUnreadableAndOversizedJarDescriptorsFailClosed()
            throws IOException {
        Path exact = requiredProperty("functionalStorageJar");
        Path broken = temporary.resolve("broken.jar");
        Files.writeString(broken, "not a zip", StandardCharsets.UTF_8);
        Path malformedUtf8 = writeJar(
                "malformed-utf8.jar", new byte[]{(byte) 0xC3, 0x28}, "bad utf8"
        );
        Path malformedToml = writeJar(
                "malformed-toml.jar",
                FUNCTIONAL_STORAGE_DESCRIPTOR + "\ndescription=\"\"\"unterminated\n",
                "bad toml"
        );
        byte[] oversized = new byte[1024 * 1024 + 1];
        Path oversizedDescriptor = writeJar(
                "oversized.jar", oversized, "oversized descriptor"
        );

        assertTrue(findFunctional(List.of(exact, broken)).isEmpty());
        assertTrue(findFunctional(List.of(exact, malformedUtf8)).isEmpty());
        assertTrue(findFunctional(List.of(exact, malformedToml)).isEmpty());
        assertTrue(findFunctional(List.of(exact, oversizedDescriptor)).isEmpty());
    }

    @Test
    void rootEnumerationAndInterruptionFailClosed() {
        Path missing = temporary.resolve("missing.jar");
        assertTrue(findFunctional(Collections.nCopies(4_097, missing)).isEmpty());

        try {
            Thread.currentThread().interrupt();
            assertTrue(findFunctional(List.of(
                    requiredProperty("functionalStorageJar")
            )).isEmpty());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void invalidExpectedIdentityIsRejected() {
        assertThrows(NullPointerException.class, () -> ExactArtifactDetector.find(
                null, "functionalstorage", 1, "0".repeat(64)
        ));
        assertThrows(IllegalArgumentException.class, () -> ExactArtifactDetector.find(
                List.of(), "INVALID", 1, "0".repeat(64)
        ));
        assertThrows(IllegalArgumentException.class, () -> ExactArtifactDetector.find(
                List.of(), "functionalstorage", 0, "0".repeat(64)
        ));
        assertThrows(IllegalArgumentException.class, () -> ExactArtifactDetector.find(
                List.of(), "functionalstorage", 1, "not-a-hash"
        ));
    }

    private static java.util.Optional<Path> findFunctional(Iterable<Path> roots) {
        return ExactArtifactDetector.find(
                roots, "functionalstorage",
                FunctionalStorageProfile.FUNCTIONAL_STORAGE_SIZE,
                FunctionalStorageProfile.FUNCTIONAL_STORAGE_SHA256
        );
    }

    private Path writeJar(String name, String descriptor, String marker)
            throws IOException {
        return writeJar(name, descriptor.getBytes(StandardCharsets.UTF_8), marker);
    }

    private Path writeJar(String name, byte[] descriptor, String marker)
            throws IOException {
        Path jar = temporary.resolve(name);
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new ZipEntry("META-INF/neoforge.mods.toml"));
            output.write(descriptor);
            output.closeEntry();
            output.putNextEntry(new ZipEntry("marker.txt"));
            output.write(marker.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        return jar;
    }

    private static Path requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null) {
            throw new AssertionError("missing test system property " + name);
        }
        return Path.of(value);
    }
}
