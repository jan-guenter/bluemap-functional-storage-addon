/*
 * SPDX-License-Identifier: MIT
 */

package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap523;

import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.mca.blockentity.BlockEntityType;
import io.github.janguenter.bluemap.addon.adapter.api.bluemap523.RegistryGuard;
import io.github.janguenter.bluemap.addon.adapter.api.bluemap523.ResourceExtensionType;
import io.github.janguenter.bluemap.addon.adapter.api.bluemap523.SyntheticDispatch;
import io.github.janguenter.bluemap.functionalstorage.profile.FunctionalStorageProfile;

import java.util.List;

/** Exact BlueMap 5.23 feature-backport internal ABI registration boundary. */
public final class BlueMap523Adapter {

    private static final FunctionalStorageRuntime RUNTIME =
            FunctionalStorageRuntime.INSTANCE;
    private static final BlockRendererType RENDERER = new BlockRendererType.Impl(
            Key.parse("bluemap_functionalstorage:framed_shell"),
            (pack, gallery, settings) -> new FunctionalStorageRenderer(
                    pack, gallery, settings, RUNTIME
            )
    );
    private static final ResourcePack.Extension<FunctionalStorageResourceExtension> EXTENSION =
            new ResourceExtensionType<>(
                    Key.parse("bluemap_functionalstorage:framed_shell_extension"),
                    pack -> new FunctionalStorageResourceExtension(pack, RUNTIME)
            );
    private static final List<BlockEntityType> BLOCK_ENTITY_TYPES =
            FunctionalStorageProfile.BLOCK_ENTITY_IDS.stream()
                    .sorted()
                    .map(id -> (BlockEntityType) new BlockEntityType.Impl(
                            Key.parse(id), FunctionalStorageBlockEntityData.class
                    ))
                    .toList();

    private BlueMap523Adapter() {
    }

    public static synchronized boolean install() {
        if (!RegistryGuard.canRegister(BlockRendererType.REGISTRY, RENDERER)
                || !RegistryGuard.canRegister(ResourcePack.Extension.REGISTRY, EXTENSION)
                || BLOCK_ENTITY_TYPES.stream()
                        .anyMatch(type -> !RegistryGuard.canRegister(
                                BlockEntityType.REGISTRY, type))) {
            RUNTIME.inactive("registry-collision");
            return false;
        }
        if (!RegistryGuard.register(BlockRendererType.REGISTRY, RENDERER)
                || !RegistryGuard.register(ResourcePack.Extension.REGISTRY, EXTENSION)) {
            RUNTIME.inactive("registry-collision");
            return false;
        }
        for (BlockEntityType type : BLOCK_ENTITY_TYPES) {
            if (!RegistryGuard.register(BlockEntityType.REGISTRY, type)) {
                RUNTIME.inactive("block-entity-registry-collision");
                return false;
            }
        }
        return true;
    }

    static FunctionalStorageResourceExtension extension(ResourcePack pack) {
        return pack.getExtension(EXTENSION);
    }

    static boolean isExpectedDispatch(BlockState state) {
        return SyntheticDispatch.matches(state, RENDERER);
    }
}
