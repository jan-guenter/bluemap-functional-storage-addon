/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap522;

import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.math.VectorM3f;
import de.bluecolored.bluemap.core.world.LightData;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;

/** Samples light on the exposed side of a transformed child-shell face. */
final class FaceLighting {

    private FaceLighting() {
    }

    static Sample sample(
            BlockNeighborhood block,
            Direction direction,
            Variant transform,
            int lightEmission
    ) {
        VectorM3f relative = new VectorM3f(0F, 0F, 0F).set(direction.toVector());
        if (transform.isTransformed()) {
            relative.rotateAndScale(transform.getTransformMatrix());
        }
        LightData own = block.getLightData();
        LightData faced = block.getNeighborBlock(
                Math.round(relative.x), Math.round(relative.y), Math.round(relative.z)
        ).getLightData();
        return new Sample(
                Math.max(own.getSkyLight(), faced.getSkyLight()),
                Math.max(lightEmission,
                        Math.max(own.getBlockLight(), faced.getBlockLight()))
        );
    }

    record Sample(int sunlight, int blocklight) {
    }
}
