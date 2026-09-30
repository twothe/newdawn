package two.newdawn.integration;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import two.newdawn.NewDawn;
import two.newdawn.terrain.BiomePalette;
import two.newdawn.terrain.TerrainSampler;

import java.nio.file.Files;
import java.nio.file.Path;

/** Development-only real-server regression; excluded from the distributable jar. */
@EventBusSubscriber(modid = NewDawn.MOD_ID)
public final class SmokeTest {
    private SmokeTest() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!Boolean.getBoolean("newdawn.smokeTest")) return;
        var server = event.getServer();
        try {
            ServerLevel level = server.overworld();
            SmokePresetChecks.verify(level);
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
            require(level.getChunkSource().getGenerator() instanceof NewDawnChunkGenerator, "World preset did not select New Dawn");
            var generator = (NewDawnChunkGenerator) level.getChunkSource().getGenerator();
            var source = (NewDawnBiomeSource) generator.getBiomeSource();
            SmokeWaterChecks.verify(level, source.terrain());
            SmokeTerrainChecks.run(level, generator);
            if (Boolean.getBoolean("newdawn.benchmarkGeneration")) SmokePerformance.run(level, generator);
            require(source.terrain().seed() == level.getSeed(), "World seed was not bound");
            var biomeOrder = source.possibleBiomes().stream().map(biome -> biome.unwrapKey().orElseThrow().location().toString()).toList();
            require(biomeOrder.equals(biomeOrder.stream().sorted().toList()), "Biome encounter order would change feature seeds across JVMs");
            var random = level.getChunkSource().randomState();
            var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
            var json = ChunkGenerator.CODEC.encodeStart(ops, generator).getOrThrow();
            var restored = ChunkGenerator.CODEC.parse(ops, JsonParser.parseString(json.toString())).getOrThrow();
            require(restored instanceof NewDawnChunkGenerator, "Generator codec lost its type");
            restored.createState(level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET), random, level.getSeed());
            var restoredSource = (NewDawnBiomeSource) restored.getBiomeSource();
            require(restoredSource.terrain().sample(-17, 31).equals(source.terrain().sample(-17, 31)), "Codec reload changed terrain");
            try {
                restoredSource.initialize(level.getSeed() ^ 1L);
                throw new AssertionError("Biome source accepted a different seed after initialization");
            } catch (IllegalStateException expected) { /* Seed ownership is enforced. */ }
            try (var executor = java.util.concurrent.Executors.newFixedThreadPool(4)) {
                var tasks = new java.util.ArrayList<java.util.concurrent.Callable<Void>>();
                for (int worker = 0; worker < 4; worker++) {
                    final int offset = worker;
                    tasks.add(() -> {
                        var column = new two.newdawn.terrain.TerrainColumn();
                        for (int index = 0; index < 512; index++) {
                            int x = (index - 256) * 256 + offset, z = index % 17 - 8;
                            source.terrain().sampleInto(x * 4, z * 4, column);
                            for (int quartY : new int[]{-16, -6, 0, 8, 16, 64}) {
                                String expected = BiomePalette.selectAt(column, quartY * 4).biome();
                                String actual = source.getNoiseBiome(x, quartY, z, random.sampler())
                                        .unwrapKey().orElseThrow().location().getPath();
                                require(actual.equals(expected), "Concurrent biome cache collision or vertical reuse changed selection");
                            }
                        }
                        return null;
                    });
                }
                for (var future : executor.invokeAll(tasks)) future.get();
            }
            int columns = 0;
            long fillNanos = 0;
            for (ChunkPos pos : new ChunkPos[]{new ChunkPos(0, 0), new ChunkPos(-1, -1), new ChunkPos(24, -13), new ChunkPos(-37, 29)}) {
                var chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level, level.registryAccess().registryOrThrow(Registries.BIOME), null);
                generator.createBiomes(random, Blender.empty(), level.structureManager(), chunk).join();
                long start = System.nanoTime();
                generator.fillFromNoise(Blender.empty(), random, level.structureManager(), chunk).join();
                fillNanos += System.nanoTime() - start;
                for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                    int worldX = pos.getMinBlockX() + x, worldZ = pos.getMinBlockZ() + z;
                    var sample = source.terrain().sample(worldX, worldZ);
                    int solid = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) + 1;
                    int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + 1;
                    require(solid == sample.height(), "Solid heightmap differs from core at " + pos);
                    require(surface == Math.max(sample.height(), TerrainSampler.SEA_LEVEL), "Water heightmap differs from core");
                    require(generator.getBaseHeight(worldX, worldZ, Heightmap.Types.OCEAN_FLOOR_WG, level, random) == solid, "Structure height query mismatch");
                    var column = generator.getBaseColumn(worldX, worldZ, level, random);
                    for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                        require(column.getBlock(y).equals(chunk.getBlockState(new BlockPos(worldX, y, worldZ))), "Base column/fill mismatch");
                    }
                    columns++;
                }
            }
            int ores = 0, logs = 0, caves = 0, structures = 0;
            long checksum = 1;
            int generatedChunks = 0;
            long started = System.nanoTime();
            for (int z = -2; z <= 2; z++) for (int x = -2; x <= 2; x++) {
                var chunk = level.getChunk(x, z);
                generatedChunks++;
                for (var structure : chunk.getAllStarts().values()) if (structure.isValid()) structures++;
                for (int localZ = 0; localZ < 16; localZ++) for (int localX = 0; localX < 16; localX++) {
                    int worldX = x * 16 + localX, worldZ = z * 16 + localZ;
                    for (int y = -63; y < 180; y++) {
                        var block = chunk.getBlockState(new BlockPos(worldX, y, worldZ));
                        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString();
                        checksum = checksum * 31 + block.toString().hashCode();
                        if (id.endsWith("_ore")) ores++;
                        if (id.endsWith("_log")) logs++;
                        if (y < 0 && block.isAir()) caves++;
                    }
                }
            }
            require(ores > 0, "Vanilla ore decoration did not run");
            require(caves > 0, "Vanilla carvers did not produce underground air");
            BlockPos village = level.findNearestMapStructure(StructureTags.VILLAGE, BlockPos.ZERO, 32, false);
            require(village != null, "No village could be located");
            var villageChunk = level.getChunk(village.getX() >> 4, village.getZ() >> 4);
            require(villageChunk.getAllStarts().values().stream().anyMatch(start -> start.isValid()), "Located village did not generate a structure start");
            Path previous = Path.of("newdawn-smoke-checksum.txt");
            boolean reloaded = Files.exists(previous);
            if (reloaded) require(Files.readString(previous).trim().equals(Long.toString(checksum)), "Persisted chunk content changed after restart");
            else Files.writeString(previous, Long.toString(checksum));
            String report = "PASS New Dawn smoke: seed=" + level.getSeed() + ", reload=" + reloaded + ", columns=" + columns
                    + ", presets=PASS, parallelClippedColumns=4096, heightmapTypes=6, layerBoundaries=PASS"
                    + ", chunks=" + generatedChunks + ", ores=" + ores + ", logs=" + logs + ", undergroundAir=" + caves
                    + ", localStructures=" + structures + ", village=" + village + ", checksum=" + checksum
                    + ", rawFillMs=" + fillNanos / 1_000_000.0 + ", fullChunkInspectionMs=" + (System.nanoTime() - started) / 1_000_000.0;
            Files.writeString(Path.of("newdawn-smoke-result.txt"), report + "\nGenerator codec: " + json + "\n");
            LogUtils.getLogger().info(report);
        } catch (Exception | AssertionError failure) {
            LogUtils.getLogger().error("New Dawn smoke test failed", failure);
            try { Files.writeString(Path.of("newdawn-smoke-result.txt"), "FAIL " + failure); }
            catch (java.io.IOException error) { failure.addSuppressed(error); }
            throw new IllegalStateException("New Dawn smoke test failed", failure);
        } finally {
            server.halt(false);
        }
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
