/*
 * SPDX-License-Identifier: MIT
 *
 * The conservative material proof adapts owner-controlled MIT code from the
 * Sophisticated add-on. No candidate implementation is used.
 */
package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap522;

import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.map.hires.block.color.BlockColorCalculator;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Element;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Face;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Admits only one ordinary untransformed opaque canonical full-cube layer. */
final class BlockMaterialResolver {

    private static final Vector3f FULL_MIN = Vector3f.ZERO;
    private static final Vector3f FULL_MAX = new Vector3f(16F, 16F, 16F);
    private static final Vector4f FULL_UV = new Vector4f(0F, 0F, 16F, 16F);

    private final ResourcePack resourcePack;
    private final BlockItemDefaultStateResolver itemResolver;
    private final BlockColorCalculator colors;

    BlockMaterialResolver(
            ResourcePack resourcePack,
            BlockItemDefaultStateResolver itemResolver
    ) {
        this.resourcePack = resourcePack;
        this.itemResolver = itemResolver;
        this.colors = resourcePack.createBlockColorCalculator();
    }

    Optional<ResolvedBlockMaterial> resolve(String itemId, BlockNeighborhood host) {
        BlockState state = itemResolver.resolve(itemId);
        if (state == null || state.isWaterlogged()) {
            return Optional.empty();
        }
        de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState resource =
                resourcePack.getBlockStates().get(state.getId());
        if (resource == null || resource.getMultipart() != null) {
            return Optional.empty();
        }
        List<Variant> variants = new ArrayList<>();
        resource.forEach(state, host.getX(), host.getY(), host.getZ(), variants::add);
        if (variants.size() != 1) {
            return Optional.empty();
        }
        Variant variant = variants.get(0);
        Model model = variant.getModel().getResource(resourcePack.getModels()::get);
        if (!canonicalVariant(variant, model)) {
            return Optional.empty();
        }
        int tint = colors.getBlockColor(host, state, new Color()).getInt() | 0xFF00_0000;
        Map<Direction, ResolvedBlockMaterial.Face> faces = new EnumMap<>(Direction.class);
        Element element = model.getElements()[0];
        for (Direction direction : Direction.values()) {
            Face face = element.getFaces().get(direction);
            ResourcePath<Texture> texturePath = face.getTexture()
                    .getTexturePath(model.getTextures()::get);
            Texture texture = texturePath == null
                    ? null : resourcePack.getTextures().get(texturePath);
            if (!canonicalOpaque(texture)) {
                return Optional.empty();
            }
            faces.put(direction, new ResolvedBlockMaterial.Face(
                    texturePath, face.getTintindex() == 0 ? tint : 0xFFFF_FFFF
            ));
        }
        return Optional.of(new ResolvedBlockMaterial(faces));
    }

    static boolean canonicalVariant(Variant variant, Model model) {
        if (variant == null
                || variant.getRenderer() != BlockRendererType.DEFAULT
                || variant.isUvlock()
                || variant.isTransformed()
                || Double.compare(variant.getWeight(), 1D) != 0
                || model == null
                || !model.isAmbientocclusion()
                || model.getElements() == null
                || model.getElements().length != 1
                || model.getElements()[0] == null) {
            return false;
        }
        Element element = model.getElements()[0];
        if (!FULL_MIN.equals(element.getFrom())
                || !FULL_MAX.equals(element.getTo())
                || !element.isShade()
                || element.getLightEmission() != 0
                || element.getRotation().getX() != 0F
                || element.getRotation().getY() != 0F
                || element.getRotation().getZ() != 0F
                || element.getFaces().size() != Direction.values().length) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            Face face = element.getFaces().get(direction);
            if (face == null || face.getCullface() != direction
                    || face.getRotation() != 0
                    || !(face.getTintindex() == -1 || face.getTintindex() == 0)
                    || !FULL_UV.equals(face.getUv())) {
                return false;
            }
        }
        return true;
    }

    static boolean canonicalOpaque(Texture texture) {
        return texture != null && texture.getAnimation() == null
                && !texture.isHalfTransparent()
                && texture.getColorStraight().a >= 1F;
    }
}
