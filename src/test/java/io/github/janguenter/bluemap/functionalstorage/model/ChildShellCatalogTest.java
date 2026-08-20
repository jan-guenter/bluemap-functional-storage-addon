/*
 * SPDX-License-Identifier: MIT
 */

package io.github.janguenter.bluemap.functionalstorage.model;

import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Element;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.AnimationMeta;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Key;
import io.github.janguenter.bluemap.functionalstorage.profile.FunctionalStorageProfile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChildShellCatalogTest {

    private static ChildShellCatalog catalog;
    private static Path artifact;

    @BeforeAll
    static void loadExactInstalledArtifact() throws IOException {
        String path = System.getProperty("functionalStorageJar");
        if (path == null) {
            throw new AssertionError("missing functionalStorageJar");
        }
        artifact = Path.of(path);
        catalog = ChildShellCatalog.load(artifact);
    }

    @Test
    void parsesAllTenExactTopLevelModels() {
        for (String host : FunctionalStorageProfile.HOST_IDS) {
            assertNotNull(catalog.children(host));
            assertEquals(FunctionalStorageProfile.HOSTS.get(host).children(),
                    catalog.children(host).stream()
                            .map(ChildShellCatalog.Child::name)
                            .collect(java.util.stream.Collectors.toSet()));
        }
    }

    @Test
    void fluidTwoPreservesTankAndThreeMaterialChildren() {
        List<ChildShellCatalog.Child> children = catalog.children(
                "functionalstorage:framed_fluid_2"
        );
        assertEquals(List.of("side", "front", "tank", "front_divider"),
                children.stream().map(ChildShellCatalog.Child::name).toList());
        ChildShellCatalog.Child tank = children.stream()
                .filter(child -> child.name().equals("tank"))
                .findFirst().orElseThrow();
        assertFalse(tank.material());
        assertEquals("functionalstorage:block/fluid_inner_2",
                tank.parent().getFormatted());
    }

    @Test
    void controllerDisplayIsNativeAndOtherChildrenAreMaterialDriven() {
        List<ChildShellCatalog.Child> children = catalog.children(
                "functionalstorage:framed_storage_controller"
        );
        assertEquals(Set.of("front", "side"), children.stream()
                .filter(ChildShellCatalog.Child::material)
                .map(ChildShellCatalog.Child::name)
                .collect(java.util.stream.Collectors.toSet()));
        ChildShellCatalog.Child display = children.stream()
                .filter(child -> child.name().equals("display"))
                .findFirst().orElseThrow();
        assertFalse(display.material());
        assertTrue(display.textures().containsKey("front"));
    }

    @Test
    void exactParentStructureMatchesButGeometryOverrideDoesNot() throws IOException {
        Key front = Key.parse("functionalstorage:block/front");
        try (ZipFile zip = new ZipFile(artifact.toFile());
             InputStreamReader reader = exactModelReader(zip, front)) {
            Model exact = ResourcesGson.INSTANCE.fromJson(reader, Model.class);
            assertTrue(catalog.matchesInstalledModelStructure(front, exact));
        }

        assertFalse(catalog.matchesInstalledModelStructure(
                front, new Model(new Element[0])
        ));
    }

    @Test
    void exactChildModelsUseAmbientOcclusionWithoutElementEmissionOrTint()
            throws IOException {
        Set<Key> parents = new LinkedHashSet<>();
        for (String host : FunctionalStorageProfile.HOST_IDS) {
            catalog.children(host).stream()
                    .map(ChildShellCatalog.Child::parent)
                    .forEach(parents::add);
        }

        try (ZipFile zip = new ZipFile(artifact.toFile())) {
            for (Key parent : parents) {
                Model model;
                try (InputStreamReader reader = exactModelReader(zip, parent)) {
                    model = ResourcesGson.INSTANCE.fromJson(reader, Model.class);
                }
                assertTrue(model.isAmbientocclusion(), parent.getFormatted());
                for (Element element : model.getElements()) {
                    assertEquals(0F, element.getRotation().getX(), 0F,
                            parent.getFormatted());
                    assertEquals(0F, element.getRotation().getY(), 0F,
                            parent.getFormatted());
                    assertEquals(0F, element.getRotation().getZ(), 0F,
                            parent.getFormatted());
                    assertEquals(0, element.getLightEmission(),
                            parent.getFormatted());
                    element.getFaces().values().forEach(face -> assertEquals(
                            -1, face.getTintindex(), parent.getFormatted()
                    ));
                }
            }
        }
    }

    @Test
    void exactAnimatedControllerDisplayTexturesRemainNativeAndAllowed()
            throws IOException {
        try (ZipFile zip = new ZipFile(artifact.toFile())) {
            for (String name : List.of(
                    "framed_controller_front", "framed_controller_extension"
            )) {
                String base = "assets/functionalstorage/textures/block/" + name;
                var png = zip.getEntry(base + ".png");
                var metadata = zip.getEntry(base + ".png.mcmeta");
                assertNotNull(png);
                assertNotNull(metadata);
                AnimationMeta animation;
                try (InputStreamReader reader = new InputStreamReader(
                        zip.getInputStream(metadata), StandardCharsets.UTF_8)) {
                    animation = ResourcesGson.INSTANCE.fromJson(
                            reader, AnimationMeta.class
                    );
                }
                Texture texture = Texture.from(
                        Key.parse("functionalstorage:block/" + name),
                        ImageIO.read(zip.getInputStream(png)),
                        animation
                );
                assertNotNull(texture.getAnimation());
                assertTrue(ChildShellCatalog.installedTextureAllowed(texture));
            }
        }
    }

    private static InputStreamReader exactModelReader(ZipFile zip, Key key) {
        String entryName = "assets/" + key.getNamespace() + "/models/"
                + key.getValue() + ".json";
        var entry = zip.getEntry(entryName);
        if (entry == null) {
            throw new AssertionError("missing exact child model " + entryName);
        }
        try {
            return new InputStreamReader(
                    zip.getInputStream(entry), StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new AssertionError("unreadable exact child model " + entryName,
                    exception);
        }
    }
}
