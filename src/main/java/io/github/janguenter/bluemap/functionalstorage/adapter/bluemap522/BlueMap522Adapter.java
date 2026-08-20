/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap522;

import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import de.bluecolored.bluemap.core.world.mca.blockentity.BlockEntityType;
import io.github.janguenter.bluemap.functionalstorage.profile.FunctionalStorageProfile;

import java.util.List;

/** BlueMap 5.22 internal ABI registration boundary. */
public final class BlueMap522Adapter {

    private static final FunctionalStorageRuntime RUNTIME =
            FunctionalStorageRuntime.INSTANCE;
    private static final BlockRendererType RENDERER = new BlockRendererType.Impl(
            Key.parse("bluemap_functionalstorage:framed_shell"),
            (pack, gallery, settings) -> new FunctionalStorageRenderer(
                    pack, gallery, settings, RUNTIME
            )
    );
    private static final ResourcePack.Extension<FunctionalStorageResourceExtension> EXTENSION =
            new FunctionalStorageResourceExtensionType(RUNTIME);
    private static final List<BlockEntityType> BLOCK_ENTITY_TYPES =
            FunctionalStorageProfile.BLOCK_ENTITY_IDS.stream()
                    .sorted()
                    .map(id -> (BlockEntityType) new BlockEntityType.Impl(
                            Key.parse(id), FunctionalStorageBlockEntityData.class
                    ))
                    .toList();

    private BlueMap522Adapter() {
    }

    public static synchronized boolean install() {
        if (!canRegister(BlockRendererType.REGISTRY, RENDERER)
                || !canRegister(ResourcePack.Extension.REGISTRY, EXTENSION)
                || BLOCK_ENTITY_TYPES.stream()
                        .anyMatch(type -> !canRegister(BlockEntityType.REGISTRY, type))) {
            RUNTIME.inactive("registry-collision");
            return false;
        }
        if (!register(BlockRendererType.REGISTRY, RENDERER)
                || !register(ResourcePack.Extension.REGISTRY, EXTENSION)) {
            RUNTIME.inactive("registry-collision");
            return false;
        }
        for (BlockEntityType type : BLOCK_ENTITY_TYPES) {
            if (!register(BlockEntityType.REGISTRY, type)) {
                RUNTIME.inactive("block-entity-registry-collision");
                return false;
            }
        }
        return true;
    }

    static FunctionalStorageResourceExtension extension(ResourcePack pack) {
        return pack.getExtension(EXTENSION);
    }

    static boolean isExpectedDispatch(Variant variant) {
        return variant != null
                && variant.getRenderer() == RENDERER
                && ResourcePack.MISSING_BLOCK_MODEL.equals(variant.getModel())
                && !variant.isTransformed()
                && !variant.isUvlock()
                && Double.compare(variant.getWeight(), 1D) == 0;
    }

    private static <T extends Keyed> boolean canRegister(Registry<T> registry, T candidate) {
        T existing = registry.get(candidate.getKey());
        return existing == null || existing == candidate;
    }

    private static <T extends Keyed> boolean register(Registry<T> registry, T candidate) {
        T existing = registry.get(candidate.getKey());
        if (existing == null) {
            registry.register(candidate);
            existing = registry.get(candidate.getKey());
        }
        return existing == candidate;
    }
}
