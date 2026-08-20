/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.functionalstorage.model;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.BlockState;
import io.github.janguenter.bluemap.functionalstorage.adapter.bluemap522.FunctionalStorageBlockEntityData;
import io.github.janguenter.bluemap.functionalstorage.profile.FunctionalStorageProfile;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Converts only the exact persisted compound and exact host/BE/state tuple. */
public final class FramedMaterialSnapshotDecoder {

    private static final Set<String> STYLED_KEYS = Set.of(
            "particle", "side", "front", "front_divider"
    );

    public Optional<FramedMaterialSnapshot> decode(
            BlockState host,
            FunctionalStorageBlockEntityData data
    ) {
        if (host == null || data == null || data.getId() == null
                || !FunctionalStorageProfile.matches(
                        host.getId().getFormatted(), data.getId().getFormatted())
                || !FunctionalStorageProfile.legalState(host)) {
            return Optional.empty();
        }
        if (!FunctionalStorageProfile.HOSTS.containsKey(
                host.getId().getFormatted())) {
            return Optional.empty();
        }
        Map<String, String> raw = data.framedDrawerModelData();
        if (raw == null || raw.isEmpty()) {
            return Optional.of(FramedMaterialSnapshot.nativeDesign());
        }
        if (!raw.keySet().equals(STYLED_KEYS)
                || !itemId(raw.get("particle"))
                || !itemId(raw.get("side"))
                || !itemId(raw.get("front"))
                || !itemId(raw.get("front_divider"))) {
            return Optional.empty();
        }
        return Optional.of(FramedMaterialSnapshot.styled(
                raw.get("particle"), raw.get("side"), raw.get("front"),
                raw.get("front_divider")
        ));
    }

    static boolean itemId(String value) {
        if (value == null || value.isBlank() || value.length() > 256) {
            return false;
        }
        try {
            Key key = Key.parse(value);
            return value.equals(key.getFormatted()) && !"minecraft:air".equals(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
