/*
 * SPDX-License-Identifier: MIT
 *
 * This bounded installed-artifact scanner adapts the exact-mod detector
 * pattern from the owner's MIT BlueMap Sophisticated add-on.
 */
package io.github.janguenter.bluemap.functionalstorage.profile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Bounded, unambiguous exact-byte activation gate for an installed mod. */
public final class ExactArtifactDetector {

    private static final int MAX_ROOTS = 4_096;
    private static final int MAX_DESCRIPTOR_BYTES = 1024 * 1024;
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final String MOD_DESCRIPTOR = "META-INF/neoforge.mods.toml";
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern MOD_ID = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern MODS_TABLE = Pattern.compile(
            "^\\[\\[\\s*(?:mods|\\\"mods\\\"|'mods')\\s*\\]\\]$"
    );

    private ExactArtifactDetector() {
    }

    /**
     * Returns the only distinct JAR declaring {@code expectedModId}, provided
     * its complete installed bytes match the frozen identity.
     */
    public static Optional<Path> find(
            Iterable<Path> roots,
            String expectedModId,
            long expectedSize,
            String expectedSha256
    ) {
        Objects.requireNonNull(roots, "roots");
        Objects.requireNonNull(expectedModId, "expectedModId");
        Objects.requireNonNull(expectedSha256, "expectedSha256");
        if (!MOD_ID.matcher(expectedModId).matches()
                || expectedSize <= 0
                || !SHA256.matcher(expectedSha256).matches()) {
            throw new IllegalArgumentException("invalid exact artifact identity");
        }

        Set<Path> inspected = new HashSet<>();
        Path candidate = null;
        int rootCount = 0;
        for (Path root : roots) {
            if (Thread.currentThread().isInterrupted() || ++rootCount > MAX_ROOTS) {
                return Optional.empty();
            }
            if (root == null || !Files.isRegularFile(root)) {
                continue;
            }
            Path fileName = root.getFileName();
            if (fileName == null
                    || !fileName.toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                continue;
            }
            try {
                Path real = root.toRealPath();
                if (!inspected.add(real) || !declaresMod(real, expectedModId)) {
                    continue;
                }
                if (candidate != null) {
                    return Optional.empty();
                }
                candidate = real;
            } catch (IOException exception) {
                return Optional.empty();
            }
        }

        try {
            return candidate != null
                    && Files.size(candidate) == expectedSize
                    && expectedSha256.equals(digest(candidate))
                    ? Optional.of(candidate)
                    : Optional.empty();
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private static boolean declaresMod(Path jar, String expectedModId)
            throws IOException {
        Pattern declaration = Pattern.compile(
                "^(?:modId|\\\"modId\\\"|'modId')\\s*=\\s*(?:\\\""
                        + Pattern.quote(expectedModId) + "\\\"|'"
                        + Pattern.quote(expectedModId) + "')$"
        );
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry descriptor = zip.getEntry(MOD_DESCRIPTOR);
            if (descriptor == null || descriptor.isDirectory()) {
                return false;
            }
            if (descriptor.getSize() > MAX_DESCRIPTOR_BYTES) {
                throw new IOException("NeoForge mod descriptor exceeds inspection limit");
            }
            byte[] bytes;
            try (InputStream input = zip.getInputStream(descriptor)) {
                bytes = input.readNBytes(MAX_DESCRIPTOR_BYTES + 1);
            }
            if (bytes.length > MAX_DESCRIPTOR_BYTES) {
                throw new IOException("NeoForge mod descriptor exceeds inspection limit");
            }
            return descriptorDeclares(decodeUtf8(bytes), declaration);
        }
    }

    private static String decodeUtf8(byte[] bytes) throws IOException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IOException("NeoForge mod descriptor is not valid UTF-8", exception);
        }
    }

    private static boolean descriptorDeclares(
            String descriptor,
            Pattern declaration
    ) throws IOException {
        String normalized = descriptor.startsWith("\ufeff")
                ? descriptor.substring(1) : descriptor;
        QuoteMode mode = QuoteMode.NORMAL;
        boolean inModsTable = false;
        boolean declared = false;
        for (String line : normalized.split("\\R", -1)) {
            LexedLine lexed = lexLine(line, mode);
            mode = lexed.mode();
            String statement = lexed.statement().trim();
            if (statement.isEmpty()) {
                continue;
            }
            if (statement.startsWith("[")) {
                inModsTable = MODS_TABLE.matcher(statement).matches();
            } else if (inModsTable && declaration.matcher(statement).matches()) {
                declared = true;
            }
        }
        if (mode != QuoteMode.NORMAL) {
            throw new IOException("unterminated multiline string in mod descriptor");
        }
        return declared;
    }

    private static LexedLine lexLine(String line, QuoteMode initial)
            throws IOException {
        StringBuilder statement = new StringBuilder(line.length());
        QuoteMode mode = initial;
        for (int index = 0; index < line.length();) {
            if (mode == QuoteMode.MULTILINE_BASIC
                    || mode == QuoteMode.MULTILINE_LITERAL) {
                String delimiter = mode == QuoteMode.MULTILINE_BASIC ? "\"\"\"" : "'''";
                if (line.startsWith(delimiter, index)
                        && (mode != QuoteMode.MULTILINE_BASIC
                        || !escaped(line, index))) {
                    mode = QuoteMode.NORMAL;
                    index += delimiter.length();
                } else {
                    index++;
                }
                continue;
            }

            char character = line.charAt(index);
            if (mode == QuoteMode.BASIC) {
                statement.append(character);
                if (character == '"' && !escaped(line, index)) {
                    mode = QuoteMode.NORMAL;
                }
                index++;
                continue;
            }
            if (mode == QuoteMode.LITERAL) {
                statement.append(character);
                if (character == '\'') {
                    mode = QuoteMode.NORMAL;
                }
                index++;
                continue;
            }
            if (line.startsWith("\"\"\"", index)) {
                mode = QuoteMode.MULTILINE_BASIC;
                index += 3;
            } else if (line.startsWith("'''", index)) {
                mode = QuoteMode.MULTILINE_LITERAL;
                index += 3;
            } else if (character == '#') {
                break;
            } else {
                statement.append(character);
                if (character == '"') {
                    mode = QuoteMode.BASIC;
                } else if (character == '\'') {
                    mode = QuoteMode.LITERAL;
                }
                index++;
            }
        }
        if (mode == QuoteMode.BASIC || mode == QuoteMode.LITERAL) {
            throw new IOException("unterminated string in mod descriptor");
        }
        return new LexedLine(statement.toString(), mode);
    }

    private static boolean escaped(String line, int index) {
        int count = 0;
        for (int cursor = index - 1;
             cursor >= 0 && line.charAt(cursor) == '\\'; cursor--) {
            count++;
        }
        return (count & 1) != 0;
    }

    private static String digest(Path path) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream input = Files.newInputStream(path)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private record LexedLine(String statement, QuoteMode mode) {
    }

    private enum QuoteMode {
        NORMAL,
        BASIC,
        LITERAL,
        MULTILINE_BASIC,
        MULTILINE_LITERAL
    }
}
