/*
 * SPDX-License-Identifier: MIT
 */

package io.github.janguenter.bluemap.functionalstorage.adapter.bluemap522;

import com.flowpowered.math.vector.Vector4f;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.mask.Mask;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.PackVersion;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Face;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.TextureVariable;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.world.BlockEntity;
import de.bluecolored.bluemap.core.world.BlockProperties;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.DimensionType;
import de.bluecolored.bluemap.core.world.LightData;
import de.bluecolored.bluemap.core.world.biome.Biome;
import de.bluecolored.bluemap.core.world.block.BlockAccess;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChildShellVisibilityTest {

    private static final int X = 1;
    private static final int Y = 64;
    private static final int Z = 1;
    private static final BlockState HOST = BlockState.fromString(
            "functionalstorage:framed_1[facing=north,subfacing=down,locked=false]"
    );
    private static final BlockState SOLID = BlockState.fromString("test:solid");
    private static final Variant IDENTITY = new Variant(
            new ResourcePath<Model>("functionalstorage:block/framed_1")
    );
    private static final Face NORTH_CULL = new Face(
            new Vector4f(0F, 0F, 16F, 16F),
            new TextureVariable(new ResourcePath<Texture>("test:white")),
            Direction.NORTH, 0, -1
    );

    @Test
    void cullfaceDropsOpaqueNeighborButRetainsNoncullingNeighbor() {
        BlockNeighborhood opaque = neighborhood(Map.of(
                new Position(X, Y, Z), HOST,
                new Position(X, Y, Z - 1), SOLID
        ), new LightData(15, 0), Settings.ordinarySettings());
        BlockNeighborhood open = neighborhood(
                Map.of(new Position(X, Y, Z), HOST),
                new LightData(15, 0), Settings.ordinarySettings()
        );
        FaceLighting.Sample light = new FaceLighting.Sample(15, 0, 0);

        assertFalse(ChildShellEmitter.visible(
                NORTH_CULL, 0F, light, opaque, IDENTITY, Settings.ordinarySettings()
        ));
        assertTrue(ChildShellEmitter.visible(
                NORTH_CULL, 0F, light, open, IDENTITY, Settings.ordinarySettings()
        ));
    }

    @Test
    void rotatedHostTransformsCullfaceExactlyOnce() {
        Variant east = new Variant(
                new ResourcePath<Model>("functionalstorage:block/framed_1"),
                0F, 90F, 0F, false, 1D
        );
        BlockNeighborhood eastOpaque = neighborhood(Map.of(
                new Position(X, Y, Z), HOST,
                new Position(X + 1, Y, Z), SOLID
        ), new LightData(15, 0), Settings.ordinarySettings());
        BlockNeighborhood northOnly = neighborhood(Map.of(
                new Position(X, Y, Z), HOST,
                new Position(X, Y, Z - 1), SOLID
        ), new LightData(15, 0), Settings.ordinarySettings());
        FaceLighting.Sample light = new FaceLighting.Sample(15, 0, 0);

        assertFalse(ChildShellEmitter.visible(
                NORTH_CULL, 0F, light, eastOpaque, east, Settings.ordinarySettings()
        ));
        assertTrue(ChildShellEmitter.visible(
                NORTH_CULL, 0F, light, northOnly, east, Settings.ordinarySettings()
        ));
    }

    @Test
    void topOnlyCaveAndAmbientOcclusionFollowResourceRendererPolicy() {
        BlockNeighborhood open = neighborhood(
                Map.of(new Position(X, Y, Z), HOST),
                new LightData(15, 0), Settings.ordinarySettings()
        );
        FaceLighting.Sample lit = new FaceLighting.Sample(15, 0, 0);
        assertFalse(ChildShellEmitter.visible(
                NORTH_CULL, 0.009F, lit, open, IDENTITY,
                Settings.topOnlySettings()
        ));
        assertTrue(ChildShellEmitter.visible(
                NORTH_CULL, 0.01F, lit, open, IDENTITY,
                Settings.topOnlySettings()
        ));

        BlockNeighborhood cave = neighborhood(
                Map.of(new Position(X, Y, Z), HOST),
                new LightData(0, 0), Settings.caveSettings()
        );
        assertFalse(ChildShellEmitter.visible(
                NORTH_CULL, 0F, new FaceLighting.Sample(0, 0, 15),
                cave, IDENTITY, Settings.caveSettings()
        ));

        BlockNeighborhood occludedCorners = neighborhood(Map.of(
                new Position(X, Y, Z), HOST,
                new Position(X - 1, Y, Z - 1), SOLID,
                new Position(X, Y - 1, Z - 1), SOLID,
                new Position(X - 1, Y - 1, Z - 1), SOLID
        ), new LightData(15, 0), Settings.ordinarySettings());
        assertEquals(0.25F, ChildShellEmitter.testAo(
                0F, 0F, 0F, Direction.NORTH, occludedCorners, IDENTITY
        ), 0F);
    }

    private static BlockNeighborhood neighborhood(
            Map<Position, BlockState> states,
            LightData light,
            RenderSettings settings
    ) {
        ResourcePack resourcePack = new ResourcePack(new PackVersion(34, 0)) {
            @Override
            public BlockProperties getBlockProperties(BlockState state) {
                boolean solid = SOLID.equals(state);
                return BlockProperties.builder()
                        .culling(solid)
                        .occluding(solid)
                        .cullingIdentical(false)
                        .randomOffset(false)
                        .build();
            }
        };
        BlockNeighborhood neighborhood = new BlockNeighborhood(
                new TestBlockAccess(states, light), resourcePack, settings,
                DimensionType.OVERWORLD
        );
        neighborhood.set(X, Y, Z);
        return neighborhood;
    }

    private record Settings(
            int removeCavesBelowY,
            boolean caveUsesBlockLight,
            boolean renderTopOnly
    ) implements RenderSettings {

        private static Settings ordinarySettings() {
            return new Settings(Integer.MIN_VALUE, false, false);
        }

        private static Settings topOnlySettings() {
            return new Settings(Integer.MIN_VALUE, false, true);
        }

        private static Settings caveSettings() {
            return new Settings(Integer.MAX_VALUE, false, false);
        }

        @Override
        public int getRemoveCavesBelowY() {
            return removeCavesBelowY;
        }

        @Override
        public int getCaveDetectionOceanFloor() {
            return 0;
        }

        @Override
        public boolean isCaveDetectionUsesBlockLight() {
            return caveUsesBlockLight;
        }

        @Override
        public float getAmbientLight() {
            return 0F;
        }

        @Override
        public boolean isRenderEdges() {
            return false;
        }

        @Override
        public Mask getRenderMask() {
            return Mask.ALL;
        }

        @Override
        public boolean isSaveHiresLayer() {
            return false;
        }

        @Override
        public boolean isRenderTopOnly() {
            return renderTopOnly;
        }
    }

    private static final class TestBlockAccess implements BlockAccess {

        private final Map<Position, BlockState> states;
        private final LightData light;
        private int x;
        private int y;
        private int z;

        private TestBlockAccess(Map<Position, BlockState> states, LightData light) {
            this.states = states;
            this.light = light;
        }

        @Override
        public void set(int newX, int newY, int newZ) {
            x = newX;
            y = newY;
            z = newZ;
        }

        @Override
        public BlockAccess copy() {
            return new TestBlockAccess(states, light);
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public int getZ() {
            return z;
        }

        @Override
        public BlockState getBlockState() {
            return states.getOrDefault(new Position(x, y, z), BlockState.AIR);
        }

        @Override
        public LightData getLightData() {
            return new LightData(light.getSkyLight(), light.getBlockLight());
        }

        @Override
        public Biome getBiome() {
            return Biome.DEFAULT;
        }

        @Override
        public BlockEntity getBlockEntity() {
            return null;
        }

        @Override
        public boolean hasOceanFloorY() {
            return false;
        }

        @Override
        public int getOceanFloorY() {
            return 0;
        }
    }

    private record Position(int x, int y, int z) {
    }
}
