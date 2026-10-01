package two.newdawn.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;
import two.newdawn.terrain.BiomePalette;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/** Development-only layer boundaries, clipped heights and concurrent raw-fill checks. */
final class SmokeTerrainChecks {
    private SmokeTerrainChecks() {}

    static void run(ServerLevel level, NewDawnChunkGenerator generator) throws Exception {
        verifyLayers();
        verifyCliffChunk(level, generator);
        // Each worker owns its chunks. Shared generator, sampler and biome source exercise production ownership.
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tasks = new ArrayList<Callable<Void>>();
            for (int worker = 0; worker < 4; worker++) {
                final int offset = worker;
                tasks.add(() -> {
                    for (LevelHeightAccessor bounds : new LevelHeightAccessor[]{level,
                            LevelHeightAccessor.create(-96, 64), LevelHeightAccessor.create(48, 32),
                            LevelHeightAccessor.create(240, 112)}) {
                        verifyChunk(level, generator, bounds, new ChunkPos(offset * 19 - 31, offset * -13 + 7));
                    }
                    return null;
                });
            }
            for (var future : executor.invokeAll(tasks)) future.get();
        }
    }

    /** Exercise a real exposed-rock chunk, rather than relying on fixed generic chunks to hit a cliff. */
    private static void verifyCliffChunk(ServerLevel level, NewDawnChunkGenerator generator) {
        var sampler = ((NewDawnBiomeSource) generator.getBiomeSource()).terrain();
        var column = new two.newdawn.terrain.TerrainColumn();
        for (int z = -3072; z < 3072; z += 8) for (int x = -3072; x < 3072; x += 8) {
            sampler.sampleInto(x, z, column);
            if (column.exposedRock() && column.height() > 70) {
                var pos = new ChunkPos(x >> 4, z >> 4);
                verifyChunk(level, generator, level, pos);
                var states = generator.getBaseColumn(x, z, level, level.getChunkSource().randomState());
                var top = states.getBlock(column.height() - 1);
                require(top.is(Blocks.STONE) || top.is(Blocks.SANDSTONE) || top.is(Blocks.RED_SANDSTONE)
                        || top.is(Blocks.TERRACOTTA), "Cliff retained soil/snow instead of exposed rock");
                // Full pipeline also has to accept the new relief and surface materials.
                level.getChunk(pos.x, pos.z);
                com.mojang.logging.LogUtils.getLogger().info("PASS cliff chunk: {}, sample=({}, {}, {})", pos, x, column.height(), z);
                return;
            }
        }
        throw new AssertionError("No exposed land cliff found in the smoke survey");
    }

    private static void verifyChunk(ServerLevel level, NewDawnChunkGenerator generator,
                                    LevelHeightAccessor bounds, ChunkPos position) {
        var random = level.getChunkSource().randomState();
        var chunk = new ProtoChunk(position, UpgradeData.EMPTY, bounds,
                level.registryAccess().registryOrThrow(Registries.BIOME), null);
        generator.createBiomes(random, Blender.empty(), level.structureManager(), chunk).join();
        var biomeContainers = java.util.Arrays.stream(chunk.getSections()).map(section -> section.getBiomes()).toList();
        generator.fillFromNoise(Blender.empty(), random, level.structureManager(), chunk).join();
        for (int index = 0; index < chunk.getSectionsCount(); index++) {
            var section = chunk.getSection(index);
            require(section.getBiomes() == biomeContainers.get(index), "Raw fill replaced generated biome data");
            boolean hasBlocks = false, hasTickingBlocks = false;
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                var state = section.getBlockState(x, y, z);
                hasBlocks |= !state.isAir();
                hasTickingBlocks |= state.isRandomlyTicking();
            }
            require(section.hasOnlyAir() != hasBlocks, "Section empty count mismatch");
            require(section.isRandomlyTickingBlocks() == hasTickingBlocks, "Section ticking count mismatch");
        }
        int[] originalFloor = new int[256], originalSurface = new int[256];
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            originalFloor[x + z * 16] = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
            originalSurface[x + z * 16] = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        }
        // Minecraft independently scans the actual blocks with each of its six height predicates.
        Heightmap.primeHeightmaps(chunk, EnumSet.allOf(Heightmap.Types.class));
        var blockPosition = new BlockPos.MutableBlockPos();
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int worldX = position.getMinBlockX() + x, worldZ = position.getMinBlockZ() + z;
            require(originalFloor[x + z * 16] == chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z), "Floor heightmap prime mismatch");
            require(originalSurface[x + z * 16] == chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), "Surface heightmap prime mismatch");
            for (var type : Heightmap.Types.values()) {
                require(generator.getBaseHeight(worldX, worldZ, type, bounds, random) == chunk.getHeight(type, x, z) + 1,
                        "Clipped height mismatch: " + type + " at " + position);
            }
            var column = generator.getBaseColumn(worldX, worldZ, bounds, random);
            for (int y = bounds.getMinBuildHeight(); y < bounds.getMaxBuildHeight(); y++) {
                require(column.getBlock(y).equals(chunk.getBlockState(blockPosition.set(worldX, y, worldZ))),
                        "Concurrent/clipped base column mismatch");
            }
        }
    }

    private static void verifyLayers() {
        var layers = new TerrainBlockColumn();
        for (var material : BiomePalette.Material.values()) {
            layers.prepare(70, 3, new BiomePalette.Entry("test", material, material), -64);
            for (var type : Heightmap.Types.values()) {
                require(type.isOpaque().test(layers.stateAt(69)), "Fast height query does not support material " + material);
            }
        }
        var grass = new BiomePalette.Entry("plains", BiomePalette.Material.GRASS, BiomePalette.Material.DIRT);
        layers.prepare(70, 3, grass, -64);
        expect(layers, -64, -63, Blocks.BEDROCK);
        expect(layers, -63, 0, Blocks.DEEPSLATE);
        expect(layers, 0, 66, Blocks.STONE);
        expect(layers, 66, 69, Blocks.DIRT);
        expect(layers, 69, 70, Blocks.GRASS_BLOCK);
        expect(layers, 70, 320, Blocks.AIR);

        var sand = new BiomePalette.Entry("desert", BiomePalette.Material.SAND, BiomePalette.Material.SAND);
        layers.prepare(70, 5, sand, -64);
        expect(layers, 0, 64, Blocks.STONE);
        expect(layers, 64, 66, Blocks.SANDSTONE);
        expect(layers, 66, 70, Blocks.SAND);
        layers.prepare(1, 5, sand, -64);
        expect(layers, -63, -5, Blocks.DEEPSLATE);
        expect(layers, -5, -2, Blocks.SANDSTONE);
        expect(layers, -2, 1, Blocks.SAND);
        expect(layers, 1, 64, Blocks.WATER);

        layers.prepare(60, 0, grass, -64);
        expect(layers, 0, 59, Blocks.STONE);
        expect(layers, 59, 60, Blocks.DIRT);
        expect(layers, 60, 64, Blocks.WATER);
        expect(layers, 64, 320, Blocks.AIR);
        layers.prepare(64, 0, grass, 63);
        expect(layers, 63, 64, Blocks.BEDROCK);
        expect(layers, 64, 320, Blocks.AIR);
        layers.prepare(60, 3, grass, 60);
        expect(layers, 60, 64, Blocks.WATER);

        layers.prepare(90, 5, grass, -64, true);
        expect(layers, 0, 90, Blocks.STONE);
        layers.prepare(90, 5, sand, -64, true);
        expect(layers, 0, 89, Blocks.STONE);
        expect(layers, 89, 90, Blocks.SANDSTONE);
        layers.prepare(90, 5, grass, -64, false);
        expect(layers, 84, 89, Blocks.DIRT);
        expect(layers, 89, 90, Blocks.GRASS_BLOCK);
    }

    private static void expect(TerrainBlockColumn layers, int start, int end, Block expected) {
        var states = layers.toStates(start, end);
        for (int y = start; y < end; y++) {
            require(layers.stateAt(y).is(expected) && states[y - start].is(expected),
                    "Material boundary mismatch at Y=" + y + ", expected " + expected);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
