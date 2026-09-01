/*
 * SPDX-License-Identifier: MIT
 */

package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap523;

import de.bluecolored.bluemap.core.world.mca.blockentity.MCABlockEntity;
import de.bluecolored.bluenbt.NBTName;

import java.util.Map;

/** Narrow BlueNBT projection of Functional Storage's persisted framed material data. */
public final class FunctionalStorageBlockEntityData extends MCABlockEntity {

    @NBTName("framedDrawerModelData")
    private Map<String, String> framedDrawerModelData;

    public FunctionalStorageBlockEntityData() {
    }

    public Map<String, String> framedDrawerModelData() {
        return framedDrawerModelData == null
                ? null : Map.copyOf(framedDrawerModelData);
    }
}
