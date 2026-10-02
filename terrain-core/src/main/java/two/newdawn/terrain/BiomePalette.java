package two.newdawn.terrain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Climate/elevation rules for all 1.21.1 overworld biomes except rivers; no RNG or extra noise. */
public final class BiomePalette {
    public enum Material { GRASS, DIRT, STONE, GRAVEL, SAND, TERRACOTTA, RED_SAND, MYCELIUM, PODZOL, MUD, SNOW_BLOCK, PACKED_ICE }
    public record Entry(String biome, Material top, Material filler) {}
    public static final int DEEP_DARK_CEILING = -24;
    // Elevations use the sampled local offset; coastal and underwater rules use physical height.
    private static final int PEAK_HEIGHT = 128;
    private static final int UPLAND_HEIGHT = 82;
    private static final int MOUNTAIN_UPLAND_HEIGHT = 76;
    private static final int SNOWY_SLOPE_HEIGHT = 92;
    private static final int DEEP_DARK_MINIMUM_SURFACE = 92;
    private static final int CAVE_ROOF_THICKNESS = 20;
    private static final int MAXIMUM_CAVE_CEILING = 40;
    // Selection climate is dimensionless. Tune against actual game biome values, not noise amplitude alone.
    private static final float COLD_TEMPERATURE = -0.55f;
    private static final float COOL_TEMPERATURE = -0.22f;
    private static final float WARM_TEMPERATURE = 0.22f;
    private static final float HOT_TEMPERATURE = 0.60f;
    private static final float WARM_WOODLAND_HUMIDITY = -0.10f;
    private static final float JUNGLE_HUMIDITY = 0.40f;

    private enum Choice {
        PLAINS, SUNFLOWER_PLAINS, SNOWY_PLAINS,
        ICE_SPIKES(Material.SNOW_BLOCK, Material.DIRT),
        DESERT(Material.SAND, Material.SAND), SWAMP, MANGROVE_SWAMP(Material.MUD, Material.DIRT),
        FOREST, FLOWER_FOREST, BIRCH_FOREST, DARK_FOREST, OLD_GROWTH_BIRCH_FOREST,
        OLD_GROWTH_PINE_TAIGA(Material.PODZOL, Material.DIRT), OLD_GROWTH_SPRUCE_TAIGA(Material.PODZOL, Material.DIRT),
        TAIGA, SNOWY_TAIGA, SAVANNA, SAVANNA_PLATEAU,
        WINDSWEPT_HILLS(Material.STONE, Material.STONE), WINDSWEPT_GRAVELLY_HILLS(Material.GRAVEL, Material.STONE),
        WINDSWEPT_FOREST, WINDSWEPT_SAVANNA,
        // Sparse Jungle remains possible for saved chunks and stable feature encounter indices.
        JUNGLE, SPARSE_JUNGLE, BAMBOO_JUNGLE,
        BADLANDS(Material.RED_SAND, Material.TERRACOTTA), ERODED_BADLANDS(Material.RED_SAND, Material.TERRACOTTA),
        WOODED_BADLANDS(Material.GRASS, Material.TERRACOTTA),
        MEADOW, CHERRY_GROVE, GROVE,
        SNOWY_SLOPES(Material.SNOW_BLOCK, Material.STONE), FROZEN_PEAKS(Material.PACKED_ICE, Material.STONE),
        JAGGED_PEAKS(Material.STONE, Material.STONE), STONY_PEAKS(Material.STONE, Material.STONE),
        BEACH(Material.SAND, Material.SAND), SNOWY_BEACH(Material.SAND, Material.SAND), STONY_SHORE(Material.STONE, Material.STONE),
        WARM_OCEAN(Material.SAND, Material.SAND), LUKEWARM_OCEAN(Material.SAND, Material.SAND),
        DEEP_LUKEWARM_OCEAN(Material.SAND, Material.SAND), OCEAN(Material.GRAVEL, Material.STONE),
        DEEP_OCEAN(Material.GRAVEL, Material.STONE), COLD_OCEAN(Material.GRAVEL, Material.STONE),
        DEEP_COLD_OCEAN(Material.GRAVEL, Material.STONE), FROZEN_OCEAN(Material.GRAVEL, Material.STONE),
        DEEP_FROZEN_OCEAN(Material.GRAVEL, Material.STONE), MUSHROOM_FIELDS(Material.MYCELIUM, Material.DIRT),
        DRIPSTONE_CAVES(Material.STONE, Material.STONE), LUSH_CAVES(Material.STONE, Material.STONE),
        DEEP_DARK(Material.STONE, Material.STONE);

