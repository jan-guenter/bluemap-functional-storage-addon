/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap522;

import de.bluecolored.bluemap.core.world.mca.MCAUtil;
import de.bluecolored.bluenbt.BlueNBT;
import de.bluecolored.bluenbt.NBTWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunctionalStorageBlockEntityDataTest {

    @Test
    void blueNbtPreservesAnExplicitlyEmptyDesignCompound() throws IOException {
        FunctionalStorageBlockEntityData decoded = roundTrip(Map.of());

        assertNotNull(decoded.framedDrawerModelData());
        assertTrue(decoded.framedDrawerModelData().isEmpty());
    }

    @Test
    void blueNbtEnumeratesEveryDesignCompoundKeyIncludingUnknownOnes()
            throws IOException {
        Map<String, String> persisted = new LinkedHashMap<>();
        persisted.put("particle", "minecraft:oak_planks");
        persisted.put("side", "minecraft:oak_planks");
        persisted.put("front", "minecraft:bricks");
        persisted.put("front_divider", "minecraft:gold_block");
        persisted.put("tank", "minecraft:water");
        persisted.put("display", "minecraft:glass");
        persisted.put("unexpected", "minecraft:stone");

        FunctionalStorageBlockEntityData decoded = roundTrip(persisted);

        assertEquals(persisted, decoded.framedDrawerModelData());
    }

    private static FunctionalStorageBlockEntityData roundTrip(
            Map<String, String> modelData
    ) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (NBTWriter writer = new NBTWriter(bytes)) {
            writer.beginCompound();
            writer.name("id").value("functionalstorage:framed_1");
            writer.name("framedDrawerModelData").beginCompound();
            for (Map.Entry<String, String> entry : modelData.entrySet()) {
                writer.name(entry.getKey()).value(entry.getValue());
            }
            writer.endCompound();
            writer.endCompound();
        }
        BlueNBT blueNbt = MCAUtil.addCommonNbtSettings(new BlueNBT());
        return blueNbt.read(
                new ByteArrayInputStream(bytes.toByteArray()),
                FunctionalStorageBlockEntityData.class
        );
    }
}
