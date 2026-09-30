package two.newdawn.terrain;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Explicit climate/depth boundaries plus reachability through actual production noise sampling. */
public final class BiomeRegressionTest {
    // Minecraft 1.21.1 OverworldBiomeBuilder's biome set, excluding river and frozen_river.
    private static final Set<String> EXPECTED = Set.of(
            "plains", "sunflower_plains", "snowy_plains", "ice_spikes", "desert", "swamp", "mangrove_swamp",
            "forest", "flower_forest", "birch_forest", "dark_forest", "old_growth_birch_forest", "old_growth_pine_taiga",
            "old_growth_spruce_taiga", "taiga", "snowy_taiga", "savanna", "savanna_plateau", "windswept_hills",
            "windswept_gravelly_hills", "windswept_forest", "windswept_savanna", "jungle", "sparse_jungle", "bamboo_jungle",
            "badlands", "eroded_badlands", "wooded_badlands", "meadow", "cherry_grove", "grove", "snowy_slopes",
            "frozen_peaks", "jagged_peaks", "stony_peaks", "beach", "snowy_beach", "stony_shore", "warm_ocean",
            "lukewarm_ocean", "deep_lukewarm_ocean", "ocean", "deep_ocean", "cold_ocean", "deep_cold_ocean",
            "frozen_ocean", "deep_frozen_ocean", "mushroom_fields", "dripstone_caves", "lush_caves", "deep_dark");

    public static void main(String[] arguments) {
        require(BiomePalette.biomeIds().equals(EXPECTED), "Supported biome set differs from 1.21.1 excluding rivers");
        expect("deep_frozen_ocean", 55, -0.5f, 0, 0, false, 63);
        expect("frozen_ocean", 60, -0.5f, 0, 0, false, 63);
        expect("cold_ocean", 60, -0.3f, 0, 0, false, 63);
        expect("ocean", 60, -0.2f, 0, 0, false, 63);
        expect("lukewarm_ocean", 60, 0.25f, 0, 0, false, 63);
        expect("deep_lukewarm_ocean", 59, 0.25f, 0, 0, false, 63);
        expect("warm_ocean", 40, 0.55f, 0, 0, false, 63);
        expect("snowy_beach", 63, -0.5f, 0, 0, false, 64);
        expect("mangrove_swamp", 66, 0.5f, 0.7f, 0, false, 66);
        expect("mushroom_fields", 70, 0.1f, 0.7f, -8, false, 70);
        expect("cherry_grove", 85, 0.2f, 0.3f, 0, true, 85);
        expect("grove", 85, -0.3f, 0.3f, 0, true, 85);
        expect("snowy_slopes", 95, -0.3f, 0.3f, 0, true, 95);
        expect("frozen_peaks", 110, -0.3f, -0.3f, 0, true, 110);
        expect("jagged_peaks", 110, -0.3f, 0.3f, 0, true, 110);
        expect("stony_peaks", 110, 0.3f, 0.3f, 0, true, 110);
        expect("lush_caves", 70, 0.2f, 0.7f, 0, false, 40);
        expect("dark_forest", 70, 0.2f, 0.7f, 0, false, 41);
        expect("dripstone_caves", 70, 0.2f, 0, 0, false, 0);
        expect("deep_dark", 100, 0.2f, 0.7f, 0, true, -24);
        expect("lush_caves", 100, 0.2f, 0.7f, 0, true, -23);
        expect("deep_ocean", 10, 0.2f, 0.7f, 0, false, -9);
        expect("lush_caves", 10, 0.2f, 0.7f, 0, false, -10);

        Map<String, Integer> surfaceCounts = new TreeMap<>();
        Set<String> reached = new HashSet<>();
        TerrainColumn column = new TerrainColumn();
        for (long seed : new long[]{0, 1, 123456789, -8458999313514431577L}) {
            TerrainSampler sampler = new TerrainSampler(seed);
            for (int z = -256; z < 256; z++) for (int x = -256; x < 256; x++) {
                sampler.sampleInto(x * 96 + 17, z * 96 - 31, column);
                String surface = BiomePalette.select(column).biome();
                surfaceCounts.merge(surface, 1, Integer::sum);
                reached.add(surface);
                reached.add(BiomePalette.selectAt(column, 0).biome());
                reached.add(BiomePalette.selectAt(column, -48).biome());
                require(BiomePalette.selectAt(column, column.height()).biome().equals(surface), "Cave biome reached the surface");
            }
        }
        Set<String> missing = new java.util.TreeSet<>(EXPECTED);
        missing.removeAll(reached);
        require(missing.isEmpty(), "Biomes unreachable in production noise survey: " + missing);
        System.out.println("PASS: all 51 non-river biomes reached across 1048576 production columns; climate/depth boundaries; surfaceCounts=" + surfaceCounts);
    }

    private static void expect(String biome, int height, float temperature, float humidity, int region, boolean mountain, int y) {
        TerrainColumn column = new TerrainColumn();
        column.height = height; column.temperature = temperature; column.humidity = humidity;
        column.regionHeight = region; column.mountain = mountain; column.fillerDepth = 3;
        require(BiomePalette.selectAt(column, y).biome().equals(biome), "Expected " + biome + " for height=" + height + ", Y=" + y);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
