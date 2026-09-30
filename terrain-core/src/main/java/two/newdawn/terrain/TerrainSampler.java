package two.newdawn.terrain;

import java.util.Random;
import java.util.Objects;

/**
 * Seed-specific legacy terrain and climate composition, safe for concurrent sampling.
 * All random numbers are consumed during construction in the original 1.7.10 order.
 * The legacy 256-block scale deliberately remains independent of the game's build height.
 */
public final class TerrainSampler {
    public static final int SEA_LEVEL = 64;
    // The altitude correction depends only on the rounded legacy height, not on seed or coordinates.
    private static final double[] ALTITUDE_CORRECTION = altitudeCorrections();
    private final long seed;
    private final Field roughness, block, small, large, region, filler;
    private final Field temperatureLocal, temperatureArea, temperatureRegion;
    private final Field humidityLocal, humidityArea, humidityRegion, forest;
    private final Field hills, hillsBlock, hillsSmall, hillsLarge;

    public TerrainSampler(long seed) {
        this.seed = seed;
        Random random = new Random(seed);
        SimplexNoise noise = new SimplexNoise(random);
        roughness = field(noise, random, 1524.0, 1798.0);
        block = field(noise, random, 23.0, 27.0);
        small = field(noise, random, 413.0, 467.0);
        large = field(noise, random, 913.0, 967.0);
        region = field(noise, random, 1920.0, 1811.0);
        filler = field(noise, random, 16.0, 16.0);
        temperatureLocal = field(noise, random, 2.1, 2.2);
        temperatureArea = field(noise, random, 260.0, 273.0);
        temperatureRegion = field(noise, random, 2420.0, 2590.0);
        humidityLocal = field(noise, random, 6.0, 7.0);
        humidityArea = field(noise, random, 320.0, 273.0);
        humidityRegion = field(noise, random, 1080.0, 919.0);
        forest = field(noise, random, 93.0, 116.0);
        hills = field(noise, random, 897.0, 957.0);
        hillsBlock = field(noise, random, 2.0, 2.2);
        hillsSmall = field(noise, random, 41.0, 45.0);
        hillsLarge = field(noise, random, 127.0, 119.0);
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
        int height = calculateRelief(x, z, target);
        double altitude = ALTITUDE_CORRECTION[height];
        double temperature = temperatureRegion.at(x, z) * 0.8 + temperatureArea.at(x, z) * 0.15
                + temperatureLocal.at(x, z) * 0.05 + altitude;
        double humidity = humidityRegion.at(x, z) * 0.40 + humidityArea.at(x, z) * 0.55
                + humidityLocal.at(x, z) * 0.05
                + (forest.at(x, z) > (temperature >= 0.5f ? 0.85 : 0.60) ? 0.5 : 0.0) + altitude;
        target.temperature = (float) temperature;
        target.humidity = (float) humidity;
        target.fillerDepth = (int) Math.round((filler.at(x, z) + 1.0) * 1.5 * 2.0);
    }

    /** First air/water Y, without calculating climate, filler or allocating a result object. */
    public int sampleHeight(int x, int z) {
        return calculateRelief(x, z, null);
    }

    private int calculateRelief(int x, int z, TerrainColumn target) {
        double terrainRoughness = roughness.at(x, z) + 1.0;
        double localHeight = block.at(x, z) * terrainRoughness * 0.5 * 2.0;
        double smallHeight = small.at(x, z) * terrainRoughness * 6.0 * 2.0;
        double largeHeight = large.at(x, z) * terrainRoughness * 10.0 * 2.0;
        double regionHeight = (region.at(x, z) + 0.25) * 8.0 / 1.25 * 2.0;
        double baseHeight = SEA_LEVEL + regionHeight + largeHeight + smallHeight + localHeight;
        double hillFactor = hills.at(x, z);
        if (hillFactor >= 0.0) {
            hillFactor = -(Math.cos(Math.PI * Math.pow(hillFactor, 4.0)) - 1.0) / 2.0;
        }
        double hillHeight = hillFactor >= 0.01
                ? (hillsLarge.at(x, z) * 0.4 + hillsSmall.at(x, z) * 0.59
                + hillsBlock.at(x, z) * 0.01 + 0.5) * hillFactor * 32.0 * 2.0 : 0.0;
        int height = Math.max(1, Math.min(255, (int) Math.round(baseHeight + hillHeight)));
        if (target != null) {
            target.height = height;
            target.regionHeight = (int) Math.round(regionHeight);
            target.mountain = hillHeight > 4;
        }
        return height;
    }

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

    private static double[] altitudeCorrections() {
        double[] corrections = new double[256];
        for (int height = 1; height < corrections.length; height++) {
            double shifted = height + SEA_LEVEL - 256 / 2.0;
            corrections[height] = shifted < 0.0 ? 0.0
                    : -Math.pow(shifted / 256.0, 3.0) * Math.pow(shifted * 0.4, 1.001);
        }
        return corrections;
    }

    private static Field field(SimplexNoise noise, Random random, double x, double z) {
        return new Field(noise, x, z, random.nextDouble(), random.nextDouble());
    }

    private record Field(SimplexNoise noise, double scaleX, double scaleZ, double offsetX, double offsetZ) {
        double at(int x, int z) { return noise.noise(x / scaleX + offsetX, z / scaleZ + offsetZ); }
    }
}
