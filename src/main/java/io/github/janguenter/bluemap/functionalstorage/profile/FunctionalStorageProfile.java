/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.functionalstorage.profile;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.BlockState;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Closed exact-1.5.8 host, block-entity, state and child-shell profile. */
public final class FunctionalStorageProfile {

    public static final long FUNCTIONAL_STORAGE_SIZE = 810_628L;
    public static final String FUNCTIONAL_STORAGE_SHA256 =
            "e3e7368c28a24e7de5b877988aa92cc94ca7417c8f60b7d28e7f584a94a51147";
    public static final long TITANIUM_SIZE = 606_801L;
    public static final String TITANIUM_SHA256 =
            "d224a9bd5cfb9e921ba644b2a7a2ce1041f879a9945f80dacebd87d95888530c";

    public static final String LOADER = "functionalstorage:framedblock";
    public static final Key LOCK_MODEL = Key.parse("functionalstorage:block/lock");
    public static final Map<String, Host> HOSTS = createHosts();
    public static final Set<String> HOST_IDS = Set.copyOf(HOSTS.keySet());
    public static final Set<String> BLOCK_ENTITY_IDS = Set.copyOf(HOSTS.keySet());

    private static final Set<String> DIRECTIONS = Set.of(
            "down", "up", "north", "south", "west", "east"
    );
    private static final Set<String> HORIZONTAL = Set.of(
            "north", "south", "west", "east"
    );

    private FunctionalStorageProfile() {
    }

    public static boolean matches(String hostId, String blockEntityId) {
        return HOSTS.containsKey(hostId) && hostId.equals(blockEntityId);
    }

    public static boolean legalState(BlockState state) {
        if (state == null) {
            return false;
        }
        Host host = HOSTS.get(state.getId().getFormatted());
        if (host == null) {
            return false;
        }
        Map<String, String> properties = state.getProperties();
        if (host.drawerFamily()) {
            if (!properties.keySet().equals(Set.of("facing", "subfacing", "locked"))) {
                return false;
            }
            String facing = properties.get("facing");
            String subfacing = properties.get("subfacing");
            String locked = properties.get("locked");
            if (!DIRECTIONS.contains(facing)
                    || !("true".equals(locked) || "false".equals(locked))) {
                return false;
            }
            return (("down".equals(subfacing) || "up".equals(subfacing))
                    && DIRECTIONS.contains(facing))
                    || (HORIZONTAL.contains(subfacing) && "down".equals(facing));
        }
        return properties.keySet().equals(Set.of("subfacing"))
                && HORIZONTAL.contains(properties.get("subfacing"));
    }

    public static Key blockModel(String hostId) {
        Host host = HOSTS.get(hostId);
        return host == null ? null
                : Key.parse("functionalstorage:block/" + host.path());
    }

    private static Map<String, Host> createHosts() {
        Map<String, Host> result = new LinkedHashMap<>();
        add(result, "framed_1", true, "front", "side");
        add(result, "framed_2", true, "front", "side", "front_divider");
        add(result, "framed_4", true, "front", "side", "front_divider");
        add(result, "compacting_framed_drawer", true,
                "front", "side", "front_divider");
        add(result, "framed_simple_compacting_drawer", true,
                "front", "side", "front_divider");
        add(result, "framed_fluid_1", true, "side", "front", "tank");
        add(result, "framed_fluid_2", true,
                "side", "front", "tank", "front_divider");
        add(result, "framed_fluid_4", true,
                "side", "front", "tank", "front_divider");
        add(result, "framed_storage_controller", false, "front", "side", "display");
        add(result, "framed_controller_extension", false, "front", "side", "display");
        return Map.copyOf(result);
    }

    private static void add(
            Map<String, Host> target,
            String path,
            boolean drawerFamily,
            String... children
    ) {
        String id = "functionalstorage:" + path;
        target.put(id, new Host(path, drawerFamily,
                Set.copyOf(new LinkedHashSet<>(Set.of(children)))));
    }

    /** Exact installed top-level model shape expected for one host. */
    public record Host(String path, boolean drawerFamily, Set<String> children) {

        public boolean needsDivider() {
            return children.contains("front_divider");
        }
    }
}
