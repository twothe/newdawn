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
final class SmokeWaterChecks {
    private SmokeWaterChecks() {}

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

    private static void verifyAquiferBoundary() {
        TerrainSampler terrain = new TerrainSampler(-8458999313514431577L);
        ChunkPos chunk = new ChunkPos(-162, -38);
        var air = new Aquifer.FluidStatus(Integer.MIN_VALUE, Blocks.AIR.defaultBlockState());
        var delegate = Aquifer.createDisabled((x, y, z) -> air);
        var aquifer = new TerrainCarvers.OceanAquifer(delegate, terrain, chunk);
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
