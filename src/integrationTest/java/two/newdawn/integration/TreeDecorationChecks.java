package two.newdawn.integration;

import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import two.newdawn.terrain.BiomePalette;
import two.newdawn.terrain.TerrainColumn;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Real-registry decoration audit and placement contracts; never included in the released mod. */
final class TreeDecorationChecks {
    private TreeDecorationChecks() {}

    static void verify(ServerLevel level, NewDawnChunkGenerator generator) throws Exception {
        var source = (NewDawnBiomeSource) generator.getBiomeSource();
        if (Boolean.getBoolean("newdawn.decorationBaseline")) {
            require(source.possibleBiomes().stream().flatMap(biome -> biome.value().getGenerationSettings().features().stream())
                    .flatMap(features -> features.stream()).noneMatch(feature ->
                            feature.value().placement().contains(TreePatchPlacement.INSTANCE)),
                    "Baseline requires the tree_patches biome modifier to be disabled by its test datapack");
            verifyOpenChunk(level, source, "savanna", false);

            return;
        }
        var shared = new IdentityHashMap<PlacedFeature, PlacedFeature>();
        var report = new LinkedHashMap<String, Object>();
        var vanilla = level.registryAccess().registryOrThrow(Registries.WORLD_PRESET)
                .get(ResourceLocation.fromNamespaceAndPath("newdawn", "vanilla"))
                .createWorldDimensions().overworld();
        int trees = 0, preserved = 0;
        for (var biome : source.possibleBiomes()) {
            require(level.registryAccess().registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(biome.unwrapKey().orElseThrow()) == biome, "Biome registry identity changed");
            require(biome.value().getModifiedClimateSettings().equals(
                    biome.value().modifiableBiomeInfo().getOriginalBiomeInfo().climateSettings()),
                    "Tree modifier changed biome climate");
            var original = biome.value().modifiableBiomeInfo().getOriginalBiomeInfo().generationSettings().features();
            var modified = biome.value().getGenerationSettings().features();
            var entries = new ArrayList<Object>();
            for (int stage = 0; stage < original.size(); stage++) {
                var before = original.get(stage).stream().toList();
                var after = modified.get(stage).stream().toList();
                require(before.size() == after.size(), "Modifier changed feature count");
                for (int index = 0; index < before.size(); index++) {
                    var first = before.get(index);
                    var last = after.get(index);
                    boolean tree = first.value().getFeatures().anyMatch(feature -> feature.feature() instanceof TreeFeature);
                    if (!tree) {
                        require(first == last, "Non-tree feature was replaced");
                        preserved++;
                        continue;
                    }
                    trees++;
                    var wrapper = last.value();
                    require(first.value().feature() == wrapper.feature(), "Configured tree feature changed");
                    require(wrapper.placement().size() == first.value().placement().size() + 1,
                            "Tree feature needs exactly one additional filter");
                    require(wrapper.placement().subList(0, first.value().placement().size())
                            .equals(first.value().placement()), "Original tree placements changed order");
                    require(wrapper.placement().getLast() == TreePatchPlacement.INSTANCE, "Missing final tree filter");
                    var previous = shared.putIfAbsent(first.value(), wrapper);
                    require(previous == null || previous == wrapper, "Shared tree feature lost its shared wrapper");
                    require(biome.value().getGenerationSettings().hasFeature(wrapper), "BiomeFilter cannot see wrapper");
                    entries.add(Map.of("feature", first.unwrapKey().orElseThrow().location().toString(),
                            "placement", first.value().placement().stream().map(value -> value.getClass().getSimpleName()).toList()));
                }
            }
            verifyFilter(level, generator, vanilla, biome);
            report.put(biome.unwrapKey().orElseThrow().location().toString(), Map.of(
                    "open", biome.is(TreePatchPlacement.OPEN_VEGETATION), "savannaBands", biome.is(TreePatchPlacement.SAVANNA_VEGETATION), "tree_features", entries));
        }
        Files.writeString(level.getServer().getServerDirectory().resolve("tree-decoration-audit.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(report));
        require(trees > 0 && preserved > 0, "Decoration audit did not cover trees and non-tree features");
        verifyOpenChunk(level, source, "savanna", true);
        verifyWetSavanna(level, source);
        LogUtils.getLogger().info("PASS tree decoration: {} biomes, {} tree references, {} untouched non-tree references",
                report.size(), trees, preserved);
    }

    /** Only the world's biome lookup is isolated; the registered production filter performs all decisions. */
    private static void verifyFilter(ServerLevel level, ChunkGenerator generator, ChunkGenerator vanilla, Holder<Biome> biome) {
        var world = (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("getBiome")) return biome;
                    return method.invoke(level, arguments);
                });
        var context = new PlacementContext(world, generator, Optional.empty());
        var vanillaContext = new PlacementContext(world, vanilla, Optional.empty());
        var terrain = ((NewDawnBiomeSource) generator.getBiomeSource()).terrain();
        var random = RandomSource.create(123);
        var untouched = RandomSource.create(123);
        for (int index = 0; index < 256; index++) {
            var position = new BlockPos((index % 16 - 8) * 97, 100, (index / 16 - 8) * 89);
            boolean accepted = TreePatchPlacement.INSTANCE.getPositions(context, random, position).findAny().isPresent();
            boolean expected = biome.is(TreePatchPlacement.SAVANNA_VEGETATION)
                    ? BiomePalette.allowsSavannaTrees(terrain.sampleHumidity(position.getX(), position.getZ()))
                    : !biome.is(TreePatchPlacement.OPEN_VEGETATION) || terrain.isForestPatch(position.getX(), position.getZ());
            require(accepted == expected, "Incorrect vegetation placement");
            require(TreePatchPlacement.INSTANCE.getPositions(vanillaContext, random, position).findAny().isPresent(),
                    "Vanilla generator was filtered");
        }
        require(random.nextLong() == untouched.nextLong(), "Tree filter consumed decoration RNG");
    }

