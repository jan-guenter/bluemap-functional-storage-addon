/*
 * SPDX-License-Identifier: MIT
 *
 * This aggregation follows BlueMap's MIT ResourceModelRenderer and
 * BlockStateModelRenderer map-color contracts.
 */

package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap522;

import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.math.Color;

/** Accumulates premultiplied upward-face samples and restores maximum opacity. */
final class MapColorAccumulator {

    private final Color sum = new Color().set(0F, 0F, 0F, 0F, true);
    private float maximumOpacity;

    void add(
            Texture texture,
            int argb,
            FaceLighting.Sample light,
            float ambientLight
    ) {
        Color sample = new Color().set(texture.getColorPremultiplied());
        multiplyPremultiplied(sample, new Color().set(argb));
        float combinedLight = Math.max(
                light.sunlight() / 15F,
                light.blocklight() / 15F
        );
        combinedLight = (1F - ambientLight) * combinedLight + ambientLight;
        sample.r *= combinedLight;
        sample.g *= combinedLight;
        sample.b *= combinedLight;
        maximumOpacity = Math.max(maximumOpacity, sample.a);
        sum.add(sample);
    }

    private static void multiplyPremultiplied(Color sample, Color tint) {
        /*
         * Color.multiply(Color) adopts the straight/premultiplied state of its
         * argument. Resource textures are already premultiplied, so applying a
         * straight tint that way makes Color.add reject every translucent
         * sample. Keep the representation intact and apply the mathematically
         * equivalent straight RGBA tint directly to the premultiplied channels.
         */
        sample.r *= tint.r * tint.a;
        sample.g *= tint.g * tint.a;
        sample.b *= tint.b * tint.a;
        sample.a *= tint.a;
    }

    void finish(Color target) {
        target.set(sum);
        finish(target, maximumOpacity);
    }

    static void combineVariants(Color target, Color... variants) {
        target.set(0F, 0F, 0F, 0F, true);
        float maximumOpacity = 0F;
        for (Color variant : variants) {
            if (variant == null) {
                continue;
            }
            maximumOpacity = Math.max(maximumOpacity, variant.a);
            target.add(new Color().set(variant).premultiplied());
        }
        finish(target, maximumOpacity);
    }

    private static void finish(Color target, float maximumOpacity) {
        if (target.a > 0F) {
            target.flatten().straight();
            target.a = maximumOpacity;
        }
    }
}
