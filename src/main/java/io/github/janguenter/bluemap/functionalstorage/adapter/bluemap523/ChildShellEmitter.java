/*
 * SPDX-License-Identifier: MIT
 *
 * Model emission adapts BlueMap's and the owner's Sophisticated add-on's MIT
 * coordinate/UV conventions, extended only for installed child definitions,
 * exact variant transforms, and UV lock.
 */

package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap523;

import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModel;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Element;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Face;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import de.bluecolored.bluemap.core.world.block.ExtendedBlock;
import io.github.janguenter.bluemap.functionalstorage.model.ChildShellCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Emits one installed ordinary child model with a bounded material override. */
final class ChildShellEmitter {

    private static final float BLOCK_SCALE = 1F / 16F;

    private final ResourcePack resourcePack;
    private final TextureGallery textures;
    private final RenderSettings settings;

    ChildShellEmitter(
            ResourcePack resourcePack,
            TextureGallery textures,
            RenderSettings settings
    ) {
        this.resourcePack = resourcePack;
        this.textures = textures;
        this.settings = settings;
    }

    boolean emitNative(
            ChildShellCatalog.Child child,
            Variant variant,
            BlockNeighborhood block,
            TileModelView target,
            MapColorAccumulator mapColor
    ) {
        return emit(child, null, false, variant, block, target, mapColor);
    }

    boolean emitStyled(
            ChildShellCatalog.Child child,
            ResolvedBlockMaterial material,
            Variant variant,
            BlockNeighborhood block,
            TileModelView target,
            MapColorAccumulator mapColor
    ) {
        return emit(child, material, true, variant, block, target, mapColor);
    }

    private boolean emit(
            ChildShellCatalog.Child child,
            ResolvedBlockMaterial material,
            boolean substituteMaterial,
            Variant variant,
            BlockNeighborhood block,
            TileModelView target,
            MapColorAccumulator mapColor
    ) {
        Model model = resourcePack.getModels().get(child.parent());
        if (!preflight(child, material, substituteMaterial, model, variant)) {
            return false;
        }
        int modelStart = target.getTileModel().size();
        for (Element element : model.getElements()) {
            if (element == null) {
                continue;
            }
            int elementStart = target.getTileModel().size();
            emitElement(
                    child, material, substituteMaterial, model, element,
                    variant, block, target, mapColor
            );
            int elementCount = target.getTileModel().size() - elementStart;
            if (elementCount > 0) {
                target.initialize(elementStart);
                target.transform(new MatrixM4f()
                        .copy(element.getRotation().getMatrix())
                        .scale(BLOCK_SCALE, BLOCK_SCALE, BLOCK_SCALE));
            }
        }
        int count = target.getTileModel().size() - modelStart;
        if (count > 0 && variant.isTransformed()) {
            target.initialize(modelStart).transform(variant.getTransformMatrix());
        }
        target.initialize(modelStart);
        return true;
    }

    private boolean preflight(
            ChildShellCatalog.Child child,
            ResolvedBlockMaterial material,
            boolean substituteMaterial,
            Model model,
            Variant variant
    ) {
        if ((substituteMaterial && child.material() != (material != null))
                || (!substituteMaterial && material != null)
                || model == null || model.getElements() == null
                || model.getElements().length == 0) {
            return false;
        }
        boolean faceFound = false;
        for (Element element : model.getElements()) {
            if (element == null) {
                continue;
            }
            for (Map.Entry<Direction, Face> entry : element.getFaces().entrySet()) {
                faceFound = true;
                ResolvedBlockMaterial.Face replacement = replacement(
                        child, material, substituteMaterial,
                        entry.getValue(), entry.getKey(), element, variant
                );
                if (requiresReplacement(
                        child, substituteMaterial, entry.getValue())
                        && replacement == null) {
                    return false;
                }
                Key texture = replacement == null
                        ? textureKey(model, entry.getValue(), child.textures())
                        : replacement.texture();
                Texture resource = texture == null
                        ? null : resourcePack.getTextures().get(texture);
                if (resource == null) {
                    return false;
                }
            }
        }
        return faceFound;
    }

