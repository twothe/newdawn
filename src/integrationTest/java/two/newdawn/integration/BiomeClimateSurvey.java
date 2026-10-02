package two.newdawn.integration;

import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import two.newdawn.terrain.TerrainColumn;
import two.newdawn.terrain.TerrainSampler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Opt-in area survey of game-resolved surface biomes and their registered climate.
 * Uses Minecraft's biome zoom and snow/rain APIs, without generating millions of chunks.
 * This diagnostic is outside production and never asserts a preferred visual distribution.
 */
final class BiomeClimateSurvey {
    private static final long[] SEEDS = {0, 1, -1, 123456789, -8458999313514431577L,
            4815162342L, -7046029254386353131L, 7640891576956012809L};
    private static final int SIDE = 512;
    private static final int SPACING = 192;

    private BiomeClimateSurvey() {}

    static void run(ServerLevel level) throws Exception {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("method", "512x512 stratified jittered surface positions per seed; spacing 192 blocks; Minecraft BiomeManager zoom; registered modified climate; precipitation at physical surface or water level");
        report.put("scope", "Development runtime biome registry; excludes third-party modpack overrides, decoration and client color blending");
        report.put("temperatureBins", new String[]{"cold <0.15", "cool 0.15..<0.5", "mild 0.5..<0.9", "warm 0.9..<1.5", "hot >=1.5"});
        report.put("downfallBins", new String[]{"arid <=0.2", "dry >0.2..0.4", "moderate >0.4..0.6", "humid >0.6"});
        report.put("uncertainty", "Spatially correlated observations; seed ranges show variability, not independent-point confidence intervals");
        Map<String, Stats> totals = groups();
        Map<Long, Map<String, Stats>> bySeed = new LinkedHashMap<>();
        Map<Long, Object> accessBySeed = new LinkedHashMap<>();
        Map<String, Long> undergroundAtZero = new TreeMap<>(), undergroundAtMinus48 = new TreeMap<>();
        for (long seed : SEEDS) {
            var source = new NewDawnBiomeSource(level.registryAccess().lookupOrThrow(Registries.BIOME));
            source.initialize(seed);
            var manager = new BiomeManager((x, y, z) -> source.getNoiseBiome(x, y, z, null), BiomeManager.obfuscateSeed(seed));
            var column = new TerrainColumn();
            var position = new BlockPos.MutableBlockPos();
            var random = new Random(0x4E44535552564559L);
            Map<String, Stats> counts = groups();
            for (int z = -SIDE / 2; z < SIDE / 2; z++) for (int x = -SIDE / 2; x < SIDE / 2; x++) {
                int worldX = x * SPACING + random.nextInt(SPACING);
                int worldZ = z * SPACING + random.nextInt(SPACING);
                source.terrain().sampleInto(worldX, worldZ, column);
                position.set(worldX, Math.max(TerrainSampler.SEA_LEVEL, column.height()), worldZ);
                var holder = manager.getBiome(position);
                String biomeId = holder.unwrapKey().orElseThrow().location().toString();
                Biome biome = holder.value();
                add(counts, column, biome, biomeId, position);
                position.setY(0);
                undergroundAtZero.merge(manager.getBiome(position).unwrapKey().orElseThrow().location().toString(), 1L, Long::sum);
                position.setY(-48);
                undergroundAtMinus48.merge(manager.getBiome(position).unwrapKey().orElseThrow().location().toString(), 1L, Long::sum);
            }
            if (Boolean.getBoolean("newdawn.climateAccess")) accessBySeed.put(seed, LocalBiomeAccess.sample(source, manager));
            bySeed.put(seed, counts);
            for (String group : totals.keySet()) totals.get(group).merge(counts.get(group));
            System.out.printf(java.util.Locale.ROOT, "SURVEY seed=%d land=%d cold=%.2f%% warm/hot=%.2f%% arid=%.2f%% humid=%.2f%%%n",
                    seed, counts.get("land").count, counts.get("land").percent(counts.get("land").temperature[0]),
                    counts.get("land").percent(counts.get("land").temperature[3] + counts.get("land").temperature[4]),
                    counts.get("land").percent(counts.get("land").downfall[0]), counts.get("land").percent(counts.get("land").downfall[3]));
        }
        report.put("totals", totals);
        report.put("bySeed", bySeed);
        report.put("jointClimateAxes", "Rows: original five temperature bins; columns: original four downfall bins");
        report.put("woodedProxyBiomes", LocalBiomeAccess.WOODED.stream().sorted().toList());
        report.put("woodedProxyScope", "Tree-bearing biome IDs, not measured trees, usable wood, visibility or walkability");
        report.put("undergroundAtY0", undergroundAtZero);
        report.put("undergroundAtYMinus48", undergroundAtMinus48);
        report.put("undergroundScope", "Biome queries at fixed elevations, not cave air or cave-volume coverage");
        if (!accessBySeed.isEmpty()) report.put("localAccessBySeed", accessBySeed);
        Map<String, Map<String, Stats>> localWindows = new LinkedHashMap<>();
        var localSource = new NewDawnBiomeSource(level.registryAccess().lookupOrThrow(Registries.BIOME));
        localSource.initialize(-8458999313514431577L);
        var localManager = new BiomeManager((x, y, z) -> localSource.getNoiseBiome(x, y, z, null),
                BiomeManager.obfuscateSeed(localSource.terrain().seed()));
        for (int[] window : new int[][]{{0, 0, 256, 64}, {-703, 2887, 128, 32}, {5823, 2233, 128, 32}}) {
            var counts = groups();
            var column = new TerrainColumn();
            var position = new BlockPos.MutableBlockPos();
            var random = new Random(0x4E44535552564559L);
            for (int z = -window[2] / 2; z < window[2] / 2; z++) for (int x = -window[2] / 2; x < window[2] / 2; x++) {
                int worldX = window[0] + x * window[3] + random.nextInt(window[3]);
                int worldZ = window[1] + z * window[3] + random.nextInt(window[3]);
                localSource.terrain().sampleInto(worldX, worldZ, column);
                position.set(worldX, Math.max(TerrainSampler.SEA_LEVEL, column.height()), worldZ);
                var holder = localManager.getBiome(position);
                add(counts, column, holder.value(), holder.unwrapKey().orElseThrow().location().toString(), position);
            }
            localWindows.put("center=" + window[0] + "," + window[1] + "; width=" + window[2] * window[3], counts);
        }
        report.put("localWindowsReportedSeed", localWindows);
        Map<String, Biome.ClimateSettings> registeredClimates = new TreeMap<>();
        for (var holder : localSource.possibleBiomes()) {
            registeredClimates.put(holder.unwrapKey().orElseThrow().location().toString(), holder.value().getModifiedClimateSettings());
        }
        report.put("registeredBiomes", registeredClimates);
        long expected = (long) SIDE * SIDE * SEEDS.length;
        if (totals.get("all").count != expected || totals.get("land").count + totals.get("water").count != expected
                || totals.get("lowlands").count + totals.get("hills").count + totals.get("mountains").count != totals.get("land").count) {
            throw new AssertionError("Survey groups do not cover the sampled area");
        }
        Path output = Path.of(System.getProperty("newdawn.climateSurveyOutput")).toAbsolutePath();
        Files.createDirectories(output.getParent());
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report));
        System.out.println("PASS biome climate survey: positions=" + totals.get("all").count + ", report=" + output);
    }

    private static void add(Map<String, Stats> counts, TerrainColumn column, Biome biome, String id, BlockPos position) {
        String surface = column.height() < TerrainSampler.SEA_LEVEL ? "water" : "land";
        counts.get("all").add(column, biome, id, position);
        counts.get(surface).add(column, biome, id, position);
        if (surface.equals("land")) {
            String terrain = column.mountain() ? "mountains"
                    : column.height() + column.biomeHeightOffset() >= 82 ? "hills" : "lowlands";
            counts.get(terrain).add(column, biome, id, position);
        }
    }

    private static Map<String, Stats> groups() {
        Map<String, Stats> groups = new LinkedHashMap<>();
        for (String name : new String[]{"all", "land", "water", "lowlands", "hills", "mountains"}) groups.put(name, new Stats());
        return groups;
    }

    /** Counts area-weighted outcomes. Source climate is retained only to diagnose the translation. */
    private static final class Stats {
        private long count;
        private final long[] temperature = new long[5];
        private final long[] downfall = new long[4];
        private final long[] precipitation = new long[3];
        private final long[][] jointClimate = new long[5][4];
        private final long[] woodedByTemperature = new long[5];
        private double temperatureSum, downfallSum;
        private long sourceTemperatureNegative, sourceHumidityNegative, freezing;
        private double sourceTemperatureSum, sourceHumiditySum;
        private final Map<String, Long> biomes = new TreeMap<>();
        private final Map<String, long[]> sourceClimateOutcomes = new TreeMap<>();
        private final Map<String, long[]> sourceHumidityOutcomes = new TreeMap<>();

        void add(TerrainColumn column, Biome biome, String id, BlockPos position) {
            count++;
            var climate = biome.getModifiedClimateSettings();
            float temperatureValue = climate.temperature(), downfallValue = climate.downfall();
            int temperatureIndex = temperatureBin(temperatureValue);
            temperature[temperatureIndex]++;
            int downfallIndex = downfallValue <= 0.2f ? 0 : downfallValue <= 0.4f ? 1 : downfallValue <= 0.6f ? 2 : 3;
            downfall[downfallIndex]++;
            jointClimate[temperatureIndex][downfallIndex]++;
            if (LocalBiomeAccess.WOODED.contains(id)) woodedByTemperature[temperatureIndex]++;
            var weather = biome.getPrecipitationAt(position);
            precipitation[weather == Biome.Precipitation.NONE ? 0 : weather == Biome.Precipitation.RAIN ? 1 : 2]++;
            if (biome.coldEnoughToSnow(position)) freezing++;
            temperatureSum += temperatureValue;
            downfallSum += downfallValue;
            if (column.temperature() < 0) sourceTemperatureNegative++;
            if (column.humidity() < 0) sourceHumidityNegative++;
            sourceTemperatureSum += column.temperature();
            sourceHumiditySum += column.humidity();
            biomes.merge(id, 1L, Long::sum);
            String input = column.temperature() < -0.5f ? "T<-0.5" : column.temperature() < -0.15f ? "T[-0.5,-0.15)"
                    : column.temperature() < 0.4f ? "T[-0.15,0.4)" : column.temperature() < 0.65f ? "T[0.4,0.65)" : "T>=0.65";
            long[] outcomes = sourceClimateOutcomes.computeIfAbsent(input, ignored -> new long[6]);
            outcomes[0]++;
            outcomes[temperatureIndex + 1]++;
            String moisture = column.humidity() < -0.5f ? "H<-0.5" : column.humidity() < -0.2f ? "H[-0.5,-0.2)"
                    : column.humidity() < 0.2f ? "H[-0.2,0.2)" : column.humidity() < 0.5f ? "H[0.2,0.5)" : "H>=0.5";
            long[] moistureOutcomes = sourceHumidityOutcomes.computeIfAbsent(moisture, ignored -> new long[5]);
            moistureOutcomes[0]++;
            moistureOutcomes[downfallIndex + 1]++;
        }

        void merge(Stats other) {
            count += other.count;
            for (int i = 0; i < temperature.length; i++) temperature[i] += other.temperature[i];
            for (int i = 0; i < downfall.length; i++) downfall[i] += other.downfall[i];
            for (int i = 0; i < precipitation.length; i++) precipitation[i] += other.precipitation[i];
            for (int i = 0; i < temperature.length; i++) {
                woodedByTemperature[i] += other.woodedByTemperature[i];
                for (int j = 0; j < downfall.length; j++) jointClimate[i][j] += other.jointClimate[i][j];
            }
            temperatureSum += other.temperatureSum;
            downfallSum += other.downfallSum;
            freezing += other.freezing;
            sourceTemperatureNegative += other.sourceTemperatureNegative;
            sourceHumidityNegative += other.sourceHumidityNegative;
            sourceTemperatureSum += other.sourceTemperatureSum;
            sourceHumiditySum += other.sourceHumiditySum;
            other.biomes.forEach((id, value) -> biomes.merge(id, value, Long::sum));
            other.sourceClimateOutcomes.forEach((input, values) -> {
                long[] target = sourceClimateOutcomes.computeIfAbsent(input, ignored -> new long[6]);
                for (int i = 0; i < target.length; i++) target[i] += values[i];
            });
            other.sourceHumidityOutcomes.forEach((input, values) -> {
                long[] target = sourceHumidityOutcomes.computeIfAbsent(input, ignored -> new long[5]);
                for (int i = 0; i < target.length; i++) target[i] += values[i];
            });
        }

        double percent(long value) { return count == 0 ? 0 : value * 100.0 / count; }
    }

    /** Stable diagnostic bins shared by area and proximity measurements. */
    static int temperatureBin(float temperature) {
        return temperature < 0.15f ? 0 : temperature < 0.5f ? 1
                : temperature < 0.9f ? 2 : temperature < 1.5f ? 3 : 4;
    }
}
