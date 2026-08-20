/*
 * SPDX-License-Identifier: MIT
 */

package io.github.janguenter.bluemap.functionalstorage.model;

import java.util.Objects;

/** Normalized native or strict styled framed design. */
public record FramedMaterialSnapshot(
        Mode mode,
        String particle,
        String side,
        String front,
        String frontDivider
) {

    public FramedMaterialSnapshot {
        Objects.requireNonNull(mode, "mode");
        boolean complete = particle != null && side != null
                && front != null && frontDivider != null;
        boolean empty = particle == null && side == null
                && front == null && frontDivider == null;
        if ((mode == Mode.NATIVE && !empty) || (mode == Mode.STYLED && !complete)) {
            throw new IllegalArgumentException("inconsistent framed design mode");
        }
    }

    public static FramedMaterialSnapshot nativeDesign() {
        return new FramedMaterialSnapshot(Mode.NATIVE, null, null, null, null);
    }

    public static FramedMaterialSnapshot styled(
            String particle,
            String side,
            String front,
            String frontDivider
    ) {
        return new FramedMaterialSnapshot(
                Mode.STYLED, particle, side, front, frontDivider
        );
    }

    public boolean isNative() {
        return mode == Mode.NATIVE;
    }

    public enum Mode {
        NATIVE,
        STYLED
    }
}
