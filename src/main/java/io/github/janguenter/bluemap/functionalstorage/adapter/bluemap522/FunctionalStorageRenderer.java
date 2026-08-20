/*
 * SPDX-License-Identifier: MIT
 */

package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap522;

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.MaxCapacityReachedException;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.map.hires.block.BlockRenderer;
import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.map.hires.block.ResourceModelRenderer;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import io.github.janguenter.bluemap.functionalstorage.model.ChildShellCatalog;
import io.github.janguenter.bluemap.functionalstorage.model.FramedMaterialSnapshot;
import io.github.janguenter.bluemap.functionalstorage.model.FramedMaterialSnapshotDecoder;
import io.github.janguenter.bluemap.functionalstorage.profile.FunctionalStorageProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Exact framed child-shell renderer with whole-block stock fallback. */
final class FunctionalStorageRenderer implements BlockRenderer {

    private final ResourcePack resourcePack;
    private final FunctionalStorageRuntime runtime;
    private final ResourceModelRenderer stock;
    private final ChildShellEmitter emitter;
    private final BlockMaterialResolver materials;
    private final FramedMaterialSnapshotDecoder decoder =
            new FramedMaterialSnapshotDecoder();

    FunctionalStorageRenderer(
            ResourcePack resourcePack,
            TextureGallery textures,
            RenderSettings settings,
            FunctionalStorageRuntime runtime
    ) {
        this.resourcePack = resourcePack;
        this.runtime = runtime;
        this.stock = new ResourceModelRenderer(resourcePack, textures, settings);
        this.emitter = new ChildShellEmitter(resourcePack, textures, settings);
        BlockItemDefaultStateResolver itemResolver =
                BlockItemDefaultStateResolver.createVerified();
        this.materials = itemResolver == null
                ? null : new BlockMaterialResolver(resourcePack, itemResolver);
    }

    @Override
    public void render(
            BlockNeighborhood block,
            Variant ignored,
            TileModelView target,
            Color mapColor
    ) {
        int start = target.getStart();
        Color initialMapColor = new Color().set(mapColor);
        if (!runtime.active()) {
            renderStock(block, target, mapColor);
            return;
        }
        try {
            if (!renderExact(block, target, mapColor)) {
                resetAndRenderStock(block, target, start, mapColor, initialMapColor);
            }
        } catch (MaxCapacityReachedException exception) {
            resetPartialGeometry(target, start, mapColor, initialMapColor);
            throw exception;
        } catch (RuntimeException | LinkageError exception) {
            runtime.report("render-failed-" + exception.getClass().getSimpleName());
            resetAndRenderStock(block, target, start, mapColor, initialMapColor);
        }
    }

    private boolean renderExact(
            BlockNeighborhood block,
            TileModelView target,
            Color mapColor
    ) {
        if (!ordinaryHostProperties(block.getProperties())) {
            return false;
        }
        BlockState hostState = block.getBlockState();
        String hostId = hostState.getId().getFormatted();
        FunctionalStorageBlockEntityData data = block.getBlockEntity()
                instanceof FunctionalStorageBlockEntityData found ? found : null;
        Optional<FramedMaterialSnapshot> decoded = decoder.decode(hostState, data);
        FunctionalStorageProfile.Host profile = FunctionalStorageProfile.HOSTS.get(hostId);
        ChildShellCatalog catalog = runtime.catalog();
        List<ChildShellCatalog.Child> children = catalog == null
                ? null : catalog.children(hostId);
        InstalledVariants variants = selectedVariants(block, profile);
        if (decoded.isEmpty() || profile == null || children == null
                || children.isEmpty() || variants == null) {
            return false;
        }

        FramedMaterialSnapshot snapshot = decoded.orElseThrow();
        Map<String, ResolvedBlockMaterial> substitutions;
        if (snapshot.isNative()) {
            substitutions = Map.of();
        } else {
            if (materials == null) {
                runtime.report("minecraft-blockitem-boundary-unavailable");
                return false;
            }
            if (materials.resolve(snapshot.particle(), block).isEmpty()) {
                return false;
            }
            Optional<ResolvedBlockMaterial> side =
                    materials.resolve(snapshot.side(), block);
            Optional<ResolvedBlockMaterial> front =
                    materials.resolve(snapshot.front(), block);
            Optional<ResolvedBlockMaterial> divider =
                    materials.resolve(snapshot.frontDivider(), block);
            if (side.isEmpty() || front.isEmpty() || divider.isEmpty()) {
                return false;
            }
            substitutions = new HashMap<>();
            substitutions.put("side", side.orElseThrow());
            substitutions.put("front", front.orElseThrow());
            if (profile.needsDivider()) {
                substitutions.put("front_divider", divider.orElseThrow());
            }
        }

        MapColorAccumulator mainColors = new MapColorAccumulator();
        if (!emitInstalledChildren(
                snapshot, children, substitutions,
                (child, material, nativeDesign) -> nativeDesign
                        ? emitter.emitNative(
                                child, variants.main(), block, target, mainColors)
                        : emitter.emitStyled(
                                child, material, variants.main(), block, target, mainColors)
        )) {
            return false;
        }
        Color mainColor = new Color();
        mainColors.finish(mainColor);
        Color lockColor = null;
        if (variants.lock() != null) {
            lockColor = new Color().set(0F, 0F, 0F, 0F, true);
            Color finalLockColor = lockColor;
            renderAtFreshBoundary(target, () -> stock.render(
                    block, variants.lock(), target, finalLockColor
            ));
        }
        MapColorAccumulator.combineVariants(mapColor, mainColor, lockColor);
        return true;
    }

