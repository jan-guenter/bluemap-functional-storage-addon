/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.functionalstorage.model;

/** Strict persisted material item identities; particle is validation-only. */
public record FramedMaterialSnapshot(
        String particle,
        String side,
        String front,
        String frontDivider
) {
}
