/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.functionalstorage.model;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.BlockState;
import io.github.janguenter.bluemap.functionalstorage.adapter.bluemap522.FunctionalStorageBlockEntityData;
import io.github.janguenter.bluemap.functionalstorage.profile.FunctionalStorageProfile;

import java.util.Optional;

/** Converts only the exact persisted compound and exact host/BE/state tuple. */
public final class FramedMaterialSnapshotDecoder {

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
        FunctionalStorageProfile.Host profile =
                FunctionalStorageProfile.HOSTS.get(host.getId().getFormatted());
        FunctionalStorageBlockEntityData.FramedDrawerModelData raw =
                data.framedDrawerModelData();
        if (profile == null || raw == null
                || !itemId(raw.particle())
                || !itemId(raw.side())
                || !itemId(raw.front())) {
            return Optional.empty();
        }
        String divider = raw.frontDivider();
        if ((profile.needsDivider() && !itemId(divider))
                || (!profile.needsDivider() && divider != null && !itemId(divider))) {
            return Optional.empty();
        }
        return Optional.of(new FramedMaterialSnapshot(
                raw.particle(), raw.side(), raw.front(), divider
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
