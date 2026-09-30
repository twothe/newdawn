package two.newdawn.terrain;

import java.lang.management.ManagementFactory;
import java.util.Locale;

/** Warmed CPU/allocation measurements of production APIs; excludes full Minecraft generation. */
public final class TerrainBenchmark {
    private static volatile long blackhole;
    private static volatile Object retained;
    private static final int CHUNKS = 8192;

    public static void main(String[] args) {
        boolean verifyAllocations = args.length == 1 && args[0].equals("--verify-allocations");
        if (args.length > 0 && !verifyAllocations) throw new IllegalArgumentException("Expected --verify-allocations or no arguments");
        TerrainSampler sampler = new TerrainSampler(123456789L);
        if (!verifyAllocations) {
            measure("snapshot-chunks", CHUNKS, () -> run(sampler, CHUNKS));
            measure("snapshot-height-queries", CHUNKS * 256, () -> heights(sampler, CHUNKS * 256));
            measure("owned-buffer-chunks", CHUNKS, () -> buffered(sampler, null, CHUNKS));
        }
        int chunks = verifyAllocations ? 1024 : CHUNKS;
        TerrainChunk reusable = new TerrainChunk();
        double chunkBytes = measure("reused-buffer-chunks", chunks, () -> buffered(sampler, reusable, chunks));
        double heightBytes = measure("height-only-queries", chunks * 256, () -> heightOnly(sampler, chunks * 256));
        TerrainColumn column = new TerrainColumn();
        double columnBytes = measure("reused-column-queries", chunks * 256, () -> columns(sampler, column, chunks * 256));
        double biomeBytes = measure("reused-biome-queries", chunks * 256, () -> biomes(sampler, column, chunks * 256));
        if (verifyAllocations) {
            if (chunkBytes < 0 || heightBytes < 0 || columnBytes < 0 || biomeBytes < 0) {
                System.out.println("SKIP: JVM does not expose thread allocation counters");
            } else if (chunkBytes > 16 || heightBytes > 1 || columnBytes > 1 || biomeBytes > 1) {
                throw new AssertionError("Reusable sampling allocated per-call objects");
            } else {
                System.out.println("PASS: reusable chunk, column, height and biome sampling allocation budgets");
            }
        }
    }

    private static double measure(String name, int operations, Runnable operation) {
        var bean = ManagementFactory.getThreadMXBean();
        var allocation = bean instanceof com.sun.management.ThreadMXBean supported
                && supported.isThreadAllocatedMemorySupported() ? supported : null;
        if (allocation != null) allocation.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        for (int warmup = 0; warmup < 3; warmup++) operation.run();
        double minimumBytes = Double.POSITIVE_INFINITY;
        for (int trial = 0; trial < 3; trial++) {
            long before = allocation == null ? 0 : allocation.getThreadAllocatedBytes(thread);
            long start = System.nanoTime();
            operation.run();
            double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
            double bytes = allocation == null ? -1 : (allocation.getThreadAllocatedBytes(thread) - before) / (double) operations;
            minimumBytes = Math.min(minimumBytes, bytes);
            System.out.printf(Locale.ROOT, "%s: %.0f ops/s, %.3f us/op, %.1f B/op, checksum=%d%n",
                    name, operations / seconds, seconds * 1_000_000 / operations, bytes, blackhole);
        }
        return minimumBytes;
    }

    private static void run(TerrainSampler sampler, int chunks) {
        long sum = 0;
        for (int i = 0; i < chunks; i++) {
            TerrainSample[] columns = sampler.sampleChunk(i % 128 - 64, i / 128 - 64);
            retained = columns;
            for (TerrainSample sample : columns) {
                sum += sample.height() + Float.floatToIntBits(sample.humidity());
            }
        }
        blackhole = sum;
    }

    private static void heights(TerrainSampler sampler, int count) {
        long sum = 0;
        for (int i = 0; i < count; i++) sum += sampler.sample(i % 2048 - 1024, i / 2048 - 512).height();
        blackhole = sum;
    }

    private static void buffered(TerrainSampler sampler, TerrainChunk reusable, int count) {
        long sum = 0;
        for (int i = 0; i < count; i++) {
            TerrainChunk chunk = reusable == null ? new TerrainChunk() : reusable;
            sampler.sampleChunkInto(i % 128 - 64, i / 128 - 64, chunk);
            retained = chunk;
            for (int column = 0; column < TerrainChunk.COLUMN_COUNT; column++) {
                sum += chunk.height(column) + Float.floatToIntBits(chunk.humidity(column));
            }
        }
        blackhole = sum;
    }

    private static void heightOnly(TerrainSampler sampler, int count) {
        long sum = 0;
        for (int i = 0; i < count; i++) sum += sampler.sampleHeight(i % 2048 - 1024, i / 2048 - 512);
        blackhole = sum;
    }

    private static void columns(TerrainSampler sampler, TerrainColumn column, int count) {
        long sum = 0;
        for (int i = 0; i < count; i++) {
            sampler.sampleInto(i % 2048 - 1024, i / 2048 - 512, column);
            sum += column.height();
        }
        blackhole = sum;
    }

    private static void biomes(TerrainSampler sampler, TerrainColumn column, int count) {
        long sum = 0;
        for (int i = 0; i < count; i++) {
            sampler.sampleInto(i % 2048 - 1024, i / 2048 - 512, column);
            sum += BiomePalette.selectAt(column, (i & 127) - 64).biome().hashCode();
        }
        blackhole = sum;
    }
}
