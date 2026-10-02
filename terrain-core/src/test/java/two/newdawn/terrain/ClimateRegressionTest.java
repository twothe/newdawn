package two.newdawn.terrain;

/** Stable climate contracts: altitude cools smoothly, while the forest overlay remains positive and climate-dependent. */
public final class ClimateRegressionTest {
    public static void main(String[] arguments) {
        double previous = 0;
        for (int height = 1; height <= 255; height++) {
            double cooling = ClimateRules.coolingAt(height);
            require(Double.isFinite(cooling) && cooling >= previous, "Cooling must be finite and monotone");
            require(cooling - previous < 0.01, "Cooling contains an abrupt altitude step");
            previous = cooling;
        }
        require(ClimateRules.coolingAt(TerrainSampler.SEA_LEVEL) == 0, "Sea-level climate was cooled");
        require(ClimateRules.coolingAt(255) > ClimateRules.coolingAt(128), "High mountains have no further cooling");
        for (double humidity : new double[]{-0.8, 0, 0.8}) {
            require(ClimateRules.humidityWithForest(humidity, 0.2, 0.65) == humidity, "Forest trigger must be exclusive");
            require(ClimateRules.humidityWithForest(humidity, 0.2, Math.nextUp(0.65)) == humidity + 0.5,
                    "Forest patch lost its positive increment");
            require(ClimateRules.humidityWithForest(humidity, 0.5, 0.85) == humidity, "Hot forest trigger must be exclusive");
            require(ClimateRules.humidityWithForest(humidity, 0.5, Math.nextUp(0.85)) == humidity + 0.5,
                    "Hot forest patch lost its positive increment");
            require(ClimateRules.humidityWithForest(humidity, 0.49, 0.70) == humidity + 0.5,
                    "Temperate patch threshold drifted");
            require(ClimateRules.humidityWithForest(humidity, 0.5, 0.70) == humidity,
                    "Hot patch threshold drifted");
        }
        // Exercise the real composition at different physical elevations with the same noise inputs.
        var climate = new ClimateRules(new TerrainNoise(1));
        var low = new TerrainColumn();
        var high = new TerrainColumn();
        low.height = 64; high.height = 255;
        climate.complete(123, -456, 2.0, 0.2, low);
        climate.complete(123, -456, 2.0, 0.2, high);
        require(high.temperature < low.temperature, "Altitude did not lower selection temperature");
        require(high.humidity == low.humidity, "Altitude changed humidity despite an unchanged forest trigger");
        for (long seed : new long[]{1, 123456789, -8458999313514431577L}) {
            var sampler = new TerrainSampler(seed);
            var noise = new TerrainNoise(seed);
            var rules = new ClimateRules(noise);
            var column = new TerrainColumn();
            int patches = 0;
            for (int index = 0; index < 4096; index++) {
                int x = (index % 64 - 32) * 97, z = (index / 64 - 32) * 89;
                sampler.sampleInto(x, z, column);
                // Compare the independent query against the actual full-column humidity increment.
                double humidity = rules.broadHumidity(x, z) + noise.humidityLocal.at(x, z) * 0.05;
                require(sampler.sampleHumidity(x, z) == column.humidity(), "Humidity-only query drifted from full sampling");
                boolean expected = column.humidity() - humidity > 0.25;
                require(sampler.isForestPatch(x, z) == expected, "Tree mask disagrees with biome woodland overlay");
                if (expected) patches++;
            }
            require(patches > 0 && patches < 4096, "Forest mask must retain patches and open areas");
        }
        System.out.println("PASS: temperature-only smooth altitude cooling and intentional one-sided forest patches");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
