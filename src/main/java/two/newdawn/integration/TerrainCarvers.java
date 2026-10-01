package two.newdawn.integration;

import net.minecraft.core.QuartPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.Beardifier;
import two.newdawn.terrain.TerrainSampler;

import java.util.function.IntBinaryOperator;

/**
 * Runs registered Minecraft carvers with a terrain-aware ocean boundary. Vanilla's aquifer surface
 * estimate describes its noise terrain, so it must not decide that an existing New Dawn ocean is air.
 * Carver traversal and random seeding follow Minecraft 1.21.1; no carver or biome registry is replaced.
 */
final class TerrainCarvers {
    private static final Aquifer.FluidStatus WATER = new Aquifer.FluidStatus(TerrainSampler.SEA_LEVEL, Blocks.WATER.defaultBlockState());
    private static final Aquifer.FluidStatus LAVA = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());

    private TerrainCarvers() {}

    static void apply(NewDawnChunkGenerator generator, TerrainSampler terrain, WorldGenRegion level,
                      long seed, RandomState random, BiomeManager biomes, StructureManager structures,
                      ChunkAccess chunk, GenerationStep.Carving step) {
        var source = generator.getBiomeSource();
        var biomeManager = biomes.withDifferentSource((x, y, z) -> source.getNoiseBiome(x, y, z, random.sampler()));
        var settings = generator.generatorSettings().value();
        var noise = chunk.getOrCreateNoiseChunk(access -> NoiseChunk.forChunk(access, random,
                Beardifier.forStructuresInChunk(structures, access.getPos()), settings,
                (x, y, z) -> y < -54 ? LAVA : WATER, Blender.of(level)));
        var aquifer = new OceanAquifer(noise.aquifer(), terrain::sampleHeight, chunk.getPos());
        var context = new CarvingContext(generator, level.registryAccess(), chunk.getHeightAccessorForGeneration(),
                noise, random, settings.surfaceRule());
        var mask = ((ProtoChunk) chunk).getOrCreateCarvingMask(step);
        // Every start-chunk iteration resets the seed before consulting the random source.
        var carverRandom = new WorldgenRandom(new LegacyRandomSource(0));
        for (int offsetX = -8; offsetX <= 8; offsetX++) for (int offsetZ = -8; offsetZ <= 8; offsetZ++) {
            var origin = new ChunkPos(chunk.getPos().x + offsetX, chunk.getPos().z + offsetZ);
            var neighbor = level.getChunk(origin.x, origin.z);
            var biomeSettings = neighbor.carverBiome(() -> generator.getBiomeGenerationSettings(source.getNoiseBiome(
                    QuartPos.fromBlock(origin.getMinBlockX()), 0, QuartPos.fromBlock(origin.getMinBlockZ()), random.sampler())));
            int index = 0;
            for (var holder : biomeSettings.getCarvers(step)) {
                var carver = holder.value();
                carverRandom.setLargeFeatureSeed(seed + index++, origin.x, origin.z);
                if (carver.isStartChunk(carverRandom)) {
                    carver.carve(context, chunk, biomeManager::getBiome, carverRandom, aquifer, origin, mask);
                }
            }
        }
    }

    /** Invocation-owned wrapper: only the original ocean envelope overrides the normal cave aquifer. */
    static final class OceanAquifer implements Aquifer {
        private final Aquifer delegate;
        private final IntBinaryOperator terrainHeight;
        private final int startX, startZ;
        // Zero means unsampled: the terrain height contract is 1..255.
        private final int[] heights = new int[256];
        private boolean scheduleUpdate;

        /** The height function supplies first-air Y for absolute coordinates; queries belong to this chunk. */
        OceanAquifer(Aquifer delegate, IntBinaryOperator terrainHeight, ChunkPos chunk) {
            this.delegate = delegate;
            this.terrainHeight = terrainHeight;
            startX = chunk.getMinBlockX();
            startZ = chunk.getMinBlockZ();
        }

        @Override
        public BlockState computeSubstance(DensityFunction.FunctionContext position, double density) {
            int y = position.blockY();
            if (density <= 0 && y < TerrainSampler.SEA_LEVEL && y >= height(position.blockX(), position.blockZ())) {
                scheduleUpdate = false;
                return Blocks.WATER.defaultBlockState();
            }
            BlockState result = delegate.computeSubstance(position, density);
            scheduleUpdate = delegate.shouldScheduleFluidUpdate();
            return result;
        }

        private int height(int x, int z) {
            int index = (x & 15) + 16 * (z & 15);
            int height = heights[index];
            if (height == 0) {
                height = terrainHeight.applyAsInt(startX + (x & 15), startZ + (z & 15));
                heights[index] = height;
            }
            return height;
        }

        @Override public boolean shouldScheduleFluidUpdate() { return scheduleUpdate; }
    }
}
