/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.functionalstorage.profile;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.BlockState;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunctionalStorageProfileTest {

    @Test
    void freezesExactlyTenSameNamedHostsAndBlockEntities() {
        assertEquals(10, FunctionalStorageProfile.HOST_IDS.size());
        assertEquals(FunctionalStorageProfile.HOST_IDS,
                FunctionalStorageProfile.BLOCK_ENTITY_IDS);
        for (String id : FunctionalStorageProfile.HOST_IDS) {
            assertTrue(FunctionalStorageProfile.matches(id, id));
            assertFalse(FunctionalStorageProfile.matches(id, id + "_wrong"));
        }
    }

    @Test
    void drawerFamilyHasExactlySixteenLegalOrientationPairsPerLockState() {
        String id = "functionalstorage:framed_2";
        Set<String> legal = new HashSet<>();
        for (String facing : Set.of("down", "up", "north", "south", "west", "east")) {
            for (String subfacing
                    : Set.of("down", "up", "north", "south", "west", "east")) {
                BlockState state = state(id, Map.of(
                        "facing", facing, "subfacing", subfacing, "locked", "false"
                ));
                if (FunctionalStorageProfile.legalState(state)) {
                    legal.add(facing + '/' + subfacing);
                }
            }
        }
        assertEquals(16, legal.size());
        assertTrue(FunctionalStorageProfile.legalState(state(id, Map.of(
                "facing", "down", "subfacing", "east", "locked", "true"
        ))));
    }

    @Test
    void drawerRejectsUnknownPropertiesAndIllegalHorizontalPair() {
        String id = "functionalstorage:framed_fluid_1";
        assertFalse(FunctionalStorageProfile.legalState(state(id, Map.of(
                "facing", "north", "subfacing", "east", "locked", "false"
        ))));
        assertFalse(FunctionalStorageProfile.legalState(state(id, Map.of(
                "facing", "down", "subfacing", "north", "locked", "false",
                "active", "true"
        ))));
    }

    @Test
    void controllerHasExactlyFourHorizontalStates() {
        String id = "functionalstorage:framed_storage_controller";
        int legal = 0;
        for (String value : Set.of("down", "up", "north", "south", "west", "east")) {
            if (FunctionalStorageProfile.legalState(
                    state(id, Map.of("subfacing", value)))) {
                legal++;
            }
        }
        assertEquals(4, legal);
        assertFalse(FunctionalStorageProfile.legalState(
                state(id, Map.of("subfacing", "north", "locked", "false"))));
    }

    @Test
    void childMatrixFreezesDividerAndNativeChildren() {
        assertEquals(Set.of("front", "side"),
                FunctionalStorageProfile.HOSTS.get(
                        "functionalstorage:framed_1").children());
        assertEquals(Set.of("side", "front", "tank", "front_divider"),
                FunctionalStorageProfile.HOSTS.get(
                        "functionalstorage:framed_fluid_4").children());
        assertEquals(Set.of("front", "side", "display"),
                FunctionalStorageProfile.HOSTS.get(
                        "functionalstorage:framed_controller_extension").children());
    }

    private static BlockState state(String id, Map<String, String> properties) {
        return new BlockState(Key.parse(id), properties);
    }
}
