/*
 * SPDX-License-Identifier: MIT
 */
package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap522;

import de.bluecolored.bluemap.core.world.mca.blockentity.MCABlockEntity;
import de.bluecolored.bluenbt.NBTName;

/** Narrow BlueNBT projection of Functional Storage's persisted framed material data. */
public final class FunctionalStorageBlockEntityData extends MCABlockEntity {

    @NBTName("framedDrawerModelData")
    private FramedDrawerModelData framedDrawerModelData;

    public FunctionalStorageBlockEntityData() {
    }

    public FramedDrawerModelData framedDrawerModelData() {
        return framedDrawerModelData;
    }

    /** Exact Titanium INBTSerializable compound keys for FramedDrawerModelData. */
    public static final class FramedDrawerModelData {

        private String particle;
        private String side;
        private String front;

        @NBTName("front_divider")
        private String frontDivider;

        public FramedDrawerModelData() {
        }

        public String particle() {
            return particle;
        }

        public String side() {
            return side;
        }

        public String front() {
            return front;
        }

        public String frontDivider() {
            return frontDivider;
        }
    }
}
