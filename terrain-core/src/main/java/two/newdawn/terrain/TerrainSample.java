package two.newdawn.terrain;

/** Immutable column information. Height is the first air/water Y, not the top solid Y. */
public record TerrainSample(int height, int regionHeight, boolean mountain, float temperature,
                            float humidity, int fillerDepth) {
    public int temperatureBand() {
        return temperatureBand(temperature);
    }

    public static int temperatureBand(float temperature) {
        return temperature <= -0.5f ? 0 : temperature >= 0.5f ? 2 : 1;
    }

    public int humidityBand() {
        return humidityBand(humidity);
    }

    public static int humidityBand(float humidity) {
        return humidity <= -0.5f ? 0 : humidity >= 0.6f ? 3 : humidity >= 0.18f ? 2 : 1;
    }
}
