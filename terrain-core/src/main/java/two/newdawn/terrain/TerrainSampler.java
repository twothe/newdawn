package two.newdawn.terrain;

import java.util.Objects;

/**
 * Seed-specific terrain and climate composition with pointwise multifractal mountains, safe for concurrent sampling.
 * Random numbers are consumed only during construction; new fields follow the original 1.7.10 fields.
 * The legacy 256-block scale deliberately remains independent of the game's build height.
 */
public final class TerrainSampler {
    public static final int SEA_LEVEL = 64;
    private final long seed;
    private final TerrainNoise noise;
    private final ClimateRules climate;
    private final TerrainRelief relief;

    public TerrainSampler(long seed) {
        this.seed = seed;
        noise = new TerrainNoise(seed);
        climate = new ClimateRules(noise);
        relief = new TerrainRelief(noise, climate);
    }

    public long seed() { return seed; }

    /** Immutable convenience result. Hot loops should use sampleInto with a caller-owned buffer. */
    public TerrainSample sample(int x, int z) {
        TerrainColumn column = new TerrainColumn();
        sampleInto(x, z, column);
        return column.snapshot();
    }

    /** Samples absolute block coordinates without allocating; overwrites every field of target. */
    public void sampleInto(int x, int z, TerrainColumn target) {
        Objects.requireNonNull(target, "target");
        double temperature = climate.broadTemperature(x, z);
        double humidity = climate.broadHumidity(x, z);
        relief.sampleInto(x, z, temperature, humidity, target);
        climate.complete(x, z, temperature, humidity, target);
        target.fillerDepth = (int) Math.round((noise.filler.at(x, z) + 1.0) * 1.5 * 2.0);
    }

    /** First air/water Y; evaluates broad climate only inside mountains, without filler or allocations. */
    public int sampleHeight(int x, int z) { return relief.height(x, z); }

    /** Final selection humidity, including woodland boost, without sampling unused column outputs or allocating. */
    public float sampleHumidity(int x, int z) {
        double humidity = climate.broadHumidity(x, z) + noise.humidityLocal.at(x, z) * 0.05;
        return (float) ClimateRules.humidityWithForestPatch(humidity, isForestPatch(x, z));
    }

    /** Existing woodland overlay at a block coordinate; allocation-free and independent of surrounding columns. */
    public boolean isForestPatch(int x, int z) {
        double forest = noise.forest.at(x, z);
        if (forest <= ClimateRules.FOREST_THRESHOLD) return false;
        if (forest > ClimateRules.HOT_FOREST_THRESHOLD) return true;
        double temperature = climate.broadTemperature(x, z) + noise.temperatureLocal.at(x, z) * 0.05
                - ClimateRules.coolingAt(sampleHeight(x, z));
        return ClimateRules.isForestPatch(temperature, forest);
    }

    /** Mountain coverage from the current seed-specific field. */
    double mountainInfluence(int x, int z) { return relief.mountainInfluence(x, z); }

    /** Snapshot convenience API. Prefer sampleChunkInto for generation or repeated batch queries. */
    public TerrainSample[] sampleChunk(int chunkX, int chunkZ) {
        TerrainSample[] result = new TerrainSample[256];
        int startX = Math.multiplyExact(chunkX, 16);
        int startZ = Math.multiplyExact(chunkZ, 16);
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                result[x + z * 16] = sample(Math.addExact(startX, x), Math.addExact(startZ, z));
            }
        }
        return result;
    }

    /**
     * Overwrites all 256 columns without allocation, in local X + 16 * local Z order.
     * Invalid chunk coordinates are rejected before modifying the buffer.
     */
    public void sampleChunkInto(int chunkX, int chunkZ, TerrainChunk target) {
        Objects.requireNonNull(target, "target");
        int startX = Math.multiplyExact(chunkX, TerrainChunk.SIDE);
        int startZ = Math.multiplyExact(chunkZ, TerrainChunk.SIDE);
        for (int z = 0; z < TerrainChunk.SIDE; z++) {
            for (int x = 0; x < TerrainChunk.SIDE; x++) {
                sampleInto(startX + x, startZ + z, target.workspace);
                target.store(x + z * TerrainChunk.SIDE, target.workspace);
            }
        }
    }

}