    static boolean emitInstalledChildren(
            FramedMaterialSnapshot snapshot,
            List<ChildShellCatalog.Child> children,
            Map<String, ResolvedBlockMaterial> substitutions,
            InstalledChildEmitter emission
    ) {
        if (snapshot == null || children == null || children.isEmpty()
                || substitutions == null || emission == null) {
            return false;
        }
        for (ChildShellCatalog.Child child : children) {
            if (!emission.emit(
                    child, substitutions.get(child.name()), snapshot.isNative())) {
                return false;
            }
        }
        return true;
    }

    private InstalledVariants selectedVariants(
            BlockNeighborhood block,
            FunctionalStorageProfile.Host profile
    ) {
        if (profile == null) {
            return null;
        }
        de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState raw =
                resourcePack.getBlockStates().get(block.getBlockState().getId());
        if (raw == null) {
            return null;
        }
        List<Variant> selected = new ArrayList<>();
        raw.forEach(
                block.getBlockState(), block.getX(), block.getY(), block.getZ(),
                selected::add
        );
        return selectedVariants(block.getBlockState(), profile, selected);
    }

    static InstalledVariants selectedVariants(
            BlockState hostState,
            FunctionalStorageProfile.Host profile,
            List<Variant> selected
    ) {
        if (hostState == null || profile == null || selected == null) {
            return null;
        }
        Key expectedMain = FunctionalStorageProfile.blockModel(
                hostState.getId().getFormatted()
        );
        List<Variant> main = selected.stream()
                .filter(variant -> expectedMain.equals(variant.getModel()))
                .toList();
        List<Variant> lock = selected.stream()
                .filter(variant -> FunctionalStorageProfile.LOCK_MODEL.equals(
                        variant.getModel()))
                .toList();
        if (main.size() != 1 || selected.size() != main.size() + lock.size()) {
            return null;
        }
        Variant mainVariant = main.get(0);
        if (!ordinary(mainVariant)
                || mainVariant.isUvlock() == profile.drawerFamily()) {
            return null;
        }
        if (!profile.drawerFamily()) {
            return lock.isEmpty() ? new InstalledVariants(mainVariant, null) : null;
        }
        boolean locked = "true".equals(hostState.getProperties().get("locked"));
        if (lock.size() != (locked ? 1 : 0)) {
            return null;
        }
        Variant lockVariant = lock.isEmpty() ? null : lock.get(0);
        if (lockVariant != null
                && (!ordinary(lockVariant) || !sameTransform(mainVariant, lockVariant))) {
            return null;
        }
        return new InstalledVariants(mainVariant, lockVariant);
    }

    static boolean ordinary(Variant variant) {
        return variant != null
                && variant.getRenderer() == BlockRendererType.DEFAULT
                && Double.compare(variant.getWeight(), 1D) == 0
                && quarterTurn(variant.getX())
                && quarterTurn(variant.getY())
                && quarterTurn(variant.getZ());
    }

    static boolean ordinaryHostProperties(
            de.bluecolored.bluemap.core.world.BlockProperties properties
    ) {
        return properties != null && !properties.isRandomOffset();
    }

    private static boolean sameTransform(Variant left, Variant right) {
        return Float.compare(left.getX(), right.getX()) == 0
                && Float.compare(left.getY(), right.getY()) == 0
                && Float.compare(left.getZ(), right.getZ()) == 0
                && left.isUvlock() == right.isUvlock();
    }

    private static boolean quarterTurn(float angle) {
        return Float.isFinite(angle)
                && Math.abs(angle / 90F - Math.round(angle / 90F)) <= 1.0E-6F;
    }

    private void resetAndRenderStock(
            BlockNeighborhood block,
            TileModelView target,
            int start,
            Color mapColor,
            Color initialMapColor
    ) {
        resetPartialGeometry(target, start, mapColor, initialMapColor);
        try {
            renderStock(block, target, mapColor);
        } catch (MaxCapacityReachedException exception) {
            resetPartialGeometry(target, start, mapColor, initialMapColor);
            throw exception;
        }
    }

    static void resetPartialGeometry(
            TileModelView target,
            int start,
            Color mapColor,
            Color initialMapColor
    ) {
        target.getTileModel().reset(start);
        target.initialize(start);
        mapColor.set(initialMapColor);
    }

    static void renderAtFreshBoundary(TileModelView target, Runnable renderVariant) {
        target.initialize();
        renderVariant.run();
    }

    private void renderStock(
            BlockNeighborhood block,
            TileModelView target,
            Color mapColor
    ) {
        de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState raw =
                resourcePack.getBlockStates().get(block.getBlockState().getId());
        if (raw == null) {
            return;
        }
        List<Color> variantColors = new ArrayList<>();
        raw.forEach(
                block.getBlockState(), block.getX(), block.getY(), block.getZ(),
                variant -> {
                    Color variantColor = new Color().set(0F, 0F, 0F, 0F, true);
                    renderAtFreshBoundary(target, () -> stock.render(
                            block, variant, target, variantColor
                    ));
                    variantColors.add(variantColor);
                }
        );
        MapColorAccumulator.combineVariants(
                mapColor, variantColors.toArray(Color[]::new)
        );
    }

    record InstalledVariants(Variant main, Variant lock) {
    }

    @FunctionalInterface
    interface InstalledChildEmitter {

        boolean emit(
                ChildShellCatalog.Child child,
                ResolvedBlockMaterial material,
                boolean nativeDesign
        );
    }
}
