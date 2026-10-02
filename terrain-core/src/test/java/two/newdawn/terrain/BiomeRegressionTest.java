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
        expect("snowy_beach", 63, -0.55f, 0, 0, false, 64);
        expect("mangrove_swamp", 66, 0.5f, 0.7f, 0, false, 66);
        expect("mushroom_fields", 70, 0.1f, 0.7f, -8, false, 70);
        expect("cherry_grove", 85, 0.2f, 0.3f, 0, true, 85);
        // Rare cherry window: inclusive lower bounds, exclusive upper bounds, climate-compatible neighbors.
        expect("cherry_grove", 85, 0.15f, 0.25f, 0, true, 85);
        expect("cherry_grove", 85, Math.nextDown(0.25f), Math.nextDown(0.40f), 0, true, 85);
        expect("birch_forest", 85, Math.nextDown(0.15f), 0.3f, 0, true, 85);
        expect("savanna", 85, 0.25f, 0.3f, 0, true, 85);
        expect("birch_forest", 85, 0.2f, Math.nextDown(0.25f), 0, true, 85);
        expect("old_growth_birch_forest", 85, 0.2f, 0.40f, 0, true, 85);
        expect("cherry_grove", 76, 0.2f, 0.3f, 0, true, 76);
        expect("cherry_grove", 82, 0.2f, 0.3f, 0, false, 82);
        expect("birch_forest", 75, 0.2f, 0.3f, 0, true, 75);
        expect("birch_forest", 81, 0.2f, 0.3f, 0, false, 81);
        expect("cherry_grove", 103, 0.2f, 0.3f, 0, true, 103);
        expect("cherry_grove", 104, 0.2f, 0.3f, 0, true, 104);
        expect("plains", 106, -0.13f, -0.38f, 7, false, 106);
        expect("birch_forest", 150, 0.1f, 0.3f, 0, false, 150);
        expect("meadow", 100, 0.0f, 0.0f, 0, false, 100);
        expect("forest", 100, 0.0f, 0.5f, 0, false, 100);
        expect("birch_forest", 75, -0.2f, 0.0f, 0, false, 75);
        expect("taiga", 75, -0.3f, 0.0f, 0, false, 75);
        expect("taiga", 75, Math.nextDown(-0.22f), 0.0f, 0, false, 75);
        expect("birch_forest", 75, -0.22f, 0.0f, 0, false, 75);
        expect("birch_forest", 75, Math.nextDown(0.22f), 0.3f, 0, false, 75);
        expect("savanna", 75, 0.22f, 0.3f, 0, false, 75);
        expect("savanna", 75, 0.3f, -0.2f, 0, false, 75);
        expect("savanna", 75, 0.3f, -0.1f, 0, false, 75);
        require(!BiomePalette.allowsSavannaTrees(Math.nextDown(-0.10f)), "Dry Savanna must be treeless");
        require(BiomePalette.allowsSavannaTrees(-0.10f), "Moderately humid Savanna lost Vanilla trees");
        expect("savanna", 75, 0.3f, Math.nextDown(0.40f), 0, false, 75);
        expect("jungle", 75, 0.3f, 0.40f, 0, false, 75);
        expect("jungle", 75, 0.3f, 0.5f, 0, false, 75);
        expect("jungle", 100, 0.7f, 0.5f, 0, false, 100);
        expect("desert", 75, 0.6f, -0.3f, 0, false, 75);
        expect("savanna", 75, Math.nextDown(0.6f), -0.3f, 0, false, 75);
        expect("savanna_plateau", 100, 0.3f, -0.2f, 0, false, 100);
        expect("windswept_savanna", 100, 0.3f, -0.2f, 0, true, 100);
        expect("badlands", 100, 0.7f, -0.3f, 0, false, 100);
        expect("wooded_badlands", 100, 0.7f, -0.2f, 0, false, 100);
        expect("plains", 75, 0.0f, -0.11f, 0, false, 75);
        expect("flower_forest", 75, 0.0f, -0.10f, 0, false, 75);
        expect("birch_forest", 75, 0.0f, -0.05f, 0, false, 75);
        expect("snowy_plains", 75, -0.55f, 0.0f, 0, false, 75);
        expect("taiga", 75, Math.nextUp(-0.55f), 0.0f, 0, false, 75);
        expect("taiga", 106, -0.3f, 0.0f, 0, false, 106);
        expect("snowy_slopes", 127, -0.3f, 0.3f, 0, true, 127);
        expect("grove", 85, -0.3f, 0.3f, 0, true, 85);
        expect("snowy_slopes", 95, -0.3f, 0.3f, 0, true, 95);
        expect("frozen_peaks", 128, -0.3f, -0.3f, 0, true, 128);
        expect("jagged_peaks", 128, -0.3f, 0.3f, 0, true, 128);
        expect("stony_peaks", 128, 0.3f, 0.3f, 0, true, 128);
        expectOffset("snowy_slopes", 127, -6, -0.3f, 0.3f, true);
        expectOffset("frozen_peaks", 127, 6, -0.3f, -0.3f, true);
        expectOffset("grove", 91, -6, -0.3f, 0.3f, true);
        expectOffset("snowy_slopes", 91, 6, -0.3f, 0.3f, true);
        expectOffset("birch_forest", 81, -6, 0.2f, 0.3f, false);
        expectOffset("cherry_grove", 81, 6, 0.2f, 0.3f, false);
        // Coastlines and water must continue to use the physical terrain height.
        expectOffset("beach", 64, 6, 0.2f, 0.3f, false);
        expectOffset("ocean", 62, 6, 0.2f, 0.3f, false);
        expect("lush_caves", 70, 0.2f, 0.7f, 0, false, 40);
        expect("dark_forest", 70, 0.2f, 0.7f, 0, false, 41);
        expect("dripstone_caves", 70, 0.2f, 0, 0, false, 0);
        expect("deep_dark", 100, 0.2f, 0.7f, 0, true, -24);
        expect("lush_caves", 100, 0.2f, 0.7f, 0, true, -23);
        expect("deep_ocean", 10, 0.2f, 0.7f, 0, false, -9);
        expect("lush_caves", 10, 0.2f, 0.7f, 0, false, -10);

        boolean extended = arguments.length == 1 && arguments[0].equals("--extended");
        if (arguments.length > 0 && !extended) throw new IllegalArgumentException("Expected --extended or no arguments");
        int radius = extended ? 256 : 24;
        Map<String, Integer> surfaceCounts = new TreeMap<>();
        Set<String> reached = new HashSet<>();
        TerrainColumn column = new TerrainColumn();
        float minimumOffset = 0, maximumOffset = 0;
        for (long seed : new long[]{0, 1, 123456789, -8458999313514431577L}) {
            TerrainSampler sampler = new TerrainSampler(seed);
            for (int z = -radius; z < radius; z++) for (int x = -radius; x < radius; x++) {
                sampler.sampleInto(x * 96 + 17, z * 96 - 31, column);
                require(column.biomeHeightOffset() >= -6 && column.biomeHeightOffset() <= 6,
                        "Biome height offset exceeded its six-block bound");
                minimumOffset = Math.min(minimumOffset, column.biomeHeightOffset());
                maximumOffset = Math.max(maximumOffset, column.biomeHeightOffset());
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
        missing.remove("sparse_jungle"); // Retained as possible for saved chunks, intentionally not selected on new surfaces.
        require(!extended || missing.isEmpty(), "Biomes unreachable in production noise survey: " + missing);
        require(minimumOffset < -2 && maximumOffset > 2, "Biome height noise lacks useful variation");
        if (extended) System.out.println("PASS: all 50 selected non-river biomes reached across 1048576 production columns; climate/depth boundaries; surfaceCounts=" + surfaceCounts);
        else System.out.println("PASS: climate/depth boundaries and 9216 production columns; use extendedCheck for all-biome reachability");
    }

    private static void expect(String biome, int height, float temperature, float humidity, int region, boolean mountain, int y) {
        TerrainColumn column = new TerrainColumn();
        column.height = height; column.temperature = temperature; column.humidity = humidity;
        column.regionHeight = region; column.mountain = mountain; column.fillerDepth = 3;
        require(BiomePalette.selectAt(column, y).biome().equals(biome), "Expected " + biome + " for height=" + height + ", Y=" + y);
    }

    /** Checks height-band movement while preserving the selected column's physical elevation. */
    private static void expectOffset(String biome, int height, float offset, float temperature, float humidity, boolean mountain) {
        TerrainColumn column = new TerrainColumn();
        column.height = height; column.biomeHeightOffset = offset;
        column.temperature = temperature; column.humidity = humidity;
        column.mountain = mountain; column.fillerDepth = 3;
        require(BiomePalette.select(column).biome().equals(biome),
                "Expected " + biome + " for height=" + height + ", offset=" + offset);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
