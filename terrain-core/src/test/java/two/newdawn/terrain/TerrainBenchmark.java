package two.newdawn.terrain;

import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.Random;

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
        // Select by distribution alone, outside timing: profile changes must use the same coordinates.
        int[] mountainX = new int[4096], mountainZ = new int[4096];
        mountainCoordinates(sampler, mountainX, mountainZ);
        double mountainHeightBytes = measure("mountain-height-queries", chunks * 256,
                () -> mountainQueries(sampler, column, mountainX, mountainZ, chunks * 256, true));
        double mountainColumnBytes = measure("mountain-column-queries", chunks * 256,
                () -> mountainQueries(sampler, column, mountainX, mountainZ, chunks * 256, false));
        if (verifyAllocations) {
            if (chunkBytes < 0 || heightBytes < 0 || columnBytes < 0 || biomeBytes < 0
                    || mountainHeightBytes < 0 || mountainColumnBytes < 0) {
                System.out.println("SKIP: JVM does not expose thread allocation counters");
            } else if (chunkBytes > 16 || heightBytes > 1 || columnBytes > 1 || biomeBytes > 1
                    || mountainHeightBytes > 1 || mountainColumnBytes > 1) {
                throw new AssertionError("Reusable sampling allocated per-call objects");
            } else {
                System.out.println("PASS: reusable chunk, column, height and biome sampling allocation budgets");
            }
        }
    }

    /** A deterministic reservoir covers strong mountain regions without timing setup or allocating in queries. */
    private static void mountainCoordinates(TerrainSampler sampler, int[] xs, int[] zs) {
        Random selection = new Random(4815162342L);
        int candidates = 0;
        for (int z = -3072; z < 3072; z += 8) for (int x = -3072; x < 3072; x += 8) {
            if (sampler.mountainInfluence(x, z) < 0.35) continue;
            int index = candidates < xs.length ? candidates : selection.nextInt(candidates + 1);
            if (index < xs.length) { xs[index] = x; zs[index] = z; }
            candidates++;
        }
        if (candidates < xs.length) throw new AssertionError("Insufficient mountain benchmark coordinates");
        System.out.printf("Mountain benchmark: seed=%d samples=%d candidates=%d%n", sampler.seed(), xs.length, candidates);
    }

    private static void mountainQueries(TerrainSampler sampler, TerrainColumn column, int[] xs, int[] zs,
                                        int count, boolean heightOnly) {
        long sum = 0;
        for (int i = 0; i < count; i++) {
            int index = i % xs.length;
            if (heightOnly) sum += sampler.sampleHeight(xs[index], zs[index]);
            else {
                sampler.sampleInto(xs[index], zs[index], column);
                sum += column.height() + Float.floatToIntBits(column.humidity());
            }
        }
        blackhole = sum;
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
