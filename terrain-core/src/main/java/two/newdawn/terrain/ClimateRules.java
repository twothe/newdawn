package two.newdawn.terrain;

import static two.newdawn.terrain.NoiseMath.smooth;

/** Pointwise climate and weathering rules. Broad climate shapes relief; altitude only affects final biomes. */
final class ClimateRules {
    private static final int COOLING_START_HEIGHT = 80;
    private static final double COOLING_ONSET_HEIGHT = 48.0;
    private static final double COOLING_RATE = 0.004;
    private static final double[] ALTITUDE_COOLING = altitudeCooling();
    static final double FOREST_THRESHOLD = 0.65;
    static final double HOT_FOREST_THRESHOLD = 0.85;
    private final TerrainNoise noise;

    ClimateRules(TerrainNoise noise) { this.noise = noise; }

    double broadTemperature(int x, int z) {
        return noise.temperatureRegion.at(x, z) * 0.8 + noise.temperatureArea.at(x, z) * 0.15;
    }

    double broadHumidity(int x, int z) {
        return noise.humidityRegion.at(x, z) * 0.40 + noise.humidityArea.at(x, z) * 0.55;
    }

    /** Complete biome climate after relief is known, without feeding altitude back into the height profile. */
    void complete(int x, int z, double broadTemperature, double broadHumidity, TerrainColumn target) {
        double temperature = broadTemperature + noise.temperatureLocal.at(x, z) * 0.05 - coolingAt(target.height);
        double humidity = humidityWithForest(broadHumidity + noise.humidityLocal.at(x, z) * 0.05,
                temperature, noise.forest.at(x, z));
        target.temperature = (float) temperature;
        target.humidity = (float) humidity;
    }

    static double dryness(double humidity) { return 1.0 - smooth(-0.45, 0.30, humidity); }

    /** Moisture and a cool transition climate favor broken rock; permanent cold is not amplified. */
    static double frostWeathering(double temperature, double humidity) {
        return smooth(-0.70, -0.25, temperature) * (1.0 - smooth(-0.15, 0.15, temperature))
                * smooth(-0.35, 0.35, humidity);
    }

    /** Smoothly starting temperature lapse; physical heights 1–255 are the sampler's valid range. */
    static double coolingAt(int height) { return ALTITUDE_COOLING[height]; }

    /** Intentional one-sided woodland patches preserve local variety and wood access; height does not dry climate. */
    static double humidityWithForest(double humidity, double temperature, double forestNoise) {
        return humidityWithForestPatch(humidity, isForestPatch(temperature, forestNoise));
    }

    static double humidityWithForestPatch(double humidity, boolean forestPatch) {
        return humidity + (forestPatch ? 0.5 : 0.0);
    }

    static boolean isForestPatch(double temperature, double forestNoise) {
        return forestNoise > (temperature >= 0.5f ? HOT_FOREST_THRESHOLD : FOREST_THRESHOLD);
    }

    private static double[] altitudeCooling() {
        double[] corrections = new double[256];
        for (int height = 1; height < corrections.length; height++) {
            double elevation = Math.max(0, height - COOLING_START_HEIGHT);
            corrections[height] = COOLING_RATE * elevation * elevation / (elevation + COOLING_ONSET_HEIGHT);
        }
        return corrections;
    }

}
