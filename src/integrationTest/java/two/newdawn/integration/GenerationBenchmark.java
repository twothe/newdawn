package two.newdawn.integration;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.lang.management.ManagementFactory;
import java.util.Locale;

/** Development-only warmed measurement; chunk construction and biome setup stay outside the fill timer. */
final class GenerationBenchmark {
    private static volatile long blackhole;
    private static final int CHUNKS = 256;

    private GenerationBenchmark() {}

    static void run(ServerLevel level, NewDawnChunkGenerator generator) {
        var random = level.getChunkSource().randomState();
        var bean = ManagementFactory.getThreadMXBean();
        var allocation = bean instanceof com.sun.management.ThreadMXBean supported
                && supported.isThreadAllocatedMemorySupported() ? supported : null;
        if (allocation != null) allocation.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        for (int trial = -3; trial < 3; trial++) {
            var chunks = new ProtoChunk[CHUNKS];
            for (int i = 0; i < CHUNKS; i++) {
                chunks[i] = new ProtoChunk(new ChunkPos(i % 16 - 8, i / 16 - 8), UpgradeData.EMPTY,
                        level, level.registryAccess().registryOrThrow(Registries.BIOME), null);
            }
            long biomeBefore = allocation == null ? 0 : allocation.getThreadAllocatedBytes(thread);
            long biomeStart = System.nanoTime();
            for (var chunk : chunks) generator.createBiomes(random, Blender.empty(), level.structureManager(), chunk).join();
            double biomeSeconds = (System.nanoTime() - biomeStart) / 1_000_000_000.0;
            double biomeBytes = allocation == null ? -1 : (allocation.getThreadAllocatedBytes(thread) - biomeBefore) / (double) CHUNKS;
            long before = allocation == null ? 0 : allocation.getThreadAllocatedBytes(thread);
            long start = System.nanoTime();
            for (var chunk : chunks) generator.fillFromNoise(Blender.empty(), random, level.structureManager(), chunk).join();
            double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
            double bytes = allocation == null ? -1 : (allocation.getThreadAllocatedBytes(thread) - before) / (double) CHUNKS;
            long sum = 0;
            for (var chunk : chunks) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                sum += chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            }
            blackhole = sum;
            if (trial >= 0) LogUtils.getLogger().info(String.format(Locale.ROOT,
                    "New Dawn biome-fill: %.0f chunks/s, %.3f ms/chunk, %.1f B/chunk",
                    CHUNKS / biomeSeconds, biomeSeconds * 1000 / CHUNKS, biomeBytes));
            if (trial >= 0) LogUtils.getLogger().info(String.format(Locale.ROOT,
                    "New Dawn raw-fill: %.0f chunks/s, %.3f ms/chunk, %.1f B/chunk, checksum=%d",
                    CHUNKS / seconds, seconds * 1000 / CHUNKS, bytes, blackhole));
        }
    }
}
