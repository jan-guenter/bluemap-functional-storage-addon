/*
 * SPDX-License-Identifier: MIT
 */

package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap523;

import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.Key;

import java.util.EnumMap;
import java.util.Map;

/** Exact per-face texture and stable default-state tint of an admitted BlockItem. */
record ResolvedBlockMaterial(Map<Direction, Face> faces) {

    ResolvedBlockMaterial {
        faces = Map.copyOf(new EnumMap<>(faces));
    }

    Face face(Direction direction) {
        return faces.get(direction);
    }

    record Face(Key texture, int argb) {
    }
}
