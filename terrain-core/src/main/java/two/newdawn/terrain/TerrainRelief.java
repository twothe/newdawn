package two.newdawn.terrain;

/** Composes base terrain, mountain bodies and cliffs from this column alone; all scratch values are local. */
final class TerrainRelief {
    private static final double BLOCK_SCALE = 2.0;
    private static final double LOCAL_AMPLITUDE = 0.5;
    private static final double SMALL_AMPLITUDE = 6.0;
    private static final double LARGE_AMPLITUDE = 10.0;
    private static final double REGION_AMPLITUDE = 8.0;
    private static final double RIDGE_WARP = 18.0;
    private static final double MOUNTAIN_RELIEF_MINIMUM = 24.0;
    private static final double BIOME_HEIGHT_VARIATION = 6.0;
    private final TerrainNoise noise;
    private final ClimateRules climate;

    TerrainRelief(TerrainNoise noise, ClimateRules climate) {
        this.noise = noise;
        this.climate = climate;
    }

    int height(int x, int z) { return compose(x, z, 0.0, 0.0, false, null); }

    void sampleInto(int x, int z, double temperature, double humidity, TerrainColumn target) {
        compose(x, z, temperature, humidity, true, target);
    }

    private int compose(int x, int z, double broadTemperature, double broadHumidity, boolean climateAvailable, TerrainColumn target) {
        double terrainRoughness = noise.roughness.at(x, z) + 1.0;
        double blockNoise = noise.block.at(x, z);
        double localHeight = blockNoise * terrainRoughness * LOCAL_AMPLITUDE * BLOCK_SCALE;
        double smallNoise = noise.small.at(x, z);
        double largeNoise = noise.large.at(x, z);
        double smallHeight = smallNoise * terrainRoughness * SMALL_AMPLITUDE * BLOCK_SCALE;
        double largeHeight = largeNoise * terrainRoughness * LARGE_AMPLITUDE * BLOCK_SCALE;
        double regionHeight = (noise.region.at(x, z) + 0.25) * REGION_AMPLITUDE / 1.25 * BLOCK_SCALE;
        double baseHeight = TerrainSampler.SEA_LEVEL + regionHeight + largeHeight + smallHeight + localHeight;
        double hillFactor = mountainInfluence(x, z);
        double hillHeight = 0.0;
        if (target != null) target.exposedRock = false;
        if (hillFactor > 0.0) {
            if (!climateAvailable) {
                broadTemperature = climate.broadTemperature(x, z);
                broadHumidity = climate.broadHumidity(x, z);
            }
            // Reuse existing broad terrain signals to bend the ridges, without extra noise queries.
            double warpedX = x + smallNoise * RIDGE_WARP;
            double warpedZ = z + largeNoise * RIDGE_WARP;
            double mainRidge = noise.hillsLarge.at(warpedX, warpedZ);
            double secondaryRidge = noise.hillsSmall.at(warpedX + mainRidge * 9.0, warpedZ - mainRidge * 9.0);
            // Signed broad fields keep the massif filled instead of folding its body into ridge walls.
            double broadVariation = Math.max(0.0, Math.min(1.0, 0.5 + largeNoise * 0.325 + smallNoise * 0.175));
            double dry = ClimateRules.dryness(broadHumidity);
            double frost = ClimateRules.frostWeathering(broadTemperature, broadHumidity);
            hillHeight = MountainProfile.heightFromWeathering(hillFactor, broadVariation, mainRidge, secondaryRidge,
                    blockNoise, noise.hillsBlock.at(x, z), dry, frost);
            double cliffStrength = CliffProfile.strengthFromWeathering(hillFactor, dry, frost,
                    mainRidge * 0.70 + smallNoise * 0.30);
            if (cliffStrength > 0.0) {
                double cliffNoise = noise.cliffs.at(warpedX + secondaryRidge * 7.0, warpedZ - mainRidge * 11.0);
                hillHeight += CliffProfile.height(cliffStrength, cliffNoise, blockNoise, secondaryRidge, target);
            }
        }
        int height = Math.max(1, Math.min(255, (int) Math.round(baseHeight + hillHeight)));
        if (target != null) {
            target.height = height;
            target.regionHeight = (int) Math.round(regionHeight);
            // A raised base-terrain hill is not a mountain, even at a high absolute elevation.
            target.mountain = hillHeight >= MOUNTAIN_RELIEF_MINIMUM;
            target.biomeHeightOffset = (float) (Math.max(-1.0, Math.min(1.0, blockNoise)) * BIOME_HEIGHT_VARIATION);
        }
        return height;
    }

    /** Mountain coverage from the current seed-specific field. */
    double mountainInfluence(int x, int z) { return MountainProfile.influence(noise.hills.at(x, z)); }

}
