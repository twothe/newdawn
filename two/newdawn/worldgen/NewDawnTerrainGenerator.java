/*
 * Copyright (c) by Stefan Feldbinder aka Two
 */
package two.newdawn.worldgen;

import cpw.mods.fml.common.FMLLog;
import two.newdawn.API.WorldBaseValues;
import two.newdawn.API.ChunkInformation;
import java.security.SecureRandom;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSand;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.SpawnerAnimals;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.feature.MapGenScatteredFeature;
import net.minecraft.world.gen.feature.WorldGenDungeons;
import net.minecraft.world.gen.feature.WorldGenLakes;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraftforge.common.MinecraftForge;
import static net.minecraftforge.event.terraingen.InitMapGenEvent.EventType.*;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import static net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.DUNGEON;
import static net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.ICE;
import static net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.LAKE;
import static net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.LAVA;
import net.minecraftforge.event.terraingen.TerrainGen;
import two.newdawn.API.NewDawnBiomeSelector;
import two.newdawn.API.NewDawnBiome;
import two.newdawn.API.NewDawnBiomeList;
import two.newdawn.API.noise.NoiseStretch;
import two.newdawn.API.noise.SimplexNoise;
import two.newdawn.util.TimeCounter;

/**
 *
 * @author Two
 */
public class NewDawnTerrainGenerator implements IChunkProvider {

  /**
   *
   */
  protected static final boolean SHOW_MAP_FEATURES = true;
  protected static final boolean SHOW_MAP_DECORATION = true;
  /**
   * Reference to the World object.
   */
  private World worldObj;
  /**
   * are map structures going to be generated (e.g. strongholds)
   */
  private final boolean mapFeaturesEnabled;
  private MapGenBase caveGenerator;
  /**
   * Holds Stronghold Generator
   */
  private MapGenStronghold strongholdGenerator;
  /**
   * Holds Village Generator
   */
  private MapGenVillage villageGenerator;
  /**
   * Holds Mineshaft Generator
   */
  private MapGenMineshaft mineshaftGenerator;
  private MapGenScatteredFeature scatteredFeatureGenerator;
  /**
   * Holds ravine generator
   */
  private MapGenBase ravineGenerator;
  protected final SecureRandom seedRandom;
  protected final SimplexNoise worldNoise;
  protected final NoiseStretch terrainRoughness;
  protected final NoiseStretch heightBlock;
  protected final NoiseStretch heightAreaSmall;
  protected final NoiseStretch heightAreaLarge;
  protected final NoiseStretch heightRegionNoise;
  protected final NoiseStretch fillerNoise;
  protected final NoiseStretch temperatureChunkNoise;
  protected final NoiseStretch temperatureAreaNoise;
  protected final NoiseStretch temperatureRegionNoise;
  protected final NoiseStretch humidityLocalNoise;
  protected final NoiseStretch humidityAreaNoise;
  protected final NoiseStretch humidityRegionNoise;
  protected final NoiseStretch stretchForestSmallNoise;
  protected final WorldBaseValues baseValues;
  protected final TreeSet<NewDawnBiomeSelector> biomeSelectors;
  protected final TreeSet<NewDawnBiomeSelector> terrainModifiers;
  private final TimeCounter timeTerrain = new TimeCounter("Terrain");
  private final TimeCounter timeInfo = new TimeCounter("Info");