        final Entry entry;
        Choice() { this(Material.GRASS, Material.DIRT); }
        Choice(Material top, Material filler) { entry = new Entry(name().toLowerCase(Locale.ROOT), top, filler); }
    }

    private static final Set<String> BIOMES = Arrays.stream(Choice.values()).map(choice -> choice.entry.biome()).collect(Collectors.toUnmodifiableSet());
    private BiomePalette() {}

    /** Surface choice, used for both biome columns and matching raw surface materials. */
    public static Entry select(TerrainSample sample) {
        return surface(sample.height(), sample.biomeHeightOffset(), sample.regionHeight(), sample.mountain(),
                sample.temperature(), sample.humidity()).entry;
    }

    public static Entry select(TerrainColumn column) {
        return surface(column.height(), column.biomeHeightOffset(), column.regionHeight(), column.mountain(),
                column.temperature(), column.humidity()).entry;
    }

    /** Complete three-dimensional choice. The surface remains intact above the cave ceiling. */
    public static Entry selectAt(TerrainColumn column, int blockY) {
        Entry underground = selectVertical(blockY, null, caveBiome(column), caveCeiling(column),
                hasDeepDark(column), deepDark());
        return underground != null ? underground : select(column);
    }

    /**
     * Applies depth precedence to preselected candidates, including cached game registry holders.
     * A null cave retains the surface candidate. Surface may be null to defer its computation.
     * The Deep Dark candidate must be present when hasDeepDark is true.
     * This is the shared vertical rule for direct and cached biome queries, without allocation.
     */
    public static <T> T selectVertical(int blockY, T surface, T cave, int caveCeiling,
                                       boolean hasDeepDark, T deepDarkBiome) {
        if (blockY <= DEEP_DARK_CEILING && hasDeepDark) return deepDarkBiome;
        return blockY <= caveCeiling && cave != null ? cave : surface;
    }

    /** Cached candidate for normal underground elevations; null retains the surface biome. */
    public static Entry caveBiome(TerrainColumn column) {
        if (column.humidity() >= 0.35f && column.temperature() > -0.45f) return Choice.LUSH_CAVES.entry;
        if (column.humidity() >= -0.35f && column.humidity() < 0.35f && column.temperature() > -0.6f) return Choice.DRIPSTONE_CAVES.entry;
        return null;
    }

    /** Leave at least 20 blocks of terrain above cave biomes, including low ocean floors. */
    public static int caveCeiling(TerrainColumn column) { return Math.min(MAXIMUM_CAVE_CEILING, column.height() - CAVE_ROOF_THICKNESS); }
    /** Deep Dark is restricted to deep rock beneath elevated terrain. */
    public static boolean hasDeepDark(TerrainColumn column) {
        return column.mountain() && column.height() + column.biomeHeightOffset() >= DEEP_DARK_MINIMUM_SURFACE;
    }
    public static Entry deepDark() { return Choice.DEEP_DARK.entry; }
    public static Set<String> biomeIds() { return BIOMES; }

    /** Dry Savanna Plains and wooded Vanilla Savanna share a registry identity and differ only in placement. */
    public static boolean allowsSavannaTrees(float humidity) { return humidity >= WARM_WOODLAND_HUMIDITY; }

    private static Choice surface(int height, float biomeHeightOffset, int regionHeight,
                                  boolean mountain, float temperature, float humidity) {
        float uplandHeight = height + biomeHeightOffset;
        if (height < TerrainSampler.SEA_LEVEL - 1) return ocean(height, temperature);
        // Rare humid, temperate islands in low regional basins, selected without per-column randomness.
        if (height >= 64 && height <= 76 && regionHeight <= -7 && temperature >= -0.15f && temperature < 0.25f && humidity >= 0.65f) {
            return Choice.MUSHROOM_FIELDS;
        }
        if (height >= 64 && height <= 68 && humidity >= 0.55f && temperature > -0.15f) {
            return temperature >= WARM_TEMPERATURE ? Choice.MANGROVE_SWAMP : Choice.SWAMP;
        }
        if (height <= TerrainSampler.SEA_LEVEL) return temperature <= COLD_TEMPERATURE ? Choice.SNOWY_BEACH : Choice.BEACH;
        if (height <= 68 && mountain) return Choice.STONY_SHORE;
        if (mountain && uplandHeight >= PEAK_HEIGHT) {
            if (temperature >= 0.1f) return Choice.STONY_PEAKS;
            return humidity < 0.0f ? Choice.FROZEN_PEAKS : Choice.JAGGED_PEAKS;
        }
        if (uplandHeight >= UPLAND_HEIGHT || (mountain && uplandHeight >= MOUNTAIN_UPLAND_HEIGHT))
            return highland(uplandHeight, mountain, temperature, humidity);
        return lowland(temperature, humidity);
    }

