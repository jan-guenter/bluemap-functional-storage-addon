/*
 * SPDX-License-Identifier: MIT
 */

package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap522;

import de.bluecolored.bluemap.core.map.hires.ArrayTileModel;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.AnimationMeta;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.BlockProperties;
import io.github.janguenter.bluemap.functionalstorage.model.ChildShellCatalog;
import io.github.janguenter.bluemap.functionalstorage.model.FramedMaterialSnapshot;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RendererParityContractTest {

    @Test
    void lockedOverlayStartsAfterTheLastCustomChildBoundary()
            throws ReflectiveOperationException {
        ArrayTileModel model = new ArrayTileModel(4);
        TileModelView target = new TileModelView(model);
        addTriangle(target, 1F);
        target.initialize(0);

        FunctionalStorageRenderer.renderAtFreshBoundary(target, () -> {
            addTriangle(target, 2F);
            target.translate(10F, 0F, 0F);
        });

        float[] positions = positions(model);
        assertEquals(1F, positions[0], 0F);
        assertEquals(12F, positions[9], 0F);
    }

    @Test
    void multipartStockVariantsUseIndependentTransformBoundaries()
            throws ReflectiveOperationException {
        ArrayTileModel model = new ArrayTileModel(4);
        TileModelView target = new TileModelView(model);
        addTriangle(target, -5F);

        FunctionalStorageRenderer.renderAtFreshBoundary(target, () -> {
            addTriangle(target, 1F);
            target.translate(10F, 0F, 0F);
        });
        FunctionalStorageRenderer.renderAtFreshBoundary(target, () -> {
            addTriangle(target, 2F);
            target.translate(100F, 0F, 0F);
        });

        float[] positions = positions(model);
        assertEquals(-5F, positions[0], 0F);
        assertEquals(11F, positions[9], 0F);
        assertEquals(102F, positions[18], 0F);
    }

    @Test
    void mainFaceColorsFlattenAndRestoreMaximumOpacity() throws IOException {
        MapColorAccumulator colors = new MapColorAccumulator();
        colors.add(texture(0xFFFF0000), 0xFFFF_FFFF,
                new FaceLighting.Sample(15, 0, 0), 0F);
        colors.add(texture(0xFF0000FF), 0xFFFF_FFFF,
                new FaceLighting.Sample(15, 0, 0), 0F);
        Color result = new Color();

        colors.finish(result);

        assertEquals(0.5F, result.r, 0.0001F);
        assertEquals(0F, result.g, 0F);
        assertEquals(0.5F, result.b, 0.0001F);
        assertEquals(1F, result.a, 0F);
        assertFalse(result.premultiplied);
        assertTrue(result.r <= 1F && result.g <= 1F && result.b <= 1F);
    }

    @Test
    void mapColorUsesRawBlockLightAndVariantsRestoreMaximumAlpha()
            throws IOException {
        FaceLighting.Sample emissive = new FaceLighting.Sample(0, 3, 15);
        MapColorAccumulator dim = new MapColorAccumulator();
        dim.add(texture(0xFFFFFFFF), 0xFFFF_FFFF, emissive, 0F);
        Color dimResult = new Color();
        dim.finish(dimResult);

        assertEquals(0.2F, dimResult.r, 0.0001F);
        assertEquals(1F, dimResult.a, 0F);
        assertEquals(15, emissive.emissiveBlocklight());

        Color main = new Color().set(0.5F, 0F, 0.5F, 1F, false);
        Color lock = new Color().set(0F, 1F, 0F, 0.5F, false);
        Color combined = new Color();
        MapColorAccumulator.combineVariants(combined, main, lock);

        assertEquals(1F / 3F, combined.r, 0.0001F);
        assertEquals(1F / 3F, combined.g, 0.0001F);
        assertEquals(1F / 3F, combined.b, 0.0001F);
        assertEquals(1F, combined.a, 0F);
    }

    @Test
    void galleryC3DownWestNativeSideSampleRemainsPremultiplied()
            throws IOException {
        Texture framedSide = exactTexture(
                "assets/functionalstorage/textures/block/framed_side.png"
        );
        assertEquals(0.71875F, framedSide.getColorPremultiplied().a, 0.0001F);

        MapColorAccumulator colors = new MapColorAccumulator();
        colors.add(framedSide, 0xFFFF_FFFF,
                new FaceLighting.Sample(15, 0, 0), 0F);
        Color result = new Color();
        colors.finish(result);

        assertFalse(result.premultiplied);
        assertEquals(0.71875F, result.a, 0.0001F);
        assertTrue(result.r >= 0F && result.r <= 1F);
        assertTrue(result.g >= 0F && result.g <= 1F);
        assertTrue(result.b >= 0F && result.b <= 1F);
    }

    @Test
    void nativeDesignPlansEveryInstalledChildWithoutMaterialResolution()
            throws IOException {
        ChildShellCatalog catalog = ChildShellCatalog.load(exactArtifact());
        var children = catalog.children("functionalstorage:framed_storage_controller");
        AtomicInteger emissions = new AtomicInteger();

        boolean success = FunctionalStorageRenderer.emitInstalledChildren(
                FramedMaterialSnapshot.nativeDesign(), children, Map.of(),
                (child, material, nativeDesign) -> {
                    assertTrue(nativeDesign);
                    assertNull(material);
                    emissions.incrementAndGet();
                    return true;
                }
        );

        assertTrue(success);
        assertTrue(emissions.get() > 0);
        assertEquals(children.size(), emissions.get());
    }

    @Test
    void styledDesignKeepsMaterialAndNativeChildrenInTheirOriginalLanes()
            throws IOException {
        ChildShellCatalog catalog = ChildShellCatalog.load(exactArtifact());
        var children = catalog.children("functionalstorage:framed_storage_controller");
        ResolvedBlockMaterial material = material();
        AtomicInteger emissions = new AtomicInteger();

        boolean success = FunctionalStorageRenderer.emitInstalledChildren(
                FramedMaterialSnapshot.styled(
                        "minecraft:oak_planks", "minecraft:oak_planks",
                        "minecraft:bricks", "minecraft:gold_block"
                ),
                children,
                Map.of("front", material, "side", material),
                (child, resolved, nativeDesign) -> {
                    assertFalse(nativeDesign);
                    if (child.material()) {
                        assertEquals(material, resolved);
                    } else {
                        assertNull(resolved);
                    }
                    emissions.incrementAndGet();
                    return true;
                }
        );

        assertTrue(success);
        assertEquals(children.size(), emissions.get());
    }

    @Test
    void atomicFallbackDropsPartialShellAndRestoresMapColor() {
        ArrayTileModel model = new ArrayTileModel(8);
        model.add(2);
        TileModelView target = new TileModelView(model);
        int start = target.getStart();
        target.add(4);
        Color initial = new Color().set(0.2F, 0.3F, 0.4F, 0.5F, true);
        Color changed = new Color().set(0.9F, 0.8F, 0.7F, 1F, true);

        FunctionalStorageRenderer.resetPartialGeometry(
                target, start, changed, initial
        );

        assertEquals(2, model.size());
        assertEquals(0, target.getSize());
        assertEquals(initial.r, changed.r, 0F);
        assertEquals(initial.g, changed.g, 0F);
        assertEquals(initial.b, changed.b, 0F);
        assertEquals(initial.a, changed.a, 0F);
    }

    @Test
    void animatedTargetMaterialAndRandomOffsetHostAreRejected() throws IOException {
        AnimationMeta animation = ResourcesGson.INSTANCE.fromJson(
                "{\"animation\":{\"frametime\":2}}", AnimationMeta.class
        );
        Texture animated = Texture.from(
                Key.parse("test:animated"), image(0xFFFFFFFF), animation
        );

        assertFalse(BlockMaterialResolver.canonicalOpaque(animated));
        assertFalse(FunctionalStorageRenderer.ordinaryHostProperties(
                BlockProperties.builder().randomOffset(true).build()
        ));
        assertTrue(FunctionalStorageRenderer.ordinaryHostProperties(
                BlockProperties.builder().randomOffset(false).build()
        ));
    }

    private static Path exactArtifact() {
        String path = System.getProperty("functionalStorageJar");
        if (path == null) {
            throw new AssertionError("missing functionalStorageJar");
        }
        return Path.of(path);
    }

    private static ResolvedBlockMaterial material() {
        Map<Direction, ResolvedBlockMaterial.Face> faces =
                new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            faces.put(direction, new ResolvedBlockMaterial.Face(
                    Key.parse("minecraft:block/stone"), 0xFFFF_FFFF
            ));
        }
        return new ResolvedBlockMaterial(faces);
    }

    private static Texture texture(int argb) throws IOException {
        return Texture.from(Key.parse("test:color"), image(argb));
    }

    private static Texture exactTexture(String entryName) throws IOException {
        try (JarFile jar = new JarFile(exactArtifact().toFile())) {
            JarEntry entry = jar.getJarEntry(entryName);
            if (entry == null) {
                throw new AssertionError("missing exact texture " + entryName);
            }
            try (InputStream input = jar.getInputStream(entry)) {
                BufferedImage image = ImageIO.read(input);
                if (image == null) {
                    throw new AssertionError("unreadable exact texture " + entryName);
                }
                return Texture.from(Key.parse("functionalstorage:block/framed_side"), image);
            }
        }
    }

    private static BufferedImage image(int argb) {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, argb);
        return image;
    }

    private static void addTriangle(TileModelView target, float x) {
        int face = target.add(1);
        target.getTileModel().setPositions(
                face, x, 0F, 0F, x, 1F, 0F, x, 0F, 1F
        );
    }

    private static float[] positions(ArrayTileModel model)
            throws ReflectiveOperationException {
        Field field = ArrayTileModel.class.getDeclaredField("position");
        field.setAccessible(true);
        return (float[]) field.get(model);
    }
}