  public NewDawnTerrainGenerator(World world, long worldSeed, boolean useMapFeatures) {
    this.seedRandom = getRandomGenerator(worldSeed);
    worldNoise = new SimplexNoise(seedRandom);
    biomeSelectors = NewDawnBiomeList.getSelectors(worldNoise);
    terrainModifiers = new TreeSet<NewDawnBiomeSelector>();
    if (biomeSelectors.isEmpty()) {
      throw new IllegalStateException("No biome registered for NewDawn world type!");
    } else {
      for (final NewDawnBiomeSelector selector : biomeSelectors) {
        if (selector.modifiesTerrain()) {
          terrainModifiers.add(selector);
        }
        FMLLog.info("Using NewDawn biome: %s %s", selector.getClass().getSimpleName(), selector.modifiesTerrain() ? "(modifies terrain)" : "");
      }
    }

    this.baseValues = new WorldBaseValues(-0.5f, 0.5f, -0.5f, 0.5f, 0.17f, world.provider.getAverageGroundLevel());

    this.terrainRoughness = worldNoise.generateNoiseStretcher(1524.0, 1798.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
    this.heightBlock = worldNoise.generateNoiseStretcher(23.0, 27.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
    this.heightAreaSmall = worldNoise.generateNoiseStretcher(413.0, 467.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
    this.heightAreaLarge = worldNoise.generateNoiseStretcher(913.0, 967.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
    this.heightRegionNoise = worldNoise.generateNoiseStretcher(1920.0, 1811.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
    this.fillerNoise = worldNoise.generateNoiseStretcher(16.0, 16.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
    this.temperatureChunkNoise = worldNoise.generateNoiseStretcher(2.1, 2.2, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
    this.temperatureAreaNoise = worldNoise.generateNoiseStretcher(260.0, 273.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
    this.temperatureRegionNoise = worldNoise.generateNoiseStretcher(2420.0, 2590.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
    this.humidityLocalNoise = worldNoise.generateNoiseStretcher(6.0, 7.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
    this.humidityAreaNoise = worldNoise.generateNoiseStretcher(320.0, 273.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
    this.humidityRegionNoise = worldNoise.generateNoiseStretcher(880.0, 919.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
    this.stretchForestSmallNoise = worldNoise.generateNoiseStretcher(93.0, 116.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());

    caveGenerator = TerrainGen.getModdedMapGen(new NewDawnMapGenCaves(), CAVE);
    strongholdGenerator = (MapGenStronghold) TerrainGen.getModdedMapGen(new MapGenStronghold(), STRONGHOLD);
    villageGenerator = (MapGenVillage) TerrainGen.getModdedMapGen(new MapGenVillage(), VILLAGE);
    mineshaftGenerator = (MapGenMineshaft) TerrainGen.getModdedMapGen(new MapGenMineshaft(), MINESHAFT);
    scatteredFeatureGenerator = (MapGenScatteredFeature) TerrainGen.getModdedMapGen(new MapGenScatteredFeature(), SCATTERED_FEATURE);
    ravineGenerator = TerrainGen.getModdedMapGen(new NewDawnMapGenRavine(), RAVINE);

    worldObj = world;
    mapFeaturesEnabled = useMapFeatures && SHOW_MAP_FEATURES;

    BiomeGenBase.ocean.fillerBlock = (byte) (Block.sand.blockID & 0xFF);
    BiomeGenBase.ocean.topBlock = (byte) (Block.sand.blockID & 0xFF);
  }

  /**
   * Will return back a chunk, if it doesn't exist and its not a MP client it
   * will generates all the blocks for the specified chunk from the map seed and
   * chunk seed
   */
  @Override
  public Chunk provideChunk(final int chunkX, final int chunkZ) {
//    timeInfo.start();
    final ChunkInformation chunkInfo = generateChunkInformation(chunkX, chunkZ);
//    timeInfo.stop();
//    System.out.println(timeInfo);
//    timeTerrain.start();
    final byte chunkData[] = new byte[ChunkInformation.CHUNK_SIZE_XZ * ChunkInformation.WORLD_HEIGHT];
    final BiomeGenBase[] generatedBiomes = new BiomeGenBase[ChunkInformation.CHUNK_SIZE_XZ];
    generateNewDawnTerrain(chunkInfo, chunkData, generatedBiomes);
//    timeTerrain.stop();
//    System.out.println(timeTerrain);
    if (SHOW_MAP_FEATURES) {
      generateMapFeatures(chunkX, chunkZ, chunkData);
    }
    final NewDawnChunk chunk = new NewDawnChunk(worldObj, chunkData, chunkX, chunkZ);
    byte chunkBiomes[] = chunk.getBiomeArray();

    int i = -1;
    for (final BiomeGenBase biome : generatedBiomes) {
      chunkBiomes[++i] = (byte) (biome.biomeID & 0xFF);
    }

    chunk.setBiomeArray(chunkBiomes);
    chunk.generateSkylightMap();

    return chunk;
  }

  protected ChunkInformation generateChunkInformation(final int chunkX, final int chunkZ) {
    final int[] heightMap = new int[ChunkInformation.CHUNK_SIZE_XZ];
    final int[] regionHeightMap = new int[ChunkInformation.CHUNK_SIZE_XZ];
    final boolean[] isMountain = new boolean[ChunkInformation.CHUNK_SIZE_XZ];
    final float[] temperatureMap = new float[ChunkInformation.CHUNK_SIZE_XZ];
    final float[] humidityMap = new float[ChunkInformation.CHUNK_SIZE_XZ];

    final int x0 = chunkX << 4;
    final int z0 = chunkZ << 4;

    int x, y, z, dataPos;
    for (x = 0; x < 16; ++x) {
      for (z = 0; z < 16; ++z) {
        final int blockX = x0 + x;
        final int blockZ = z0 + z;
        dataPos = x + z * ChunkInformation.CHUNK_SIZE_X;

        //--- calculate world height at this coordinate ------------------------
        // terrain height
        final double roughness = this.terrainRoughness.getNoise(blockX, blockZ) + 1.0;
        final double localBlockHeight = this.heightBlock.getNoise(blockX, blockZ) * roughness * 0.5 * ChunkInformation.BLOCK_SCALE;
        final double areaSmallHeight = this.heightAreaSmall.getNoise(blockX, blockZ) * roughness * 6.0 * ChunkInformation.BLOCK_SCALE;
        final double areaLargeHeight = this.heightAreaLarge.getNoise(blockX, blockZ) * roughness * 10.0 * ChunkInformation.BLOCK_SCALE;
        final double regionHeight = (this.heightRegionNoise.getNoise(blockX, blockZ) + 0.25) * 8.0 / 1.25 * ChunkInformation.BLOCK_SCALE;
        final double baseHeight = baseValues.groundLevel + regionHeight + areaLargeHeight + areaSmallHeight + localBlockHeight;

        double heightMod = 0.0;
        boolean isModified = false;
        for (final NewDawnBiomeSelector selector : terrainModifiers) {
          if (selector.modifiesLocation(blockX, blockZ)) {
            heightMod += selector.modifyHeight(blockX, blockZ, baseHeight, regionHeight, roughness, heightMod, isModified);
            isModified = true;
          }
        }
        final int height = Math.max(1, Math.min(ChunkInformation.WORLD_HEIGHT - 1, (int) Math.round(baseHeight + heightMod))); // final height at this coordinate

        //--- calculate temperature and humidity -------------------------------
        final double heightShifted = height + baseValues.groundLevel - ChunkInformation.WORLD_HEIGHT / 2.0; // just for the formula: shift formula zero to ground-level
        final double worldHeightMod = heightShifted < 0.0 ? 0.0
                : -Math.pow(heightShifted / ((double) ChunkInformation.WORLD_HEIGHT), 3.0) * Math.pow(heightShifted * 0.4, 1.001);
        final double temperature = this.temperatureRegionNoise.getNoise(blockX, blockZ) * 0.8
                + this.temperatureAreaNoise.getNoise(blockX, blockZ) * 0.15
                + this.temperatureChunkNoise.getNoise(blockX, blockZ) * 0.05
                + worldHeightMod; // temperature reduces with height
        final double humidity = this.humidityRegionNoise.getNoise(blockX, blockZ) * 0.40
                + this.humidityAreaNoise.getNoise(blockX, blockZ) * 0.55
                + this.humidityLocalNoise.getNoise(blockX, blockZ) * 0.05
                + ((this.stretchForestSmallNoise.getNoise(blockX, blockZ) > ((temperature >= baseValues.temperatureHot) ? 0.85 : 0.60)) ? 0.5 : 0.0) // add some small forest patches
                + worldHeightMod; // humidity reduces with height

        heightMap[dataPos] = height;
        regionHeightMap[dataPos] = (int) Math.round(regionHeight);
        temperatureMap[dataPos] = (float) temperature;
        humidityMap[dataPos] = (float) humidity;
      }
    }

    return new ChunkInformation(chunkX, chunkZ, baseValues, heightMap, regionHeightMap, isMountain, temperatureMap, humidityMap);
  }

  protected void generateNewDawnTerrain(final ChunkInformation chunkInfo, final byte[] chunkData, final BiomeGenBase chunkBiomes[]) {
    final int x0 = chunkInfo.chunkX * ChunkInformation.CHUNK_SIZE_X;
    final int z0 = chunkInfo.chunkZ * ChunkInformation.CHUNK_SIZE_Z;
    NewDawnBiome blockBiome;

    int dataPos = 0, height, heightFiller, blockX, blockZ;
    for (int x = 0; x < ChunkInformation.CHUNK_SIZE_X; ++x) {
      for (int z = 0; z < ChunkInformation.CHUNK_SIZE_Z; ++z) {
        blockX = x0 + x;
        blockZ = z0 + z;
        blockBiome = getBiomeFor(blockX, blockZ, chunkInfo);
        chunkBiomes[x + z * ChunkInformation.CHUNK_SIZE_X] = blockBiome.vanillaBiome;

        height = chunkInfo.getHeight(blockX, blockZ); // the y of the first air block
        heightFiller = height - 1 - (int) Math.round((this.fillerNoise.getNoise(blockX, blockZ) + 1.0) * 1.5 * ChunkInformation.BLOCK_SCALE);
        blockBiome.fillLocation(worldNoise, chunkData, dataPos, height, heightFiller, blockX, blockZ, blockBiome, chunkInfo);

        dataPos += ChunkInformation.WORLD_HEIGHT;
      }
    }
  }

  protected NewDawnBiome getBiomeFor(final int blockX, final int blockY, final ChunkInformation chunkInfo) {
    NewDawnBiome result;
    for (final NewDawnBiomeSelector selector : biomeSelectors) {
      result = selector.selectBiome(blockX, blockY, chunkInfo);
      if (result != null) {
        return result;
      }
    }
    throw new IllegalStateException("No biome was selected during world-gen for (" + blockX + ", " + blockY + ")!");
  }

  protected void generateMapFeatures(final int chunkX, final int chunkZ, final byte[] chunkData) {
    caveGenerator.generate(this, worldObj, chunkX, chunkZ, chunkData);
    ravineGenerator.generate(this, worldObj, chunkX, chunkZ, chunkData);

    if (mapFeaturesEnabled) {
      mineshaftGenerator.generate(this, worldObj, chunkX, chunkZ, chunkData);
      villageGenerator.generate(this, worldObj, chunkX, chunkZ, chunkData);
      strongholdGenerator.generate(this, worldObj, chunkX, chunkZ, chunkData);
      scatteredFeatureGenerator.generate(this, this.worldObj, chunkX, chunkZ, chunkData);
    }
  }

  /**
   * Checks to see if a chunk exists at x, y
   */
  @Override
  public boolean chunkExists(int par1, int par2) {
    return true;
  }

  /**
   * Populates chunk with ores etc etc
   */
  @Override
  public void populate(IChunkProvider chunkProvider, int chunkX, int chunkZ) {
    BlockSand.fallInstantly = true;
    final int blockX = chunkX * 16;
    final int blockZ = chunkZ * 16;
    final BiomeGenBase biomegenbase = this.worldObj.getBiomeGenForCoords(blockX + 16, blockZ + 16);
    this.seedRandom.setSeed(this.worldObj.getSeed());
    final long seedModX = this.seedRandom.nextLong() / 2L * 2L + 1L;
    final long seedModY = this.seedRandom.nextLong() / 2L * 2L + 1L;
    this.seedRandom.setSeed((long) chunkX * seedModX + (long) chunkZ * seedModY ^ this.worldObj.getSeed());
    boolean hasVillage = false;

    MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Pre(chunkProvider, worldObj, seedRandom, chunkX, chunkZ, hasVillage));

    if (this.mapFeaturesEnabled) {
      this.mineshaftGenerator.generateStructuresInChunk(this.worldObj, this.seedRandom, chunkX, chunkZ);
      hasVillage = this.villageGenerator.generateStructuresInChunk(this.worldObj, this.seedRandom, chunkX, chunkZ);
      this.strongholdGenerator.generateStructuresInChunk(this.worldObj, this.seedRandom, chunkX, chunkZ);
      this.scatteredFeatureGenerator.generateStructuresInChunk(this.worldObj, this.seedRandom, chunkX, chunkZ);
    }

    int x;
    int y;
    int z;

    if (this.mapFeaturesEnabled && biomegenbase != BiomeGenBase.desert && biomegenbase != BiomeGenBase.desertHills && !hasVillage && this.seedRandom.nextInt(10) == 0
            && TerrainGen.populate(chunkProvider, worldObj, seedRandom, chunkX, chunkZ, hasVillage, LAKE)) {
      x = blockX + this.seedRandom.nextInt(16) + 8;
      y = this.seedRandom.nextInt(ChunkInformation.WORLD_HEIGHT);
      z = blockZ + this.seedRandom.nextInt(16) + 8;
      (new WorldGenLakes(Block.waterStill.blockID)).generate(this.worldObj, this.seedRandom, x, y, z);
    }

    if (this.mapFeaturesEnabled && TerrainGen.populate(chunkProvider, worldObj, seedRandom, chunkX, chunkZ, hasVillage, LAVA)
            && !hasVillage && this.seedRandom.nextInt(16) == 0) {
      x = blockX + this.seedRandom.nextInt(16) + 8;
      y = this.seedRandom.nextInt(this.seedRandom.nextInt(ChunkInformation.WORLD_HEIGHT - 8) + 8);
      z = blockZ + this.seedRandom.nextInt(16) + 8;

      if (y < baseValues.groundLevel || this.seedRandom.nextInt(10) == 0) {
        (new WorldGenLakes(Block.lavaStill.blockID)).generate(this.worldObj, this.seedRandom, x, y, z);
      }
    }

    if (this.mapFeaturesEnabled && TerrainGen.populate(chunkProvider, worldObj, seedRandom, chunkX, chunkZ, hasVillage, DUNGEON)) {
      for (int tries = 0; tries < 8; ++tries) {
        x = blockX + this.seedRandom.nextInt(16) + 8;
        y = this.seedRandom.nextInt(ChunkInformation.WORLD_HEIGHT);
        z = blockZ + this.seedRandom.nextInt(16) + 8;
        (new WorldGenDungeons()).generate(this.worldObj, this.seedRandom, x, y, z);
      }
    }

    if (SHOW_MAP_DECORATION) {
      biomegenbase.decorate(this.worldObj, this.seedRandom, blockX, blockZ);

      SpawnerAnimals.performWorldGenSpawning(this.worldObj, biomegenbase, blockX + 8, blockZ + 8, 16, 16, this.seedRandom);
    }

    if (this.mapFeaturesEnabled && TerrainGen.populate(chunkProvider, worldObj, seedRandom, chunkX, chunkZ, hasVillage, ICE)) {
      applyIceAndSnow(chunkX, chunkZ);
    }

    MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(chunkProvider, worldObj, seedRandom, chunkX, chunkZ, hasVillage));

    BlockSand.fallInstantly = false;
  }

  protected void applyIceAndSnow(final int chunkX, final int chunkZ) {
    final int waterID = Block.waterStill.blockID;
    final int iceID = Block.ice.blockID;
    final int flatSnowID = Block.snow.blockID;
    final Chunk chunk = this.worldObj.getChunkFromChunkCoords(chunkX, chunkZ);
    final int x0 = chunkX * ChunkInformation.CHUNK_SIZE_X;
    final int z0 = chunkZ * ChunkInformation.CHUNK_SIZE_Z;

    int height, blockID, biomeID;
    BiomeGenBase blockBiome;
    final byte[] chunkBiomes = chunk.getBiomeArray();
    for (int x = 0; x < ChunkInformation.CHUNK_SIZE_X; ++x) {
      for (int z = 0; z < ChunkInformation.CHUNK_SIZE_Z; ++z) {
        biomeID = chunkBiomes[x + z * ChunkInformation.CHUNK_SIZE_X] & 0xFF;
        blockBiome = BiomeGenBase.biomeList[biomeID];
        if (blockBiome.getEnableSnow()) {
          height = chunk.getHeightValue(x, z);
          blockID = chunk.getBlockID(x, height, z);
          while (blockID == 0) {
            --height;
            if (height <= 0) {
              throw new IllegalStateException("Chunk (x" + chunk.xPosition + " z" + chunk.zPosition + ") at x" + x0 + " z" + z0 + " contains no blocks.");
            }
            blockID = chunk.getBlockID(x, height, z);
          }
          if (blockID == waterID) {
            this.worldObj.setBlock(x0 + x, height, z0 + z, iceID, 0, 2); // need to use world for proper block update distribution
          } else if (canBlockCarrySnow(blockID)) {
            this.worldObj.setBlock(x0 + x, height + 1, z0 + z, flatSnowID, 0, 2); // need to use world for proper block update distribution
            //TODO: Gravel gets snow but then it is removed somewhere later
          }
        }
      }
    }
  }

  protected boolean canBlockCarrySnow(final int blockID) {
    if ((blockID == 0) || (blockID == Block.ice.blockID)) {
      return false;
    } else {
      final Material material = Block.blocksList[blockID].blockMaterial;
      if (material.isLiquid()
              || (material == Material.plants) // this does NOT include leaves
              || (material == Material.fire)) {
        return false;
      } else {
        return material.isSolid();
      }
    }
  }

  /**
   * Two modes of operation: if passed true, save all Chunks in one go. If
   * passed false, save up to two chunks. Return true if all chunks have been
   * saved.
   */
  @Override
  public boolean saveChunks(boolean par1, IProgressUpdate par2IProgressUpdate) {
    return true;
  }

  /**
   * Returns if the IChunkProvider supports saving.
   */
  @Override
  public boolean canSave() {
    return true;
  }

  /**
   * Converts the instance data to a readable string.
   */
  @Override
  public String makeString() {
    return "RandomLevelSource";
  }

  /**
   * Returns a list of creatures of the specified type that can spawn at the
   * given location.
   */
  @Override
  public List getPossibleCreatures(EnumCreatureType par1EnumCreatureType, int par2, int par3, int par4) {
    BiomeGenBase biomegenbase = worldObj.getBiomeGenForCoords(par2, par4);

    if (biomegenbase == null) {
      return null;
    } else {
      return biomegenbase.getSpawnableList(par1EnumCreatureType);
    }
  }

  /**
   * Returns the location of the closest structure of the specified type. If not
   * found returns null.
   */
  @Override
  public ChunkPosition findClosestStructure(World par1World, String par2Str, int par3, int par4, int par5) {
    return "Stronghold".equals(par2Str) && this.strongholdGenerator != null ? this.strongholdGenerator.getNearestInstance(par1World, par3, par4, par5) : null;
  }

  protected static SecureRandom getRandomGenerator(final long seed) {
    SecureRandom random;
    try {
      random = SecureRandom.getInstance("SHA1PRNG", "SUN");
    } catch (Exception ex) {
      random = new SecureRandom(); // take what we can get
    }
    random.setSeed(seed);
    return random;
  }

  @Override
  public int getLoadedChunkCount() {
    return 0;
  }

  @Override
  public void recreateStructures(int chunkX, int chunkZ) {
    if (this.mapFeaturesEnabled) {
      this.mineshaftGenerator.generate(this, this.worldObj, chunkX, chunkZ, (byte[]) null);
      this.villageGenerator.generate(this, this.worldObj, chunkX, chunkZ, (byte[]) null);
      this.strongholdGenerator.generate(this, this.worldObj, chunkX, chunkZ, (byte[]) null);
      this.scatteredFeatureGenerator.generate(this, this.worldObj, chunkX, chunkZ, (byte[]) null);
    }
  }

  @Override
  public boolean unloadQueuedChunks() {
    return false;
  }

  @Override
  public void saveExtraData() {
  }

  /**
   * loads or generates the chunk at the chunk location specified
   */
  @Override
  public Chunk loadChunk(int par1, int par2) {
    return provideChunk(par1, par2);
  }
}
