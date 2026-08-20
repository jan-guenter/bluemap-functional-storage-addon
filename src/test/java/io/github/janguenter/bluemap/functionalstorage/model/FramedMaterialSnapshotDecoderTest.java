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
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FramedMaterialSnapshotDecoderTest {

    private final FramedMaterialSnapshotDecoder decoder =
            new FramedMaterialSnapshotDecoder();

    @Test
    void acceptsExactStrictDrawerCompound() throws ReflectiveOperationException {
        String id = "functionalstorage:framed_2";
        var data = data(id, styled());
        FramedMaterialSnapshot snapshot = decoder.decode(drawer(id), data).orElseThrow();
        assertEquals(FramedMaterialSnapshot.Mode.STYLED, snapshot.mode());
        assertEquals("minecraft:oak_planks", snapshot.particle());
        assertEquals("minecraft:gold_block", snapshot.frontDivider());
    }

    @Test
    void absentAndEmptyCompoundsNormalizeToNativeDesign()
            throws ReflectiveOperationException {
        String id = "functionalstorage:framed_1";
        FramedMaterialSnapshot absent = decoder.decode(
                drawer(id), data(id)
        ).orElseThrow();
        FramedMaterialSnapshot empty = decoder.decode(
                drawer(id), data(id, Map.of())
        ).orElseThrow();

        assertTrue(absent.isNative());
        assertTrue(empty.isNative());
        assertNull(absent.particle());
        assertNull(empty.frontDivider());
    }

    @Test
    void styledCompoundRequiresAllFourCanonicalItemKeys()
            throws ReflectiveOperationException {
        String id = "functionalstorage:framed_1";
        for (String key : styled().keySet()) {
            Map<String, String> incomplete = new LinkedHashMap<>(styled());
            incomplete.remove(key);
            assertTrue(decoder.decode(drawer(id), data(id, incomplete)).isEmpty());
        }
    }

    @Test
    void rejectsTankDisplayAndEveryOtherUnknownCompoundKey()
            throws ReflectiveOperationException {
        String id = "functionalstorage:framed_fluid_2";
        for (String key : new String[]{"tank", "display", "unexpected"}) {
            Map<String, String> adversarial = new LinkedHashMap<>(styled());
            adversarial.put(key, "minecraft:stone");
            assertTrue(decoder.decode(
                    drawer(id), data(id, adversarial)
            ).isEmpty());
        }
    }

    @Test
    void rejectsWrongBlockEntityIdentityAndIllegalState()
            throws ReflectiveOperationException {
        String id = "functionalstorage:framed_1";
        var data = data("functionalstorage:framed_2", styled());
        assertTrue(decoder.decode(drawer(id), data).isEmpty());
        BlockState illegal = new BlockState(Key.parse(id), Map.of(
                "facing", "north", "subfacing", "east", "locked", "false"
        ));
        assertTrue(decoder.decode(illegal,
                data(id, styled())).isEmpty());
    }

    private static BlockState drawer(String id) {
        return new BlockState(Key.parse(id), Map.of(
                "facing", "down", "subfacing", "north", "locked", "false"
        ));
    }

    private static Map<String, String> styled() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("particle", "minecraft:oak_planks");
        result.put("side", "minecraft:oak_planks");
        result.put("front", "minecraft:bricks");
        result.put("front_divider", "minecraft:gold_block");
        return result;
    }

    private static FunctionalStorageBlockEntityData data(String id)
            throws ReflectiveOperationException {
        FunctionalStorageBlockEntityData data = new FunctionalStorageBlockEntityData();
        set(MCABlockEntity.class, data, "id", Key.parse(id));
        return data;
    }

    private static FunctionalStorageBlockEntityData data(
            String id,
            Map<String, String> modelData
    ) throws ReflectiveOperationException {
        FunctionalStorageBlockEntityData data = data(id);
        set(data.getClass(), data, "framedDrawerModelData",
                new LinkedHashMap<>(modelData));
        return data;
    }

    private static void set(Class<?> owner, Object target, String name, Object value)
            throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
