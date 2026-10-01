package two.newdawn.terrain;

import java.util.Random;

/** Immutable seed-specific noise catalog. Scales are in blocks; append fields to preserve existing offsets. */
final class TerrainNoise {
    private static final double MOUNTAIN_SPREAD = 1.8;
    final Field roughness, block, small, large, region, filler;
    final Field temperatureLocal, temperatureArea, temperatureRegion;
    final Field humidityLocal, humidityArea, humidityRegion, forest;
    final Field hills, hillsBlock, hillsSmall, hillsLarge;
    final Field cliffs;

    TerrainNoise(long seed) {
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
        hills = field(noise, random, 897.0 * MOUNTAIN_SPREAD, 957.0 * MOUNTAIN_SPREAD);
        hillsBlock = field(noise, random, 2.0, 2.2);
        hillsSmall = field(noise, random, 41.0, 45.0);
        hillsLarge = field(noise, random, 127.0, 119.0);
        // Append new fields so existing terrain, climate and mountain offsets remain unchanged.
        cliffs = field(noise, random, 181.0, 163.0);
    }

    private static Field field(SimplexNoise noise, Random random, double scaleX, double scaleZ) {
        if (!Double.isFinite(scaleX) || !Double.isFinite(scaleZ) || scaleX <= 0 || scaleZ <= 0) {
            throw new IllegalArgumentException("Noise scales must be finite and positive");
        }
        return new Field(noise, 1.0 / scaleX, 1.0 / scaleZ, random.nextDouble(), random.nextDouble());
    }

    /** Precomputed reciprocals remove two divisions per query; tuning above remains in block units. */
    record Field(SimplexNoise noise, double inverseScaleX, double inverseScaleZ, double offsetX, double offsetZ) {
        double at(double x, double z) { return noise.noise(x * inverseScaleX + offsetX, z * inverseScaleZ + offsetZ); }
    }
}
