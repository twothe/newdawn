package two.newdawn.terrain;

import java.util.Random;
import java.util.Objects;

/**
 * Seed-specific terrain and climate composition with pointwise multifractal mountains, safe for concurrent sampling.
 * Random numbers are consumed only during construction; new fields follow the original 1.7.10 fields.
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
    private final Field cliffs;

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
        hills = field(noise, random, 897.0*1.8, 957.0*1.8);
        hillsBlock = field(noise, random, 2.0, 2.2);
        hillsSmall = field(noise, random, 41.0, 45.0);
        hillsLarge = field(noise, random, 127.0, 119.0);
        // Append new fields so existing terrain, climate and mountain offsets remain unchanged.
        cliffs = field(noise, random, 181.0, 163.0);
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
        double broadTemperature = temperatureRegion.at(x, z) * 0.8 + temperatureArea.at(x, z) * 0.15;
        double broadHumidity = humidityRegion.at(x, z) * 0.40 + humidityArea.at(x, z) * 0.55;
        int height = calculateRelief(x, z, broadTemperature, broadHumidity, target);
        double altitude = ALTITUDE_CORRECTION[height];
        double temperature = broadTemperature + temperatureLocal.at(x, z) * 0.05 + altitude;
        double humidity = broadHumidity + humidityLocal.at(x, z) * 0.05
                + (forest.at(x, z) > (temperature >= 0.5f ? 0.85 : 0.65) ? 0.5 : 0.0) + altitude;
        target.temperature = (float) temperature;
        target.humidity = (float) humidity;
        target.fillerDepth = (int) Math.round((filler.at(x, z) + 1.0) * 1.5 * 2.0);
    }

    /** First air/water Y; evaluates broad climate only inside mountains, without filler or allocations. */
    public int sampleHeight(int x, int z) {
        return calculateRelief(x, z, 0.0, 0.0, null);
    }

    private int calculateRelief(int x, int z, double broadTemperature, double broadHumidity, TerrainColumn target) {
        double terrainRoughness = roughness.at(x, z) + 1.0;
        double blockNoise = block.at(x, z);
        double localHeight = blockNoise * terrainRoughness * 0.5 * 2.0;
        double smallNoise = small.at(x, z);
        double largeNoise = large.at(x, z);
        double smallHeight = smallNoise * terrainRoughness * 6.0 * 2.0;
        double largeHeight = largeNoise * terrainRoughness * 10.0 * 2.0;
        double regionHeight = (region.at(x, z) + 0.25) * 8.0 / 1.25 * 2.0;
        double baseHeight = SEA_LEVEL + regionHeight + largeHeight + smallHeight + localHeight;
        double hillFactor = mountainInfluence(x, z);
        double hillHeight = 0.0;
        if (target != null) target.exposedRock = false;
        if (hillFactor > 0.0) {
            if (target == null) {
                broadTemperature = temperatureRegion.at(x, z) * 0.8 + temperatureArea.at(x, z) * 0.15;
                broadHumidity = humidityRegion.at(x, z) * 0.40 + humidityArea.at(x, z) * 0.55;
            }
            // Reuse existing broad terrain signals to bend the ridges, without extra noise queries.
            double warpedX = x + smallNoise * 18.0;
            double warpedZ = z + largeNoise * 18.0;
            double mainRidge = hillsLarge.at(warpedX, warpedZ);
            double secondaryRidge = hillsSmall.at(warpedX + mainRidge * 9.0, warpedZ - mainRidge * 9.0);
            // Signed broad fields keep the massif filled instead of folding its body into ridge walls.
            double broadVariation = Math.max(0.0, Math.min(1.0, 0.5 + largeNoise * 0.325 + smallNoise * 0.175));
            hillHeight = MountainProfile.height(hillFactor, broadVariation, mainRidge, secondaryRidge,
                    blockNoise, hillsBlock.at(x, z), broadTemperature, broadHumidity);
            double cliffStrength = CliffProfile.strength(hillFactor, broadTemperature, broadHumidity,
                    mainRidge * 0.70 + smallNoise * 0.30);
            if (cliffStrength > 0.0) {
                double cliffNoise = cliffs.at(warpedX + secondaryRidge * 7.0, warpedZ - mainRidge * 11.0);
                hillHeight += CliffProfile.height(cliffStrength, cliffNoise, blockNoise, secondaryRidge, target);
            }
        }
        int height = Math.max(1, Math.min(255, (int) Math.round(baseHeight + hillHeight)));
        if (target != null) {
            target.height = height;
            target.regionHeight = (int) Math.round(regionHeight);
            // A raised base-terrain hill is not a mountain, even at a high absolute elevation.
            target.mountain = hillHeight >= 24;
            target.biomeHeightOffset = (float) (Math.max(-1.0, Math.min(1.0, blockNoise)) * 6.0);
        }
        return height;
    }

    /** Mountain coverage from the current seed-specific field. */
    double mountainInfluence(int x, int z) { return MountainProfile.influence(hills.at(x, z)); }

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
            // Preserve the lowland curve; its fourth-power growth overwhelms climate on taller peaks.
            double shifted = Math.min(height, 128) + SEA_LEVEL - 256 / 2.0;
            corrections[height] = shifted < 0.0 ? 0.0
                    : -Math.pow(shifted / 256.0, 3.0) * Math.pow(shifted * 0.4, 1.001);
            corrections[height] -= Math.max(0, height - 128) * 0.004;
        }
        return corrections;
    }

    private static Field field(SimplexNoise noise, Random random, double scaleX, double scaleZ) {
        return new Field(noise, scaleX, scaleZ, random.nextDouble(), random.nextDouble());
    }

    private record Field(SimplexNoise noise, double scaleX, double scaleZ, double offsetX, double offsetZ) {
        double at(double x, double z) { return noise.noise(x / scaleX + offsetX, z / scaleZ + offsetZ); }
    }
}