    private static Choice ocean(int height, float temperature) {
        boolean deep = height < 60;
        // Oceans retain their independently tuned thermal progression.
        if (temperature <= -0.5f) return deep ? Choice.DEEP_FROZEN_OCEAN : Choice.FROZEN_OCEAN;
        if (temperature < -0.2f) return deep ? Choice.DEEP_COLD_OCEAN : Choice.COLD_OCEAN;
        if (temperature < 0.25f) return deep ? Choice.DEEP_OCEAN : Choice.OCEAN;
        if (temperature < 0.55f) return deep ? Choice.DEEP_LUKEWARM_OCEAN : Choice.LUKEWARM_OCEAN;
        return Choice.WARM_OCEAN;
    }

    private static Choice highland(float uplandHeight, boolean mountain, float temperature, float humidity) {
        if (temperature <= -0.25f) {
            if (!mountain) return lowland(temperature, humidity);
            if (uplandHeight >= SNOWY_SLOPE_HEIGHT || humidity < 0.1f) return Choice.SNOWY_SLOPES;
            return Choice.GROVE;
        }
        // The narrow destination takes precedence over the adjoining warm transition.
        if (temperature >= 0.15f && temperature < 0.25f && humidity >= 0.25f && humidity < 0.40f) return Choice.CHERRY_GROVE;
        if (temperature >= HOT_TEMPERATURE) {
            if (humidity < -0.25f) return humidity < -0.6f ? Choice.ERODED_BADLANDS : Choice.BADLANDS;
            if (humidity < WARM_WOODLAND_HUMIDITY) return Choice.WOODED_BADLANDS;
        }
        if (temperature >= WARM_TEMPERATURE) {
            if (humidity < WARM_WOODLAND_HUMIDITY) return mountain ? Choice.WINDSWEPT_SAVANNA : Choice.SAVANNA_PLATEAU;
            return lowland(temperature, humidity);
        }
        // Dry uplands expose rock; ordinary humid hills retain climate-compatible forests.
        if (humidity < -0.85f) return Choice.WINDSWEPT_GRAVELLY_HILLS;
        if (humidity < -0.45f) return Choice.WINDSWEPT_HILLS;
        if (mountain && humidity > 0.65f) return Choice.WINDSWEPT_FOREST;
        if (temperature >= -0.10f && temperature < 0.15f && humidity >= -0.15f && humidity < 0.20f) return Choice.MEADOW;
        return lowland(temperature, humidity);
    }

    private static Choice lowland(float temperature, float humidity) {
        if (temperature <= COLD_TEMPERATURE) {
            if (humidity < -0.55f) return Choice.ICE_SPIKES;
            return humidity < 0.1f ? Choice.SNOWY_PLAINS : Choice.SNOWY_TAIGA;
        }
        if (temperature < COOL_TEMPERATURE) {
            if (humidity < -0.4f) return Choice.PLAINS;
            if (humidity < 0.2f) return Choice.TAIGA;
            return humidity < 0.5f ? Choice.OLD_GROWTH_PINE_TAIGA : Choice.OLD_GROWTH_SPRUCE_TAIGA;
        }
        if (temperature < WARM_TEMPERATURE) {
            if (humidity < -0.45f) return Choice.SUNFLOWER_PLAINS;
            if (humidity < -0.10f) return Choice.PLAINS;
            if (humidity < -0.05f) return Choice.FLOWER_FOREST;
            if (humidity < 0.35f) return Choice.BIRCH_FOREST;
            if (humidity < 0.50f) return Choice.OLD_GROWTH_BIRCH_FOREST;
            return humidity < 0.70f ? Choice.FOREST : Choice.DARK_FOREST;
        }
        if (temperature >= HOT_TEMPERATURE && humidity < -0.2f) return Choice.DESERT;
        if (humidity < JUNGLE_HUMIDITY) return Choice.SAVANNA;
        return humidity < 0.65f ? Choice.JUNGLE : Choice.BAMBOO_JUNGLE;
    }
}
