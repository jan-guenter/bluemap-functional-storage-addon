/*
 * SPDX-License-Identifier: MIT
 *
 * Model emission adapts BlueMap's and the owner's Sophisticated add-on's MIT
 * coordinate/UV conventions, extended only for installed child definitions,
 * exact variant transforms, and UV lock.
 */
package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap522;

import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.core.map.TextureGallery;
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
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import io.github.janguenter.bluemap.functionalstorage.model.ChildShellCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Emits one installed ordinary child model with a bounded material override. */
final class ChildShellEmitter {

    private static final float BLOCK_SCALE = 1F / 16F;

    private final ResourcePack resourcePack;
    private final TextureGallery textures;

    ChildShellEmitter(ResourcePack resourcePack, TextureGallery textures) {
        this.resourcePack = resourcePack;
        this.textures = textures;
    }

    boolean emit(
            ChildShellCatalog.Child child,
            ResolvedBlockMaterial material,
            Variant variant,
            BlockNeighborhood block,
            TileModelView target,
            Color mapColor
    ) {
        Model model = resourcePack.getModels().get(child.parent());
        if (!preflight(child, material, model, variant)) {
            return false;
        }
        int modelStart = target.getTileModel().size();
        boolean emitted = false;
        for (Element element : model.getElements()) {
            if (element == null) {
                continue;
            }
            int elementStart = target.getTileModel().size();
            emitted |= emitElement(
                    child, material, model, element, variant, block, target, mapColor
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
        return emitted;
    }

    private boolean preflight(
            ChildShellCatalog.Child child,
            ResolvedBlockMaterial material,
            Model model,
            Variant variant
    ) {
        if (child.material() != (material != null)
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
                        child, material, entry.getValue(), entry.getKey(), variant
                );
                Key texture = replacement == null
                        ? textureKey(model, entry.getValue(), child.textures())
                        : replacement.texture();
                if (texture == null || resourcePack.getTextures().get(texture) == null) {
                    return false;
                }
            }
        }
        return faceFound;
    }

    private boolean emitElement(
            ChildShellCatalog.Child child,
            ResolvedBlockMaterial material,
            Model model,
            Element element,
            Variant variant,
            BlockNeighborhood block,
            TileModelView target,
            Color mapColor
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
        emitted |= emitFace(child, material, model, element, Direction.DOWN,
                variant, block, target, mapColor,
                x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        emitted |= emitFace(child, material, model, element, Direction.UP,
                variant, block, target, mapColor,
                x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        emitted |= emitFace(child, material, model, element, Direction.NORTH,
                variant, block, target, mapColor,
                x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        emitted |= emitFace(child, material, model, element, Direction.SOUTH,
                variant, block, target, mapColor,
                x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        emitted |= emitFace(child, material, model, element, Direction.WEST,
                variant, block, target, mapColor,
                x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        emitted |= emitFace(child, material, model, element, Direction.EAST,
                variant, block, target, mapColor,
                x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
        return emitted;
    }

    private boolean emitFace(
            ChildShellCatalog.Child child,
            ResolvedBlockMaterial material,
            Model model,
            Element element,
            Direction direction,
            Variant variant,
            BlockNeighborhood block,
            TileModelView target,
            Color mapColor,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float cx, float cy, float cz,
            float dx, float dy, float dz
    ) {
        Face face = element.getFaces().get(direction);
        if (face == null) {
            return false;
        }
        ResolvedBlockMaterial.Face replacement = replacement(
                child, material, face, direction, variant
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
        mesh.setAOs(start, 1F, 1F, 1F);
        mesh.setAOs(start + 1, 1F, 1F, 1F);

        FaceLighting.Sample light = FaceLighting.sample(
                block, direction, variant, element.getLightEmission()
        );
        mesh.setSunlight(start, light.sunlight());
        mesh.setSunlight(start + 1, light.sunlight());
        mesh.setBlocklight(start, light.blocklight());
        mesh.setBlocklight(start + 1, light.blocklight());

        if (transformedDirection(direction, variant) == Direction.UP) {
            Color average = new Color().set(texture.getColorPremultiplied());
            average.r *= red;
            average.g *= green;
            average.b *= blue;
            mapColor.add(average);
        }
        return true;
    }

    private static ResolvedBlockMaterial.Face replacement(
            ChildShellCatalog.Child child,
            ResolvedBlockMaterial material,
            Face face,
            Direction direction,
            Variant variant
    ) {
        return child.material() && child.name().equals(
                face.getTexture().getReferenceName())
                ? material.face(transformedDirection(direction, variant)) : null;
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

    /** BlueMap 5.22's UV-lock counter-rotation oracle. */
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
        Vec vector = vector(direction);
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
