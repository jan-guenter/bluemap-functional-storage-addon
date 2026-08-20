/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap522;

import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePackExtension;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.VariantSet;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variants;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.BlockProperties;
import de.bluecolored.bluemap.core.world.BlockState;
import io.github.janguenter.bluemap.functionalstorage.model.ChildShellCatalog;
import io.github.janguenter.bluemap.functionalstorage.profile.ExactArtifactDetector;
import io.github.janguenter.bluemap.functionalstorage.profile.FunctionalStorageProfile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Dual exact-artifact activation and narrow synthetic host routing. */
final class FunctionalStorageResourceExtension implements ResourcePackExtension {

    static final Key SYNTHETIC =
            Key.parse("bluemap_functionalstorage:framed_shell");
    private static final int MAX_ROOTS = 4096;

    private final ResourcePack resourcePack;
    private final FunctionalStorageRuntime runtime;

    FunctionalStorageResourceExtension(
            ResourcePack resourcePack,
            FunctionalStorageRuntime runtime
    ) {
        this.resourcePack = resourcePack;
        this.runtime = runtime;
    }

    @Override
    public void loadResources(Iterable<Path> roots) {
        if (Boolean.getBoolean("bluemap.functionalstorage.disabled")) {
            runtime.inactive("operator-disabled");
            return;
        }
        List<Path> bounded = boundedRoots(roots);
        if (bounded == null) {
            runtime.inactive("artifact-root-limit");
            return;
        }
        Optional<Path> functionalStorage = ExactArtifactDetector.find(
                bounded,
                "functionalstorage",
                FunctionalStorageProfile.FUNCTIONAL_STORAGE_SIZE,
                FunctionalStorageProfile.FUNCTIONAL_STORAGE_SHA256
        );
        if (functionalStorage.isEmpty()) {
            runtime.inactive("exact-functional-storage-artifact-not-found");
            return;
        }
        if (ExactArtifactDetector.find(
                bounded,
                "titanium",
                FunctionalStorageProfile.TITANIUM_SIZE,
                FunctionalStorageProfile.TITANIUM_SHA256
        ).isEmpty()) {
            runtime.inactive("exact-titanium-artifact-not-found");
            return;
        }
        if (!validDispatch(resourcePack.getBlockStates().get(SYNTHETIC))) {
            runtime.inactive("synthetic-dispatch-invalid");
            return;
        }
        try {
            runtime.activate(ChildShellCatalog.load(functionalStorage.orElseThrow()));
        } catch (IOException | RuntimeException exception) {
            runtime.inactive("installed-child-shell-invalid");
        }
    }

    @Override
    public Set<Key> collectUsedTextureKeys() {
        ChildShellCatalog catalog = runtime.catalog();
        return runtime.active() && catalog != null
                ? catalog.collectInstalledTextures(resourcePack) : Set.of();
    }

    @Override
    public void bake() {
        ChildShellCatalog catalog = runtime.catalog();
        if (runtime.active() && (catalog == null || !catalog.validateModels(resourcePack))) {
            runtime.inactive("installed-child-model-invalid");
            return;
        }
        if (runtime.active()) {
            System.out.println("BlueMap Functional Storage add-on active: routed "
                    + FunctionalStorageProfile.HOST_IDS.size()
                    + " exact framed hosts.");
        }
    }

    @Override
    public Key getBlockStateKey(Key key) {
        return runtime.active()
                && FunctionalStorageProfile.HOST_IDS.contains(key.getFormatted())
                ? SYNTHETIC : key;
    }

    @Override
    public void getBlockProperties(BlockState state, BlockProperties.Builder builder) {
        if (runtime.active()
                && FunctionalStorageProfile.HOST_IDS.contains(
                        state.getId().getFormatted())) {
            builder.culling(false).occluding(false).cullingIdentical(false);
        }
    }

    private static List<Path> boundedRoots(Iterable<Path> roots) {
        List<Path> result = new ArrayList<>();
        for (Path root : roots) {
            if (result.size() >= MAX_ROOTS || Thread.currentThread().isInterrupted()) {
                return null;
            }
            result.add(root);
        }
        return List.copyOf(result);
    }

    private static boolean validDispatch(
            de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState state
    ) {
        if (state == null || state.getMultipart() != null) {
            return false;
        }
        Variants variants = state.getVariants();
        if (variants == null || variants.getDefaultVariant() == null) {
            return false;
        }
        VariantSet set = variants.getDefaultVariant();
        return set.getVariants().length == 1
                && BlueMap522Adapter.isExpectedDispatch(set.getVariants()[0]);
    }
}
