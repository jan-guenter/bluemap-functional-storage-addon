/*
 * SPDX-License-Identifier: MIT
 */

package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap523;

import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Element;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Face;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Rotation;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.TextureVariable;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdapterAndGeometryContractTest {

    @Test
    void controllerQuarterTurnTransformsMaterialFaceDirectionOnce() {
        Variant east = new Variant(
                new ResourcePath<Model>("functionalstorage:block/framed_storage_controller"),
                0F, 90F, 0F, true, 1D
        );
        assertEquals(Direction.EAST,
                ChildShellEmitter.transformedDirection(Direction.NORTH, east));
        assertEquals(Direction.UP,
                ChildShellEmitter.transformedDirection(Direction.UP, east));
        assertTrue(Float.isFinite(
                ChildShellEmitter.uvLockRotation(Direction.NORTH, east)));
    }

    @Test
    void ordinaryVariantRequiresFiniteQuarterTurnsAndUnitWeight() {
        var model = new ResourcePath<Model>("functionalstorage:block/framed_1");
        assertTrue(FunctionalStorageRenderer.ordinary(
                new Variant(model, 90F, 270F, 0F, false, 1D)));
        assertFalse(FunctionalStorageRenderer.ordinary(
                new Variant(model, 45F, 0F, 0F, false, 1D)));
        assertFalse(FunctionalStorageRenderer.ordinary(
                new Variant(model, 0F, 0F, 0F, false, 2D)));
    }

    @Test
    void materialAdmissionRequiresCanonicalOneLayerFullCube() {
        Variant variant = new Variant(new ResourcePath<Model>("minecraft:block/stone"));
        Model model = cube(-1);
        assertTrue(BlockMaterialResolver.canonicalVariant(variant, model));
        assertFalse(BlockMaterialResolver.canonicalVariant(
                new Variant(new ResourcePath<Model>("minecraft:block/stone"),
                        0F, 90F, 0F), model));
        assertFalse(BlockMaterialResolver.canonicalVariant(variant, cube(2)));
    }

    private static Model cube(int tint) {
        Map<Direction, Face> faces = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            faces.put(direction, new Face(
                    new Vector4f(0F, 0F, 16F, 16F),
                    new TextureVariable(
                            new ResourcePath<Texture>("minecraft:block/stone")),
                    direction, 0, tint
            ));
        }
        Element element = new Element(
                Vector3f.ZERO, new Vector3f(16F, 16F, 16F),
                Rotation.ZERO, true, 0, faces
        );
        return new Model(element);
    }
}
