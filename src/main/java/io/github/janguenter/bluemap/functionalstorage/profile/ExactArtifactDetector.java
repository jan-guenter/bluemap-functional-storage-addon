/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.functionalstorage.profile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

/** Bounded exact-byte activation gate for an installed artifact. */
public final class ExactArtifactDetector {

    private static final int MAX_ROOTS = 4096;

    private ExactArtifactDetector() {
    }

    public static Optional<Path> find(
            Iterable<Path> roots,
            long expectedSize,
            String expectedSha256
    ) {
        int count = 0;
        Set<Path> inspected = new HashSet<>();
        for (Path root : roots) {
            if (++count > MAX_ROOTS || Thread.currentThread().isInterrupted()) {
                return Optional.empty();
            }
            try {
                if (root == null || !Files.isRegularFile(root)
                        || Files.size(root) != expectedSize) {
                    continue;
                }
                Path real = root.toRealPath();
                if (inspected.add(real) && expectedSha256.equals(digest(real))) {
                    return Optional.of(real);
                }
            } catch (IOException exception) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static String digest(Path path) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
        byte[] buffer = new byte[64 * 1024];
        try (InputStream input = Files.newInputStream(path)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
