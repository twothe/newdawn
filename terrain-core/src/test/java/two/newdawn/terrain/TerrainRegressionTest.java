package two.newdawn.terrain;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/** Tests production sampling against captured original-code results, including concurrent access. */
public final class TerrainRegressionTest {
    public static void main(String[] args) throws Exception {
        List<Fixture> fixtures = new ArrayList<>();
        var resource = TerrainRegressionTest.class.getResourceAsStream("/legacy-terrain.csv");
        if (resource == null) throw new AssertionError("Legacy fixture is missing");
        try (var reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            reader.readLine();
            for (String line; (line = reader.readLine()) != null;) {
                String[] fields = line.split(",");
                fixtures.add(new Fixture(Long.parseLong(fields[0]), Integer.parseInt(fields[1]), Integer.parseInt(fields[2]),
                        new TerrainSample(Integer.parseInt(fields[3]), Integer.parseInt(fields[4]), Boolean.parseBoolean(fields[5]),
                                Float.intBitsToFloat(Integer.parseInt(fields[6])), Float.intBitsToFloat(Integer.parseInt(fields[7])),
                                Integer.parseInt(fields[8]))));
            }
        }
        Map<Long, TerrainSampler> samplers = new java.util.HashMap<>();
        for (Fixture fixture : fixtures) samplers.computeIfAbsent(fixture.seed, TerrainSampler::new);
        for (Fixture fixture : fixtures) verify(samplers.get(fixture.seed), fixture);
        verifyBuffers(fixtures, samplers);
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int worker = 0; worker < 8; worker++) {
                final int offset = worker;
                tasks.add(() -> {
                    TerrainColumn column = new TerrainColumn();
                    for (int index = offset; index < fixtures.size(); index += 8) {
                        Fixture fixture = fixtures.get(index);
                        verify(samplers.get(fixture.seed), fixture);
                        samplers.get(fixture.seed).sampleInto(fixture.x, fixture.z, column);
                        require(column.snapshot().equals(fixture.expected), "Worker-owned buffer changed output");
                    }
                    return null;
                });
            }
            for (var future : executor.invokeAll(tasks)) future.get();
        }
        TerrainSampler sampler = samplers.get(0L);
        for (int chunkX = -2; chunkX <= 2; chunkX++) {
            for (int chunkZ = -2; chunkZ <= 2; chunkZ++) {
                TerrainSample[] chunk = sampler.sampleChunk(chunkX, chunkZ);
                for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                    require(chunk[x + z * 16].equals(sampler.sample(chunkX * 16 + x, chunkZ * 16 + z)), "Chunk indexing mismatch");
                }
            }
        }
        require(!sampler.sample(100, 100).equals(samplers.get(1L).sample(100, 100)), "World seed was ignored");
        TerrainSample threshold = new TerrainSample(64, 0, false, -0.5f, 0.18f, 3);
        require(threshold.temperatureBand() == 0 && threshold.humidityBand() == 2, "Inclusive climate thresholds changed");
        require(BiomePalette.select(threshold).biome().equals("snowy_beach"), "Shore precedence changed");
        require(BiomePalette.select(new TerrainSample(63, 0, true, 0f, 0f, 3)).biome().equals("beach"), "Submerged mountain masks shore");
        require(BiomePalette.select(new TerrainSample(59, 0, false, 0f, 0f, 3)).biome().equals("deep_ocean"), "Ocean threshold changed");
        try {
            sampler.sampleChunk(Integer.MAX_VALUE, 0);
            throw new AssertionError("Overflowing chunk coordinate accepted");
        } catch (ArithmeticException expected) { /* Invalid coordinate rejected at the public boundary. */ }
        verifyBufferContracts(sampler);
        System.out.println("PASS: " + fixtures.size() + " original-code fixtures, exact climate float bits, 8-worker determinism, chunk indexing and boundaries");
    }

    private static void verifyBuffers(List<Fixture> fixtures, Map<Long, TerrainSampler> samplers) {
        TerrainColumn column = new TerrainColumn();
        TerrainColumn copied = new TerrainColumn();
        TerrainChunk chunk = new TerrainChunk();
        long previousSeed = 42;
        int previousX = Integer.MAX_VALUE, previousZ = Integer.MAX_VALUE;
        for (Fixture fixture : fixtures) {
            TerrainSampler sampler = samplers.get(fixture.seed);
            sampler.sampleInto(fixture.x, fixture.z, column);
            require(column.snapshot().equals(fixture.expected), "Buffered point differs from original fixture: " + fixture);
            require(BiomePalette.select(column).equals(BiomePalette.select(fixture.expected)), "Buffered biome differs from snapshot selection");
            require(sampler.sampleHeight(fixture.x, fixture.z) == fixture.expected.height(), "Height-only path differs from fixture");
            int chunkX = fixture.x >> 4, chunkZ = fixture.z >> 4;
            if (previousSeed != fixture.seed || previousX != chunkX || previousZ != chunkZ) {
                sampler.sampleChunkInto(chunkX, chunkZ, chunk);
                previousSeed = fixture.seed; previousX = chunkX; previousZ = chunkZ;
            }
            int index = (fixture.x & 15) + (fixture.z & 15) * 16;
            chunk.copyColumn(index, copied);
            require(copied.snapshot().equals(fixture.expected), "Batch output differs from original fixture: " + fixture);
            require(chunk.height(index) == copied.height() && chunk.regionHeight(index) == copied.regionHeight()
                    && chunk.mountain(index) == copied.mountain() && chunk.temperature(index) == copied.temperature()
                    && chunk.humidity(index) == copied.humidity() && chunk.fillerDepth(index) == copied.fillerDepth(), "Batch getters differ");
        }
    }

    private static void verifyBufferContracts(TerrainSampler sampler) {
        TerrainColumn column = new TerrainColumn();
        TerrainChunk first = new TerrainChunk(), second = new TerrainChunk();
        expect(IllegalStateException.class, column::height);
        expect(IllegalStateException.class, () -> first.height(0));
        expect(IllegalStateException.class, first::maximumHeight);
        expect(NullPointerException.class, () -> sampler.sampleInto(0, 0, null));
        expect(NullPointerException.class, () -> sampler.sampleChunkInto(0, 0, null));
        sampler.sampleChunkInto(-1, -1, first);
        first.copyColumn(0, column);
        TerrainSample retained = column.snapshot();
        expect(IndexOutOfBoundsException.class, () -> first.copyColumn(256, column));
        expect(IndexOutOfBoundsException.class, () -> first.height(-1));
        expect(ArithmeticException.class, () -> sampler.sampleChunkInto(0, Integer.MAX_VALUE, first));
        first.copyColumn(0, column);
        require(column.snapshot().equals(retained), "Rejected request modified valid data");
        sampler.sampleChunkInto(0, 0, second);
        first.copyColumn(0, column);
        require(column.snapshot().equals(retained), "Buffers share writable storage");
        sampler.sampleInto(1700, 1900, column);
        require(retained.equals(sampler.sample(-16, -16)), "Snapshot changed after buffer reuse");
        int maximum = 0;
        for (int i = 0; i < TerrainChunk.COLUMN_COUNT; i++) maximum = Math.max(maximum, first.height(i));
        require(maximum == first.maximumHeight(), "Incorrect batch maximum height");
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("Unexpected exception", failure);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static void verify(TerrainSampler sampler, Fixture fixture) {
        TerrainSample actual = sampler.sample(fixture.x, fixture.z);
        require(actual.equals(fixture.expected), "Legacy mismatch at " + fixture + ": " + actual);
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }

    private record Fixture(long seed, int x, int z, TerrainSample expected) {}
}
