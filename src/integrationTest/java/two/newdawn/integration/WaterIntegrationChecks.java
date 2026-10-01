package two.newdawn.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.chunk.ChunkAccess;
import two.newdawn.terrain.TerrainSampler;

/** Development-only scan of the original ocean water envelope at generation boundaries. */
final class WaterIntegrationChecks {
    private WaterIntegrationChecks() {}

    static int countOceanAir(ChunkAccess chunk, TerrainSampler terrain) {
        int holes = 0;
        var position = new BlockPos.MutableBlockPos();
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int worldX = chunk.getPos().getMinBlockX() + x, worldZ = chunk.getPos().getMinBlockZ() + z;
            for (int y = terrain.sampleHeight(worldX, worldZ); y < TerrainSampler.SEA_LEVEL; y++) {
                if (chunk.getBlockState(position.set(worldX, y, worldZ)).isAir()) holes++;
            }
        }
        return holes;
    }

    static void verify(ServerLevel level, TerrainSampler terrain) {
        verifyLazyHeightCache();
        verifyAquiferBoundary();
        // Known failing area: compare fresh generation and persisted chunks on the reported seed.
        if (level.getSeed() != -8458999313514431577L) return;
        int holes = 0;
        for (int z = -42; z <= -34; z++) for (int x = -166; x <= -158; x++) {
            holes += countOceanAir(level.getChunk(x, z), terrain);
        }
        if (holes != 0) throw new AssertionError("Reported ocean-hole regression: " + holes + " air blocks in 81 chunks");
        com.mojang.logging.LogUtils.getLogger().info("PASS ocean-hole regression: fullChunks=81, oceanAir=0 (before fix: 402)");
    }

    /** Count the real height-function boundary, without a production counter or copied cache logic. */
    private static void verifyLazyHeightCache() {
        int[] queries = {0};
        var delegate = new Aquifer() {
            @Override public net.minecraft.world.level.block.state.BlockState computeSubstance(
                    DensityFunction.FunctionContext point, double density) {
                return density > 0 ? null : Blocks.AIR.defaultBlockState();
            }
            @Override public boolean shouldScheduleFluidUpdate() { return true; }
        };
        var chunk = new ChunkPos(-2, 3);
        var aquifer = new TerrainCarvers.OceanAquifer(delegate, (x, z) -> {
            if (x < -32 || x > -17 || z < 48 || z > 63) throw new AssertionError("Height query escaped its chunk");
            queries[0]++;
            return 55;
        }, chunk);
        if (queries[0] != 0) throw new AssertionError("Aquifer eagerly sampled terrain");
        var water = new DensityFunction.SinglePointContext(-31, 60, 49);
        aquifer.computeSubstance(water, 1);
        aquifer.computeSubstance(new DensityFunction.SinglePointContext(-31, 64, 49), 0);
        if (queries[0] != 0 || !aquifer.shouldScheduleFluidUpdate()) throw new AssertionError("Irrelevant query sampled terrain or lost delegate state");
        for (int i = 0; i < 4; i++) {
            if (!aquifer.computeSubstance(water, 0).is(Blocks.WATER) || aquifer.shouldScheduleFluidUpdate()) {
                throw new AssertionError("Cached water boundary changed");
            }
        }
        if (queries[0] != 1) throw new AssertionError("Aquifer did not reuse column height");
        aquifer.computeSubstance(new DensityFunction.SinglePointContext(-31, 54, 49), 0);
        if (queries[0] != 1 || !aquifer.shouldScheduleFluidUpdate()) throw new AssertionError("Underground delegation lost cached height or update state");
        aquifer.computeSubstance(new DensityFunction.SinglePointContext(-30, 60, 49), 0);
        if (queries[0] != 2) throw new AssertionError("Aquifer aliased neighboring columns");
    }

    private static void verifyAquiferBoundary() {
        TerrainSampler terrain = new TerrainSampler(-8458999313514431577L);
        ChunkPos chunk = new ChunkPos(-162, -38);
        var air = new Aquifer.FluidStatus(Integer.MIN_VALUE, Blocks.AIR.defaultBlockState());
        var delegate = Aquifer.createDisabled((x, y, z) -> air);
        var aquifer = new TerrainCarvers.OceanAquifer(delegate, terrain::sampleHeight, chunk);
        int waterBlocks = 0;
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int worldX = chunk.getMinBlockX() + x, worldZ = chunk.getMinBlockZ() + z;
            int height = terrain.sampleHeight(worldX, worldZ);
            for (int y = height; y < TerrainSampler.SEA_LEVEL; y++) {
                var point = new DensityFunction.SinglePointContext(worldX, y, worldZ);
                if (!aquifer.computeSubstance(point, 0).is(Blocks.WATER) || aquifer.shouldScheduleFluidUpdate()) {
                    throw new AssertionError("Cave aquifer replaced original ocean water");
                }
                if (aquifer.computeSubstance(point, 1) != null) throw new AssertionError("Positive density lost its solid contract");
                waterBlocks++;
            }
            for (int y : new int[]{height - 1, TerrainSampler.SEA_LEVEL}) {
                if (!aquifer.computeSubstance(new DensityFunction.SinglePointContext(worldX, y, worldZ), 0).isAir()) {
                    throw new AssertionError("Ocean protection replaced unrelated underground/above-sea aquifer");
                }
            }
        }
        if (waterBlocks == 0) throw new AssertionError("Ocean regression fixture did not exercise any water");
    }
}
