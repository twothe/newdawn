package two.newdawn.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.BiomeManager;
import two.newdawn.terrain.TerrainColumn;
import two.newdawn.terrain.TerrainSampler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Opt-in bounded proximity estimates using production terrain and game biome queries; never loaded in a release. */
final class LocalBiomeAccess {
    static final Set<String> WOODED = Set.of(
            "minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:dark_forest",
            "minecraft:old_growth_birch_forest", "minecraft:taiga", "minecraft:snowy_taiga",
            "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:grove",
            "minecraft:cherry_grove", "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle",
            "minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:savanna", "minecraft:savanna_plateau",
            "minecraft:windswept_savanna", "minecraft:windswept_forest", "minecraft:wooded_badlands");
    private static final int ORIGINS = 16, RADIUS = 1024, SPACING = 16;
    private static final int[] WOOD_RADII = {160, 320, 640};

    private LocalBiomeAccess() {}

    /** Origin acceptance depends only on physical land, never the selected biome or wood availability. */
    static Map<String, Object> sample(NewDawnBiomeSource source, BiomeManager manager) {
        var result = new LinkedHashMap<String, Object>();
        result.put("method", "16 physical-land origins; 16-block grid in 1024-block circle; sampled straight-line distances");
        result.put("families", new String[]{"cold/cool", "mild", "warm/hot"});
        result.put("woodRadii", WOOD_RADII);
        result.put("climateRadii", new int[]{512, 1024});
        result.put("originSelection", "Fixed RNG candidates over +/-49152 blocks; reject only water; no biome-dependent filtering");
        var random = new Random(0x4E44414343455353L);
        var column = new TerrainColumn();
        var position = new BlockPos.MutableBlockPos();
        var origins = new ArrayList<Map<String, Object>>();
        int[] woodWithin = new int[3];
        int[][] familyWithin = new int[3][2];
        int attempts = 0;
        while (origins.size() < ORIGINS) {
            if (++attempts > 10_000) throw new IllegalStateException("Unable to select land origins for seed=" + source.terrain().seed());
            int originX = random.nextInt(98_304) - 49_152, originZ = random.nextInt(98_304) - 49_152;
            if (source.terrain().sampleHeight(originX, originZ) < TerrainSampler.SEA_LEVEL) continue;
            int woodDistanceSquared = Integer.MAX_VALUE;
            int[] familyDistanceSquared = new int[3];
            Arrays.fill(familyDistanceSquared, Integer.MAX_VALUE);
            for (int dz = -RADIUS; dz <= RADIUS; dz += SPACING) for (int dx = -RADIUS; dx <= RADIUS; dx += SPACING) {
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared > RADIUS * RADIUS) continue;
                int x = originX + dx, z = originZ + dz;
                source.terrain().sampleInto(x, z, column);
                if (column.height() < TerrainSampler.SEA_LEVEL) continue;
                position.set(x, column.height(), z);
                var holder = manager.getBiome(position);
                int temperature = BiomeClimateSurvey.temperatureBin(holder.value().getModifiedClimateSettings().temperature());
                int family = temperature < 2 ? 0 : temperature == 2 ? 1 : 2;
                familyDistanceSquared[family] = Math.min(familyDistanceSquared[family], distanceSquared);
                if (WOODED.contains(holder.unwrapKey().orElseThrow().location().toString())) {
                    woodDistanceSquared = Math.min(woodDistanceSquared, distanceSquared);
                }
            }
            for (int i = 0; i < woodWithin.length; i++) {
                int radius = WOOD_RADII[i];
                if (woodDistanceSquared <= radius * radius) woodWithin[i]++;
            }
            for (int family = 0; family < familyWithin.length; family++) {
                if (familyDistanceSquared[family] <= 512 * 512) familyWithin[family][0]++;
                if (familyDistanceSquared[family] <= RADIUS * RADIUS) familyWithin[family][1]++;
            }
            var origin = new LinkedHashMap<String, Object>();
            origin.put("x", originX); origin.put("z", originZ);
            // Negative is an explicit unresolved search rather than an omitted observation.
            origin.put("sampledWoodDistance", distance(woodDistanceSquared));
            origin.put("sampledClimateDistances", Arrays.stream(familyDistanceSquared).map(LocalBiomeAccess::distance).toArray());
            origins.add(origin);
        }
        result.put("origins", origins);
        result.put("woodWithinCounts", woodWithin);
        result.put("climateWithinCounts", familyWithin);
        return result;
    }

    private static int distance(int squared) { return squared == Integer.MAX_VALUE ? -1 : (int) Math.ceil(Math.sqrt(squared)); }
}
