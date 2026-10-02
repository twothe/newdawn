package two.newdawn.terrain;

import java.util.function.IntToDoubleFunction;

/**
 * Opt-in measurements of actual production components with varying, precomputed inputs.
 * Isolated costs include loop/dispatch overhead and are not additive shares of full generation.
 * Use the separate JFR task to attribute time in the complete sampling workload.
 */
public final class ComponentBenchmark {
    private static final int INPUTS = 4096;
    private static final int OPERATIONS = 2_097_152;

    public static void main(String[] args) {
        var noise = new TerrainNoise(123456789L);
        var climate = new ClimateRules(noise);
        var sampler = new TerrainSampler(123456789L);
        var columns = new TerrainColumn[INPUTS];
        double[] temperature = new double[INPUTS], humidity = new double[INPUTS];
        double[] main = new double[INPUTS], secondary = new double[INPUTS], local = new double[INPUTS];
        double[] dry = new double[INPUTS], frost = new double[INPUTS], influence = new double[INPUTS];
        for (int i = 0; i < INPUTS; i++) {
            int x = x(i), z = z(i);
            columns[i] = new TerrainColumn();
            sampler.sampleInto(x, z, columns[i]);
            temperature[i] = climate.broadTemperature(x, z);
            humidity[i] = climate.broadHumidity(x, z);
            main[i] = noise.hillsLarge.at(x, z);
            secondary[i] = noise.hillsSmall.at(x, z);
            local[i] = noise.block.at(x, z);
            dry[i] = ClimateRules.dryness(humidity[i]);
            frost[i] = ClimateRules.frostWeathering(temperature[i], humidity[i]);
            // Exercise the entire active range, independent of geographic mountain rarity.
            influence[i] = i / (double) (INPUTS - 1);
        }
        measure("single-simplex-field", i -> noise.block.at(x(i), z(i)));
        measure("broad-climate-four-fields", i -> climate.broadTemperature(x(i), z(i)) + climate.broadHumidity(x(i), z(i)));
        measure("climate-weathering", i -> ClimateRules.dryness(humidity[i]) + ClimateRules.frostWeathering(temperature[i], humidity[i]));
        measure("mountain-distribution", i -> MountainProfile.influence(main[i]));
        measure("mountain-profile", i -> MountainProfile.heightFromWeathering(influence[i], (local[i] + 1) * 0.5,
                main[i], secondary[i], local[i], secondary[i], dry[i], frost[i]));
        measure("cliff-activation", i -> CliffProfile.strengthFromWeathering(influence[i], dry[i], frost[i], main[i]));
        var output = new TerrainColumn();
        measure("cliff-profile", i -> {
            double height = CliffProfile.height(influence[i], main[i], local[i], secondary[i], output);
            return height + (output.exposedRock ? 1 : 0);
        });
        measure("humidity-only-query", i -> sampler.sampleHumidity(x(i), z(i)));
        measure("forest-patch-query", i -> sampler.isForestPatch(x(i), z(i)) ? 1 : 0);
        measure("surface-palette", i -> BiomePalette.select(columns[i]).biome().hashCode());
        measure("vertical-palette", i -> BiomePalette.selectAt(columns[i], (i & 127) - 64).biome().hashCode());
        System.out.println("Component checksum=" + TerrainBenchmark.blackhole + "; use profile for full-workload stack attribution");
    }

    private static int x(int index) { return (index % 64 - 32) * 97; }
    private static int z(int index) { return (index / 64 - 32) * 89; }

    private static void measure(String name, IntToDoubleFunction component) {
        TerrainBenchmark.measure(name, OPERATIONS, () -> {
            double sum = 0;
            for (int i = 0; i < OPERATIONS; i++) sum += component.applyAsDouble(i & (INPUTS - 1));
            TerrainBenchmark.blackhole = Double.doubleToLongBits(sum);
        });
    }
}
