package two.newdawn.integration;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import two.newdawn.terrain.TerrainChunk;
import two.newdawn.terrain.TerrainColumn;
import two.newdawn.terrain.BiomePalette;
import two.newdawn.terrain.TerrainSample;
import two.newdawn.terrain.TerrainSampler;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Replaces the terrain fill while retaining Minecraft's biome decoration, carvers, structures and mobs.
 * All writes target the chunk owned by the current generation task; no executor or shared RNG is added.
 */
public final class NewDawnChunkGenerator extends NoiseBasedChunkGenerator {
    public static final MapCodec<NewDawnChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            NewDawnBiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.source),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(NewDawnChunkGenerator::generatorSettings)
    ).apply(instance, NewDawnChunkGenerator::new));
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private final NewDawnBiomeSource source;

    public NewDawnChunkGenerator(NewDawnBiomeSource source, Holder<NoiseGeneratorSettings> settings) {
        super(source, settings);
        if (settings.value().seaLevel() != TerrainSampler.SEA_LEVEL) {
            throw new IllegalArgumentException("New Dawn noise settings must use sea_level 64");
        }
        this.source = source;
    }

    @Override protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structures, RandomState random, long seed) {
        source.initialize(seed);
        return super.createState(structures, random, seed);
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(RandomState random, Blender blender, StructureManager structures, ChunkAccess chunk) {
        chunk.fillBiomesFromNoise(source, random.sampler());
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
        TerrainChunk columns = new TerrainChunk();
        source.terrain().sampleChunkInto(chunk.getPos().x, chunk.getPos().z, columns);
        int minimum = Math.max(getMinY(), chunk.getMinBuildHeight());
        int maximum = Math.min(Math.min(getMinY() + getGenDepth(), chunk.getMaxBuildHeight()),
                Math.max(columns.maximumHeight(), getSeaLevel()));
        if (minimum >= maximum) return CompletableFuture.completedFuture(chunk);
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        TerrainColumn column = new TerrainColumn();
        TerrainBlockColumn layers = new TerrainBlockColumn();
        int lowestFiller = maximum;
        for (int index = 0; index < TerrainChunk.COLUMN_COUNT; index++) {
            lowestFiller = Math.min(lowestFiller, columns.height(index) - 1 - columns.fillerDepth(index));
        }
        // Only complete sections strictly above bedrock and below every column's filler are uniform.
        int uniformStart = (Math.max(minimum, getMinY() + 1) + 15) & ~15;
        int uniformEnd = Math.min(maximum, lowestFiller) & ~15;
        fillUniformRock(chunk, uniformStart, uniformEnd);
        int firstSection = chunk.getSectionIndex(minimum);
        int lastSection = chunk.getSectionIndex(maximum - 1);
        int acquiredThrough = firstSection - 1;
        try {
            for (int section = firstSection; section <= lastSection; section++) {
                chunk.getSection(section).acquire();
                acquiredThrough = section;
            }
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    columns.copyColumn(x + z * 16, column);
                    layers.prepare(column.height(), column.fillerDepth(), BiomePalette.select(column), getMinY());
                    if (uniformStart < uniformEnd) {
                        layers.fill(chunk, x, z, minimum, uniformStart);
                        layers.fill(chunk, x, z, uniformEnd, maximum);
                    } else {
                        layers.fill(chunk, x, z, minimum, maximum);
                    }
                    int solidY = Math.min(column.height(), maximum) - 1;
                    if (solidY >= minimum) {
                        BlockState top = layers.stateAt(solidY);
                        oceanFloor.update(x, solidY, z, top);
                        worldSurface.update(x, solidY, z, top);
                    }
                    int waterY = Math.min(getSeaLevel(), maximum) - 1;
                    if (waterY >= Math.max(column.height(), minimum)) worldSurface.update(x, waterY, z, WATER);
                }
            }
        } finally {
            for (int section = acquiredThrough; section >= firstSection; section--) chunk.getSection(section).release();
        }
        return CompletableFuture.completedFuture(chunk);
    }

    /** Installs compact single-state palettes while preserving the already-generated biome containers. */
    private static void fillUniformRock(ChunkAccess chunk, int start, int end) {
        for (int y = start; y < end; y += 16) {
            int index = chunk.getSectionIndex(y);
            var previous = chunk.getSection(index);
            BlockState rock = (y < 0 ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState();
            var states = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, rock, PalettedContainer.Strategy.SECTION_STATES);
            // The constructor derives block/tick counts from the palette. This task exclusively owns the chunk.
            chunk.getSections()[index] = new LevelChunkSection(states, previous.getBiomes());
        }
    }

    /** Surface layers are filled with stone; a second vanilla surface pass would replace the chosen biome materials. */
    @Override public void buildSurface(WorldGenRegion level, StructureManager structures, RandomState random, ChunkAccess chunk) {}

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random,
                             BiomeManager biomes, StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {
        TerrainCarvers.apply(this, source.terrain(), level, seed, random, biomes, structures, chunk, step);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        int height = source.terrain().sampleHeight(x, z);
        int minimum = Math.max(getMinY(), level.getMinBuildHeight());
        int maximum = Math.min(getMinY() + getGenDepth(), level.getMaxBuildHeight());
        // Every palette material is solid for all vanilla heightmap types; only water inclusion differs.
        int surface = type.isOpaque().test(WATER) ? Math.max(height, getSeaLevel()) : height;
        int result = Math.min(maximum, surface);
        return result > minimum ? result : level.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        int minimum = Math.max(getMinY(), level.getMinBuildHeight());
        int maximum = Math.max(minimum, Math.min(getMinY() + getGenDepth(), level.getMaxBuildHeight()));
        TerrainColumn column = new TerrainColumn();
        source.terrain().sampleInto(x, z, column);
        TerrainBlockColumn layers = new TerrainBlockColumn();
        layers.prepare(column.height(), column.fillerDepth(), BiomePalette.select(column), getMinY());
        return new NoiseColumn(minimum, layers.toStates(minimum, maximum));
    }

    @Override public int getSpawnHeight(LevelHeightAccessor level) { return getSeaLevel() + 1; }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        TerrainSample sample = source.terrain().sample(pos.getX(), pos.getZ());
        info.add("New Dawn: height=" + sample.height() + " temperature=" + sample.temperature() + " humidity=" + sample.humidity());
    }

}
