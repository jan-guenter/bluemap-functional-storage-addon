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
        this.emitter = new ChildShellEmitter(resourcePack, textures);
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
            resetPartial(target, start, mapColor, initialMapColor);
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
        if (materials == null) {
            runtime.report("minecraft-blockitem-boundary-unavailable");
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
        if (materials.resolve(snapshot.particle(), block).isEmpty()) {
            return false;
        }
        Optional<ResolvedBlockMaterial> side = materials.resolve(snapshot.side(), block);
        Optional<ResolvedBlockMaterial> front = materials.resolve(snapshot.front(), block);
        if (side.isEmpty() || front.isEmpty()) {
            return false;
        }
        Map<String, ResolvedBlockMaterial> substitutions = new HashMap<>();
        substitutions.put("side", side.orElseThrow());
        substitutions.put("front", front.orElseThrow());
        if (profile.needsDivider()) {
            Optional<ResolvedBlockMaterial> divider =
                    materials.resolve(snapshot.frontDivider(), block);
            if (divider.isEmpty()) {
                return false;
            }
            substitutions.put("front_divider", divider.orElseThrow());
        }

        int customStart = target.getTileModel().size();
        for (ChildShellCatalog.Child child : children) {
            ResolvedBlockMaterial material = substitutions.get(child.name());
            if (!emitter.emit(
                    child, material, variants.main(), block, target, mapColor)) {
                return false;
            }
        }
        if (variants.lock() != null) {
            stock.render(block, variants.lock(), target, mapColor);
        }
        return target.getTileModel().size() > customStart;
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
        Key expectedMain = FunctionalStorageProfile.blockModel(
                block.getBlockState().getId().getFormatted()
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
        boolean locked = "true".equals(block.getBlockState().getProperties().get("locked"));
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
        resetPartial(target, start, mapColor, initialMapColor);
        try {
            renderStock(block, target, mapColor);
        } catch (MaxCapacityReachedException exception) {
            resetPartial(target, start, mapColor, initialMapColor);
            throw exception;
        }
    }

    private void resetPartial(
            TileModelView target,
            int start,
            Color mapColor,
            Color initialMapColor
    ) {
        target.getTileModel().reset(start);
        target.initialize(start);
        mapColor.set(initialMapColor);
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
        raw.forEach(
                block.getBlockState(), block.getX(), block.getY(), block.getZ(),
                variant -> stock.render(block, variant, target, mapColor)
        );
    }

    private record InstalledVariants(Variant main, Variant lock) {
    }
}