    /** Find a broad homogeneous open patch in the CURRENT seed, then inspect the actual fully decorated chunk. */
    private static void verifyOpenChunk(ServerLevel level, NewDawnBiomeSource source, String target, boolean filtered) {
        var terrain = source.terrain();
        var column = new TerrainColumn();
        for (int z = -8192; z <= 8192; z += 32) for (int x = -8192; x <= 8192; x += 32) {
            terrain.sampleInto(x, z, column);
            if (!BiomePalette.select(column).biome().equals(target) || BiomePalette.allowsSavannaTrees(column.humidity())) continue;
            boolean clear = true;
            for (int dz = -24; dz <= 40 && clear; dz += 4) for (int dx = -24; dx <= 40; dx += 4) {
                terrain.sampleInto(x + dx, z + dz, column);
                if (!BiomePalette.select(column).biome().equals(target) || BiomePalette.allowsSavannaTrees(column.humidity())) {
                    clear = false;
                    break;
                }
            }
            if (!clear) continue;
            var chunk = level.getChunk(x >> 4, z >> 4);
            if (!chunk.getAllStarts().isEmpty() || !chunk.getAllReferences().isEmpty()) continue;
            int logs = 0;
            var position = new BlockPos.MutableBlockPos();
            for (int dz = 0; dz < 16; dz++) for (int dx = 0; dx < 16; dx++) {
                for (int y = 64; y < 255; y++) {
                    position.set(x + dx, y, z + dz);
                    if (chunk.getBlockState(position).is(BlockTags.LOGS)) logs++;
                }
            }
            require(!filtered || logs == 0, "Scattered trees remained in open " + target + " at " + x + "/" + z);
            LogUtils.getLogger().info("PASS open {} chunk at {}/{}: {} log blocks, filtered={}", target, x, z, logs, filtered);
            return;
        }
        throw new AssertionError("No homogeneous open " + target + " found in bounded survey");
    }

    /** Real moderately humid Savanna must retain its native tree feature, including outside forest noise patches. */
    private static void verifyWetSavanna(ServerLevel level, NewDawnBiomeSource source) {
        var terrain = source.terrain();
        var column = new TerrainColumn();
        for (int z = -8192; z <= 8192; z += 32) for (int x = -8192; x <= 8192; x += 32) {
            terrain.sampleInto(x, z, column);
            if (!BiomePalette.select(column).biome().equals("savanna")
                    || !BiomePalette.allowsSavannaTrees(column.humidity()) || terrain.isForestPatch(x, z)) continue;
            var chunk = level.getChunk(x >> 4, z >> 4);
            if (!chunk.getAllStarts().isEmpty() || !chunk.getAllReferences().isEmpty()) continue;
            var position = new BlockPos.MutableBlockPos();
            for (int dz = 0; dz < 16; dz++) for (int dx = 0; dx < 16; dx++) for (int y = 64; y < 255; y++) {
                position.set(x + dx, y, z + dz);
                if (chunk.getBlockState(position).is(BlockTags.LOGS)
                        && level.getBiome(position).is(SAVANNA_KEY)
                        && BiomePalette.allowsSavannaTrees(terrain.sampleHumidity(x + dx, z + dz))
                        && !terrain.isForestPatch(x + dx, z + dz)) {
                    LogUtils.getLogger().info("PASS moderately humid Savanna tree outside forest overlay at {}", position);
                    return;
                }
            }
        }
        throw new AssertionError("No Vanilla tree found in moderately humid Savanna");
    }

    private static final net.minecraft.resources.ResourceKey<Biome> SAVANNA_KEY = net.minecraft.resources.ResourceKey.create(
            Registries.BIOME, ResourceLocation.withDefaultNamespace("savanna"));

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
