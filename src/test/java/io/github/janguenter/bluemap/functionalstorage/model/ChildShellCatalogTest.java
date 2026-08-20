/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.functionalstorage.model;

import io.github.janguenter.bluemap.functionalstorage.profile.FunctionalStorageProfile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChildShellCatalogTest {

    private static ChildShellCatalog catalog;

    @BeforeAll
    static void loadExactInstalledArtifact() throws IOException {
        String path = System.getProperty("functionalStorageJar");
        if (path == null) {
            throw new AssertionError("missing functionalStorageJar");
        }
        catalog = ChildShellCatalog.load(Path.of(path));
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
}
