package two.newdawn.terrain;

import static two.newdawn.terrain.NoiseMath.smooth;

/** Pointwise climate and weathering rules. Broad climate shapes relief; altitude only affects final biomes. */
final class ClimateRules {
    private static final double[] ALTITUDE_CORRECTION = altitudeCorrections();
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
        double altitude = ALTITUDE_CORRECTION[target.height];
        double temperature = broadTemperature + noise.temperatureLocal.at(x, z) * 0.05 + altitude;
        double humidity = broadHumidity + noise.humidityLocal.at(x, z) * 0.05
                + (noise.forest.at(x, z) > (temperature >= 0.5f ? 0.85 : 0.65) ? 0.5 : 0.0) + altitude;
        target.temperature = (float) temperature;
        target.humidity = (float) humidity;
    }

    static double dryness(double humidity) { return 1.0 - smooth(-0.45, 0.30, humidity); }

    /** Moisture and a cool transition climate favor broken rock; permanent cold is not amplified. */
    static double frostWeathering(double temperature, double humidity) {
        return smooth(-0.70, -0.25, temperature) * (1.0 - smooth(-0.15, 0.15, temperature))
                * smooth(-0.35, 0.35, humidity);
    }

    private static double[] altitudeCorrections() {
        double[] corrections = new double[256];
        for (int height = 1; height < corrections.length; height++) {
            // Preserve the lowland curve; its fourth-power growth overwhelms climate on taller peaks.
            double shifted = Math.min(height, 128) + TerrainSampler.SEA_LEVEL - 256 / 2.0;
            corrections[height] = shifted < 0.0 ? 0.0
                    : -Math.pow(shifted / 256.0, 3.0) * Math.pow(shifted * 0.4, 1.001);
            corrections[height] -= Math.max(0, height - 128) * 0.004;
        }
        return corrections;
    }

}
