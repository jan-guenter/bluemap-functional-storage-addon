/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.functionalstorage.model;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.mca.blockentity.MCABlockEntity;
import io.github.janguenter.bluemap.functionalstorage.adapter.bluemap522.FunctionalStorageBlockEntityData;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FramedMaterialSnapshotDecoderTest {

    private final FramedMaterialSnapshotDecoder decoder =
            new FramedMaterialSnapshotDecoder();

    @Test
    void acceptsExactStrictDrawerCompound() throws ReflectiveOperationException {
        String id = "functionalstorage:framed_2";
        var data = data(id, "minecraft:oak_planks", "minecraft:oak_planks",
                "minecraft:bricks", "minecraft:gold_block");
        FramedMaterialSnapshot snapshot = decoder.decode(drawer(id), data).orElseThrow();
        assertEquals("minecraft:oak_planks", snapshot.particle());
        assertEquals("minecraft:gold_block", snapshot.frontDivider());
    }

    @Test
    void particleIsRequiredEvenThoughItIsNotVisibleGeometry()
            throws ReflectiveOperationException {
        String id = "functionalstorage:framed_1";
        var data = data(id, null, "minecraft:oak_planks",
                "minecraft:bricks", null);
        assertTrue(decoder.decode(drawer(id), data).isEmpty());
    }

    @Test
    void dividerHostRequiresDivider() throws ReflectiveOperationException {
        String id = "functionalstorage:framed_fluid_4";
        var data = data(id, "minecraft:oak_planks", "minecraft:oak_planks",
                "minecraft:bricks", null);
        assertTrue(decoder.decode(drawer(id), data).isEmpty());
    }

    @Test
    void rejectsWrongBlockEntityIdentityAndIllegalState()
            throws ReflectiveOperationException {
        String id = "functionalstorage:framed_1";
        var data = data("functionalstorage:framed_2", "minecraft:oak_planks",
                "minecraft:oak_planks", "minecraft:bricks", null);
        assertTrue(decoder.decode(drawer(id), data).isEmpty());
        BlockState illegal = new BlockState(Key.parse(id), Map.of(
                "facing", "north", "subfacing", "east", "locked", "false"
        ));
        assertTrue(decoder.decode(illegal,
                data(id, "minecraft:oak_planks", "minecraft:oak_planks",
                        "minecraft:bricks", null)).isEmpty());
    }

    private static BlockState drawer(String id) {
        return new BlockState(Key.parse(id), Map.of(
                "facing", "down", "subfacing", "north", "locked", "false"
        ));
    }

    private static FunctionalStorageBlockEntityData data(
            String id,
            String particle,
            String side,
            String front,
            String divider
    ) throws ReflectiveOperationException {
        FunctionalStorageBlockEntityData data = new FunctionalStorageBlockEntityData();
        set(MCABlockEntity.class, data, "id", Key.parse(id));
        var raw = new FunctionalStorageBlockEntityData.FramedDrawerModelData();
        set(raw.getClass(), raw, "particle", particle);
        set(raw.getClass(), raw, "side", side);
        set(raw.getClass(), raw, "front", front);
        set(raw.getClass(), raw, "frontDivider", divider);
        set(data.getClass(), data, "framedDrawerModelData", raw);
        return data;
    }

    private static void set(Class<?> owner, Object target, String name, Object value)
            throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