    private boolean emitElement(
            ChildShellCatalog.Child child,
            ResolvedBlockMaterial material,
            boolean substituteMaterial,
            Model model,
            Element element,
            Variant variant,
            BlockNeighborhood block,
            TileModelView target,
            MapColorAccumulator mapColor
    ) {
        Vector3f from = element.getFrom();
        Vector3f to = element.getTo();
        float x0 = from.getX();
        float y0 = from.getY();
        float z0 = from.getZ();
        float x1 = to.getX();
        float y1 = to.getY();
        float z1 = to.getZ();
        boolean emitted = false;
        emitted |= emitFace(child, material, substituteMaterial,
                model, element, Direction.DOWN,
                variant, block, target, mapColor,
                x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        emitted |= emitFace(child, material, substituteMaterial,
                model, element, Direction.UP,
                variant, block, target, mapColor,
                x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        emitted |= emitFace(child, material, substituteMaterial,
                model, element, Direction.NORTH,
                variant, block, target, mapColor,
                x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        emitted |= emitFace(child, material, substituteMaterial,
                model, element, Direction.SOUTH,
                variant, block, target, mapColor,
                x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        emitted |= emitFace(child, material, substituteMaterial,
                model, element, Direction.WEST,
                variant, block, target, mapColor,
                x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        emitted |= emitFace(child, material, substituteMaterial,
                model, element, Direction.EAST,
                variant, block, target, mapColor,
                x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
        return emitted;
    }

    private boolean emitFace(
            ChildShellCatalog.Child child,
            ResolvedBlockMaterial material,
            boolean substituteMaterial,
            Model model,
            Element element,
            Direction direction,
            Variant variant,
            BlockNeighborhood block,
            TileModelView target,
            MapColorAccumulator mapColor,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float cx, float cy, float cz,
            float dx, float dy, float dz
    ) {
        Face face = element.getFaces().get(direction);
        if (face == null) {
            return false;
        }
        FaceLighting.Sample light = FaceLighting.sample(
                block, direction, variant, element.getLightEmission()
        );
        float upward = upwardNormal(direction, element, variant);
        if (!visible(face, upward, light, block, variant, settings)) {
            return false;
        }
        ResolvedBlockMaterial.Face replacement = replacement(
                child, material, substituteMaterial, face, direction, element, variant
        );
        Key textureKey = replacement == null
                ? textureKey(model, face, child.textures()) : replacement.texture();
        Texture texture = textureKey == null
                ? null : resourcePack.getTextures().get(textureKey);
        if (texture == null) {
            return false;
        }

        int start = target.add(2);
        TileModel mesh = target.getTileModel();
        mesh.setPositions(start, ax, ay, az, bx, by, bz, cx, cy, cz);
        mesh.setPositions(start + 1, ax, ay, az, cx, cy, cz, dx, dy, dz);
        setUvs(mesh, start, face.getUv(), face.getRotation(), direction, variant);

        int textureIndex = textures.get(textureKey);
        mesh.setMaterialIndex(start, textureIndex);
        mesh.setMaterialIndex(start + 1, textureIndex);
        int argb = replacement == null ? 0xFFFF_FFFF : replacement.argb();
        float red = ((argb >>> 16) & 0xFF) / 255F;
        float green = ((argb >>> 8) & 0xFF) / 255F;
        float blue = (argb & 0xFF) / 255F;
        mesh.setColor(start, red, green, blue);
        mesh.setColor(start + 1, red, green, blue);
        float aoA = ambientOcclusion(model, element, direction, block, variant,
                ax, ay, az);
        float aoB = ambientOcclusion(model, element, direction, block, variant,
                bx, by, bz);
        float aoC = ambientOcclusion(model, element, direction, block, variant,
                cx, cy, cz);
        float aoD = ambientOcclusion(model, element, direction, block, variant,
                dx, dy, dz);
        mesh.setAOs(start, aoA, aoB, aoC);
        mesh.setAOs(start + 1, aoA, aoC, aoD);

        mesh.setSunlight(start, light.sunlight());
        mesh.setSunlight(start + 1, light.sunlight());
        mesh.setBlocklight(start, light.emissiveBlocklight());
        mesh.setBlocklight(start + 1, light.emissiveBlocklight());

        if (upward > 0.01F) {
            mapColor.add(texture, argb, light, settings.getAmbientLight());
        }
        return true;
    }

    private static ResolvedBlockMaterial.Face replacement(
            ChildShellCatalog.Child child,
            ResolvedBlockMaterial material,
            boolean substituteMaterial,
            Face face,
            Direction direction,
            Element element,
            Variant variant
    ) {
        return requiresReplacement(child, substituteMaterial, face)
                ? material.face(transformedDirection(direction, element, variant)) : null;
    }

    private static boolean requiresReplacement(
            ChildShellCatalog.Child child,
            boolean substituteMaterial,
            Face face
    ) {
        return substituteMaterial && child.material() && child.name().equals(
                face.getTexture().getReferenceName());
    }

    private static Key textureKey(Model model, Face face, Map<String, Key> overrides) {
        String reference = face.getTexture().getReferenceName();
        if (reference != null && overrides.containsKey(reference)) {
            return overrides.get(reference);
        }
        ResourcePath<Texture> path = face.getTexture()
                .getTexturePath(model.getTextures()::get);
        return path;
    }

    private float ambientOcclusion(
            Model model,
            Element element,
            Direction direction,
            BlockNeighborhood block,
            Variant variant,
            float x,
            float y,
            float z
    ) {
        return model.isAmbientocclusion()
                ? testAo(x, y, z, direction, block, variant) : 1F;
    }

    static boolean visible(
            Face face,
            float upward,
            FaceLighting.Sample light,
            BlockNeighborhood block,
            Variant variant,
            RenderSettings settings
    ) {
        if (settings.isRenderTopOnly() && upward < 0.01F) {
            return false;
        }
        if (face.getCullface() != null) {
            ExtendedBlock neighbor = relativeBlock(block, face.getCullface(), variant);
            if (neighbor.getProperties().isCulling()
                    || (neighbor.getProperties().getCullingIdentical()
                    && neighbor.getBlockState().equals(block.getBlockState()))) {
                return false;
            }
        }
        return !block.isRemoveIfCave()
                || (settings.isCaveDetectionUsesBlockLight()
                        ? Math.max(light.blocklight(), light.sunlight())
                        : light.sunlight()) != 0;
    }

    private static float upwardNormal(
            Direction direction,
            Element element,
            Variant variant
    ) {
        return variantVector(direction, element, variant).y();
    }

    static float testAo(
            float x,
            float y,
            float z,
            Direction direction,
            BlockNeighborhood block,
            Variant variant
    ) {
        int offsetX = boundaryOffset(x);
        int offsetY = boundaryOffset(y);
        int offsetZ = boundaryOffset(z);
        var normal = direction.toVector();
        int occluding = 0;
        if (offsetX * normal.getX() + offsetY * normal.getY() > 0
                && relativeBlock(block, offsetX, offsetY, 0, variant)
                .getProperties().isOccluding()) {
            occluding++;
        }
        if (offsetX * normal.getX() + offsetZ * normal.getZ() > 0
                && relativeBlock(block, offsetX, 0, offsetZ, variant)
                .getProperties().isOccluding()) {
            occluding++;
        }
        if (offsetY * normal.getY() + offsetZ * normal.getZ() > 0
                && relativeBlock(block, 0, offsetY, offsetZ, variant)
                .getProperties().isOccluding()) {
            occluding++;
        }
        if (offsetX * normal.getX() + offsetY * normal.getY()
                + offsetZ * normal.getZ() > 0
                && relativeBlock(block, offsetX, offsetY, offsetZ, variant)
                .getProperties().isOccluding()) {
            occluding++;
        }
        return 1F - Math.min(occluding, 3) * 0.25F;
    }

    private static int boundaryOffset(float coordinate) {
        if (coordinate == 16F) {
            return 1;
        }
        return coordinate == 0F ? -1 : 0;
    }

    private static ExtendedBlock relativeBlock(
            BlockNeighborhood block,
            Direction direction,
            Variant variant
    ) {
        var vector = direction.toVector();
        return relativeBlock(
                block, vector.getX(), vector.getY(), vector.getZ(), variant
        );
    }

    private static ExtendedBlock relativeBlock(
            BlockNeighborhood block,
            int x,
            int y,
            int z,
            Variant variant
    ) {
        Vec relative = variantVector(new Vec(x, y, z), variant);
        return block.getNeighborBlock(
                Math.round(relative.x()),
                Math.round(relative.y()),
                Math.round(relative.z())
        );
    }

    private static void setUvs(
            TileModel mesh,
            int start,
            Vector4f raw,
            int rotationDegrees,
            Direction direction,
            Variant variant
    ) {
        float[][] base = {
                {raw.getX() / 16F, raw.getW() / 16F},
                {raw.getZ() / 16F, raw.getW() / 16F},
                {raw.getZ() / 16F, raw.getY() / 16F},
                {raw.getX() / 16F, raw.getY() / 16F}
        };
        int rotation = Math.floorMod(Math.floorDiv(rotationDegrees, 90), 4);
        List<float[]> uv = new ArrayList<>(4);
        for (int index = 0; index < 4; index++) {
            float[] point = base[(rotation + index) % 4].clone();
            if (variant.isUvlock() && variant.isTransformed()) {
                rotateUv(point, uvLockRotation(direction, variant));
            }
            uv.add(point);
        }
        mesh.setUvs(start,
                uv.get(0)[0], uv.get(0)[1], uv.get(1)[0], uv.get(1)[1],
                uv.get(2)[0], uv.get(2)[1]);
        mesh.setUvs(start + 1,
                uv.get(0)[0], uv.get(0)[1], uv.get(2)[0], uv.get(2)[1],
                uv.get(3)[0], uv.get(3)[1]);
    }

    private static void rotateUv(float[] uv, float radians) {
        float cosine = (float) Math.cos(radians);
        float sine = (float) Math.sin(radians);
        float x = uv[0] - 0.5F;
        float y = uv[1] - 0.5F;
        uv[0] = cosine * x - sine * y + 0.5F;
        uv[1] = sine * x + cosine * y + 0.5F;
    }

    /** BlueMap 5.23 feature-backport's UV-lock counter-rotation oracle. */
    static float uvLockRotation(Direction direction, Variant variant) {
        if (!variant.isTransformed()) {
            return 0F;
        }
        Vec normal = variantVector(direction, variant);
        Vec localUp = variantVector(direction.getLocalUp(), variant);
        Vec worldUp = new Vec(0F, 1F, 0F);
        float projection = dot(worldUp, normal);
        Vec projected = new Vec(
                -normal.x() * projection,
                1F - normal.y() * projection,
                -normal.z() * projection
        );
        if (lengthSquared(projected) < 0.01F) {
            projected = vector(normal.y() > 0F
                    ? Direction.UP.getLocalUp() : Direction.DOWN.getLocalUp());
        } else {
            projected = normalize(projected);
        }
        return (float) Math.atan2(
                dot(cross(localUp, projected), normal), dot(localUp, projected)
        );
    }

    static Direction transformedDirection(Direction direction, Variant variant) {
        Vec vector = variantVector(direction, variant);
        return nearestDirection(vector);
    }

    private static Direction transformedDirection(
            Direction direction,
            Element element,
            Variant variant
    ) {
        return nearestDirection(variantVector(direction, element, variant));
    }

    private static Direction nearestDirection(Vec vector) {
        float x = Math.abs(vector.x());
        float y = Math.abs(vector.y());
        float z = Math.abs(vector.z());
        if (x > y && x > z) {
            return vector.x() > 0F ? Direction.EAST : Direction.WEST;
        }
        if (y > z) {
            return vector.y() > 0F ? Direction.UP : Direction.DOWN;
        }
        return vector.z() > 0F ? Direction.SOUTH : Direction.NORTH;
    }

    private static Vec variantVector(Direction direction, Variant variant) {
        return variantVector(vector(direction), variant);
    }

    private static Vec variantVector(
            Direction direction,
            Element element,
            Variant variant
    ) {
        MatrixM4f elementMatrix = element.getRotation().getMatrix();
        Vec local = vector(direction);
        Vec rotated = new Vec(
                elementMatrix.m00 * local.x() + elementMatrix.m01 * local.y()
                        + elementMatrix.m02 * local.z(),
                elementMatrix.m10 * local.x() + elementMatrix.m11 * local.y()
                        + elementMatrix.m12 * local.z(),
                elementMatrix.m20 * local.x() + elementMatrix.m21 * local.y()
                        + elementMatrix.m22 * local.z()
        );
        return variantVector(rotated, variant);
    }

    private static Vec variantVector(Vec vector, Variant variant) {
        MatrixM4f matrix = variant.getTransformMatrix();
        return new Vec(
                matrix.m00 * vector.x() + matrix.m01 * vector.y() + matrix.m02 * vector.z(),
                matrix.m10 * vector.x() + matrix.m11 * vector.y() + matrix.m12 * vector.z(),
                matrix.m20 * vector.x() + matrix.m21 * vector.y() + matrix.m22 * vector.z()
        );
    }

    private static Vec vector(Direction direction) {
        var raw = direction.toVector();
        return new Vec(raw.getX(), raw.getY(), raw.getZ());
    }

    private static float dot(Vec left, Vec right) {
        return left.x() * right.x() + left.y() * right.y() + left.z() * right.z();
    }

    private static Vec cross(Vec left, Vec right) {
        return new Vec(
                left.y() * right.z() - left.z() * right.y(),
                left.z() * right.x() - left.x() * right.z(),
                left.x() * right.y() - left.y() * right.x()
        );
    }

    private static float lengthSquared(Vec vector) {
        return dot(vector, vector);
    }

    private static Vec normalize(Vec vector) {
        float inverse = 1F / (float) Math.sqrt(lengthSquared(vector));
        return new Vec(vector.x() * inverse, vector.y() * inverse, vector.z() * inverse);
    }

    private record Vec(float x, float y, float z) {
    }
}
