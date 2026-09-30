package two.newdawn.integration;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import two.newdawn.terrain.BiomePalette;
import two.newdawn.terrain.TerrainSampler;
import two.newdawn.terrain.TerrainColumn;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Resolves climate/elevation decisions to registry-owned vanilla biome holders, including mod modifiers.
 * The generator binds the world seed once during createState, before Minecraft schedules chunk work.
 */
public final class NewDawnBiomeSource extends BiomeSource {
    public static final MapCodec<NewDawnBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(RegistryOps.retrieveGetter(Registries.BIOME)).apply(instance, NewDawnBiomeSource::new));
    private final Map<String, Holder<Biome>> biomes;
    private volatile TerrainSampler terrain;
    private final ThreadLocal<BiomeCache> cache = ThreadLocal.withInitial(BiomeCache::new);

    public NewDawnBiomeSource(HolderGetter<Biome> lookup) {
        Map<String, Holder<Biome>> resolved = new HashMap<>();
        for (String id : BiomePalette.biomeIds()) {
            resolved.put(id, lookup.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace(id))));
        }
        biomes = Map.copyOf(resolved);
    }

    /** Rebinding a source to a different world is a lifecycle error, never a silent seed change. */
    public synchronized void initialize(long seed) {
        if (terrain == null) terrain = new TerrainSampler(seed);
        else if (terrain.seed() != seed) throw new IllegalStateException("New Dawn biome source was reused with a different world seed");
    }

    public TerrainSampler terrain() {
        TerrainSampler result = terrain;
        if (result == null) throw new IllegalStateException("New Dawn world seed has not been initialized by createState");
        return result;
    }

    @Override protected MapCodec<? extends BiomeSource> codec() { return CODEC; }
    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        // Minecraft assigns feature indices in biome encounter order and uses them as RNG seeds.
        // Map.copyOf iteration order is randomized across JVMs, so sorting is part of world determinism.
        return biomes.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        BiomeCache local = cache.get();
        long key = ((long) quartX << 32) | (quartZ & 0xffffffffL);
        int index = (quartX * 31 + quartZ) & 255;
        Holder<Biome> result = local.biomes.get(index);
        if (result == null || local.keys[index] != key) {
            terrain().sampleInto(QuartPos.toBlock(quartX), QuartPos.toBlock(quartZ), local.column);
            result = biomes.get(BiomePalette.select(local.column).biome());
            local.keys[index] = key;
            local.biomes.set(index, result);
            var cave = BiomePalette.caveBiome(local.column);
            local.caves.set(index, cave == null ? null : biomes.get(cave.biome()));
            local.caveCeilings[index] = BiomePalette.caveCeiling(local.column);
            local.deepDark[index] = BiomePalette.hasDeepDark(local.column);
        }
        int blockY = QuartPos.toBlock(quartY);
        if (blockY <= BiomePalette.DEEP_DARK_CEILING && local.deepDark[index]) return biomes.get(BiomePalette.deepDark().biome());
        if (blockY <= local.caveCeilings[index] && local.caves.get(index) != null) return local.caves.get(index);
        return result;
    }

    // Bounded worker-local cache: vertical quarts repeat, while carvers probe isolated distant columns.
    // Exact keys prevent hash collisions from changing results; cached values contain no world reference.
    private static final class BiomeCache {
        private final TerrainColumn column = new TerrainColumn();
        private final long[] keys = new long[256];
        private final int[] caveCeilings = new int[256];
        private final boolean[] deepDark = new boolean[256];
        private final java.util.List<Holder<Biome>> caves = new java.util.ArrayList<>(java.util.Collections.nCopies(256, null));
        private final java.util.List<Holder<Biome>> biomes = new java.util.ArrayList<>(java.util.Collections.nCopies(256, null));
    }
}
