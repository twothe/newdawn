/*
 * Copyright (c) by Stefan Feldbinder aka Two
 */
package two.newdawn.worldgen;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
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
import net.minecraft.world.gen.feature.WorldGenLiquids;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate;
import static net.minecraftforge.event.terraingen.InitMapGenEvent.EventType.*;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import static net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.DUNGEON;
import static net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.ICE;
import static net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.LAKE;
import static net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.LAVA;
import net.minecraftforge.event.terraingen.TerrainGen;
import two.newdawn.noise.NoiseStretch;
import two.newdawn.noise.SimplexNoise;
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
  protected static final boolean SHOW_WATER = true;
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
  protected final NoiseStretch heightBlockNoise;
  protected final NoiseStretch heightAreaSmall;
  protected final NoiseStretch heightAreaLarge;
  protected final NoiseStretch heightRegionNoise;
  protected final NoiseStretch fillerNoise;
  protected final NoiseStretch hillsNoise;
  protected final NoiseStretch temperatureChunkNoise;
  protected final NoiseStretch temperatureAreaNoise;
  protected final NoiseStretch temperatureRegionNoise;
  protected final NoiseStretch humidityLocalNoise;
  protected final NoiseStretch humidityAreaNoise;
  protected final NoiseStretch humidityRegionNoise;
  protected final NoiseStretch stretchForestSmallNoise;
  protected final double blockHeight;
  protected final int groundLevel;
  private final TimeCounter timeTerrain = new TimeCounter("Terrain");
  private final TimeCounter timeInfo = new TimeCounter("Info");

  public NewDawnTerrainGenerator(World world, long worldSeed, boolean useMapFeatures) {
    this.seedRandom = getRandomGenerator(worldSeed);
    worldNoise = new SimplexNoise(seedRandom);
    this.groundLevel = world.provider.getAverageGroundLevel();
    this.blockHeight = ChunkInformation.WORLD_HEIGHT / 128.0;

    this.heightBlockNoise = worldNoise.generateNoiseStretcher(23.0, 27.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
    this.heightAreaSmall = worldNoise.generateNoiseStretcher(413.0, 467.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
    this.heightAreaLarge = worldNoise.generateNoiseStretcher(913.0, 967.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
    this.heightRegionNoise = worldNoise.generateNoiseStretcher(1920.0, 1811.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
    this.fillerNoise = worldNoise.generateNoiseStretcher(16.0, 16.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
    this.hillsNoise = worldNoise.generateNoiseStretcher(897.0, 937.0, this.seedRandom.nextDouble(), this.seedRandom.nextDouble());
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

    final double x0 = chunkX << 4;
    final double z0 = chunkZ << 4;

    int x, y, z, dataPos;
    for (x = 0; x < 16; ++x) {
      for (z = 0; z < 16; ++z) {
        final double blockX = x0 + (double) x;
        final double blockZ = z0 + (double) z;
        dataPos = x + z * ChunkInformation.CHUNK_SIZE_X;

        //--- calculate world height at this coordinate ------------------------
        double hillsHeight = 0.0; // calculation for hills
        double hillsNoiseEffective = this.hillsNoise.getNoise(blockX, blockZ);
        if (hillsNoiseEffective >= 0.0) {
          hillsNoiseEffective = -(Math.cos(Math.PI * Math.pow(hillsNoiseEffective, 4.0)) - 1.0) / 2.0; // create a cosine spike from the sinus noise
          if (hillsNoiseEffective >= 0.01) { // the above calculation tends to lower hillsNoiseEffective to zero
            hillsNoiseEffective *= (this.worldNoise.noise(blockX / 67.0, blockZ / 69.0) / 2.5 + 1.0)
                    * (this.worldNoise.noise(blockX / 41.0, blockZ / 45.0) / 2.0 + 1.0)
                    * (this.worldNoise.noise(blockX / 2.2, blockZ / 2.0) / 38.0 / 2.0 + 1.0);
            hillsHeight = hillsNoiseEffective * 32.0 * this.blockHeight; // the effective height of the hill at this point
            isMountain[dataPos] = (hillsHeight > 4); // only if there is actually a reasonable amount added to the height
          }
        }

        // terrain height
        final double terrainRoughness = (this.worldNoise.noise(blockX / 1524.0, blockZ / 1798.0) + 1.0);
        final double localBlockHeight = this.heightBlockNoise.getNoise(blockX, blockZ) * terrainRoughness * 0.5 * this.blockHeight;
        final double areaSmallHeight = this.heightAreaSmall.getNoise(blockX, blockZ) * terrainRoughness * 6.0 * this.blockHeight;
        final double areaLargeHeight = this.heightAreaLarge.getNoise(blockX, blockZ) * terrainRoughness * 10.0 * this.blockHeight;
        final double regionHeight = (this.heightRegionNoise.getNoise(blockX, blockZ) + 0.25) * 8.0 / 1.25 * this.blockHeight;
        final int height = Math.min(ChunkInformation.WORLD_HEIGHT - 1, groundLevel + (int) Math.round(regionHeight + areaLargeHeight + areaSmallHeight + hillsHeight + localBlockHeight)); // final height at this coordinate

        //--- calculate temperature and humidity -------------------------------
        final double heightShifted = height + groundLevel - ChunkInformation.WORLD_HEIGHT / 2.0; // just for the formula: shift formula zero to ground-level
        final double worldHeightMod = heightShifted < 0.0 ? 0.0
                : -Math.pow(heightShifted / ((double) ChunkInformation.WORLD_HEIGHT), 3.0) * Math.pow(heightShifted * 0.4, 1.001);
        final double temperature = this.temperatureRegionNoise.getNoise(blockX, blockZ) * 0.8
                + this.temperatureAreaNoise.getNoise(blockX, blockZ) * 0.15
                + this.temperatureChunkNoise.getNoise(blockX, blockZ) * 0.05
                + worldHeightMod; // temperature reduces with height
        final double humidity = this.humidityRegionNoise.getNoise(blockX, blockZ) * 0.40
                + this.humidityAreaNoise.getNoise(blockX, blockZ) * 0.55
                + this.humidityLocalNoise.getNoise(blockX, blockZ) * 0.05
                + ((this.stretchForestSmallNoise.getNoise(blockX, blockZ) > ((temperature >= ChunkInformation.TEMPERATURE_HOT) ? 0.85 : 0.60)) ? 0.5 : 0.0) // add some small forest patches
                + worldHeightMod; // humidity reduces with height

        heightMap[dataPos] = height;
        regionHeightMap[dataPos] = (int) Math.round(regionHeight);
        temperatureMap[dataPos] = (float) temperature;
        humidityMap[dataPos] = (float) humidity;
      }
    }

    return new ChunkInformation(chunkX, chunkZ, groundLevel, heightMap, regionHeightMap, isMountain, temperatureMap, humidityMap);
  }

  protected void generateNewDawnTerrain(final ChunkInformation chunkInfo, final byte[] chunkData, final BiomeGenBase chunkBiomes[]) {
    final byte bedrockID = (byte) Block.bedrock.blockID;
    final byte stoneID = (byte) Block.stone.blockID;
    final byte waterID = (byte) Block.waterStill.blockID;
    final byte sandID = (byte) Block.sand.blockID;
    final byte sandStoneID = (byte) Block.sandStone.blockID;
    final byte dirtID = (byte) Block.dirt.blockID;
    final byte grassID = (byte) Block.grass.blockID;
    final byte savannahID = (byte) Block.hardenedClay.blockID;

    final int x0 = chunkInfo.chunkX << 4;
    final int z0 = chunkInfo.chunkZ << 4;
    BiomeGenBase blockBiome;

    int x, y, z, dataPos = 0;
    byte fillerBlockID, topBlockID;
    for (x = 0; x < 16; ++x) {
      for (z = 0; z < 16; ++z) {
        final int blockX = x0 + x;
        final int blockZ = z0 + z;

        final int height = chunkInfo.getHeight(blockX, blockZ); // the y of the first air block
        final int regionHeight = chunkInfo.getRegionHeight(blockX, blockZ);
        final float temperature = chunkInfo.getTemperature(blockX, blockZ);

        //--- decide for biome based on humidity, temperature and height -------
        if (chunkInfo.isBelowGroundLevel(blockX, blockZ)) {
          if (chunkInfo.isHumidityWoodland(blockX, blockZ) && chunkInfo.isTemperatureMedium(blockX, blockZ) && chunkInfo.isShallowWater(blockX, blockZ)) {
            blockBiome = BiomeGenBase.swampland;
          } else {
            blockBiome = getOceanBiome(temperature);
          }
        } else if (chunkInfo.isMountain(blockX, blockZ)) {
          blockBiome = getHillsBiome(temperature);
        } else {
          if (chunkInfo.isHumidityWoodland(blockX, blockZ)) {
            if ((regionHeight > 0) && (height - regionHeight >= 24)) {
              blockBiome = getForestHillsBiome(temperature);
            } else if (chunkInfo.isTemperatureMedium(blockX, blockZ) && chunkInfo.isGroundLevelOrShallowWater(blockX, blockZ)) {
              blockBiome = BiomeGenBase.swampland;
            } else {
              blockBiome = getForestsBiome(temperature);
            }
          } else if (chunkInfo.isHumiditySparse(blockX, blockZ) && chunkInfo.isGroundLevelOrShallowWater(blockX, blockZ)) {
            blockBiome = BiomeGenBase.beach;
          } else {
            blockBiome = getPlainsBiome(temperature);
          }
        }
        chunkBiomes[(x << 4) | z & 0x0F] = blockBiome;

        //--- decide for filler and top block by biome -------------------------
        fillerBlockID = blockBiome.fillerBlock;
        topBlockID = blockBiome.topBlock;
        if (chunkInfo.isBelowGroundLevel(blockX, blockZ)) {
          if (topBlockID == grassID) {
            topBlockID = dirtID; // fixes underwater-grass
          }
        } else if (chunkInfo.isHumiditySparse(blockX, blockZ) && !chunkInfo.isTemperatureHot(blockX, blockZ)) {
          topBlockID = stoneID;
          fillerBlockID = stoneID;
        } else if ((topBlockID == sandID) && chunkInfo.isTemperatureHot(blockX, blockZ) && !chunkInfo.isTemperatureHot(blockX, blockZ, -0.1f)) {
          if (chunkInfo.isHumiditySparse(blockX, blockZ)) {
            fillerBlockID = sandID;
            topBlockID = sandID;
          } else if (chunkInfo.isHumidityWet(blockX, blockZ)) {
            fillerBlockID = savannahID;
            topBlockID = savannahID;
          } else {
            fillerBlockID = sandID;
            topBlockID = savannahID;
          }
        }

        //--- write chunk data -------------------------------------------------
        chunkData[dataPos] = bedrockID;
        final int bedrockMaxHeight = Math.min(5, height - 1);
        for (int i = 1; i < bedrockMaxHeight; ++i) {
          chunkData[dataPos + i] = this.worldNoise.noise(blockX, i, blockZ) <= 0.0 ? bedrockID : stoneID; // bedrock mixed with some noise
        }

        final int heightFiller = height - 1 - (int) Math.round((this.fillerNoise.getNoise(blockX, blockZ) + 1.0) * 1.5 * this.blockHeight);
        if (heightFiller > bedrockMaxHeight) {
          Arrays.fill(chunkData, dataPos + bedrockMaxHeight, dataPos + heightFiller, stoneID); // bedrock -> almost at top
          Arrays.fill(chunkData, dataPos + heightFiller, dataPos + height - 1, fillerBlockID); // almost at top -> top
          if (fillerBlockID == sandID) {
            Arrays.fill(chunkData, dataPos + heightFiller, dataPos + (height - 1 + heightFiller) / 2, sandStoneID); // ad some sandstone so all the sand won't wall into caves
          }
        } else {
          Arrays.fill(chunkData, dataPos + bedrockMaxHeight, dataPos + height - 1, stoneID); // special case: world height is almost at bedrock level
        }
        chunkData[dataPos + height - 1] = topBlockID; // top block by biome and modifiers

        if (SHOW_WATER && chunkInfo.isBelowGroundLevel(blockX, blockZ)) { // this this part of the world below ocean level?
          Arrays.fill(chunkData, dataPos + height, dataPos + chunkInfo.groundLevel, waterID);
        }

        dataPos += ChunkInformation.WORLD_HEIGHT;
      }
    }
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

      if (y < groundLevel || this.seedRandom.nextInt(10) == 0) {
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
    final int blockX0 = chunkX * 16;
    final int blockZ0 = chunkZ * 16;

    int height, blockID, biomeID;
    BiomeGenBase blockBiome;
    final byte[] chunkBiomes = chunk.getBiomeArray();
    for (int x = 0; x < 16; ++x) {
      for (int z = 0; z < 16; ++z) {
        biomeID = chunkBiomes[x << 4 | z] & 0xFF;
        blockBiome = BiomeGenBase.biomeList[biomeID];
        if (blockBiome.getEnableSnow()) {
          height = chunk.getHeightValue(x, z);
          blockID = chunk.getBlockID(x, height, z);
          while (blockID == 0) {
            --height;
            if (height <= 0) {
              throw new IllegalStateException("Chunk (x" + chunk.xPosition + " z" + chunk.zPosition + ") at x" + blockX0 + " z" + blockZ0 + " contains no blocks.");
            }
            blockID = chunk.getBlockID(x, height, z);
          }
          if (blockID == waterID) {
            this.worldObj.setBlock(blockX0 + x, height, blockZ0 + z, iceID, 0, 2); // need to use world for proper block update distribution
          } else if (canBlockCarrySnow(blockID)) {
            this.worldObj.setBlock(blockX0 + x, height + 1, blockZ0 + z, flatSnowID, 0, 2); // need to use world for proper block update distribution
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

  BiomeGenBase getHillsBiome(final double temperature) {
    if (temperature <= ChunkInformation.TEMPERATURE_FREEZING) {
      return BiomeGenBase.iceMountains;
    } else if (temperature >= ChunkInformation.TEMPERATURE_HOT) {
      return BiomeGenBase.desertHills;
    } else {
      return BiomeGenBase.extremeHills;
    }
  }

  BiomeGenBase getPlainsBiome(final double temperature) {
    if (temperature <= ChunkInformation.TEMPERATURE_FREEZING) {
      return BiomeGenBase.icePlains;
    } else if (temperature >= ChunkInformation.TEMPERATURE_HOT) {
      return BiomeGenBase.desert;
    } else {
      return BiomeGenBase.plains;
    }
  }

  BiomeGenBase getForestsBiome(final double temperature) {
    if (temperature >= ChunkInformation.TEMPERATURE_HOT) {
      return BiomeGenBase.jungle;
    } else if (temperature <= ChunkInformation.TEMPERATURE_FREEZING) {
      return BiomeGenBase.taiga;
    } else {
      return BiomeGenBase.forest;
    }
  }

  BiomeGenBase getForestHillsBiome(final double temperature) {
    if (temperature >= ChunkInformation.TEMPERATURE_HOT) {
      return BiomeGenBase.jungleHills;
    } else if (temperature <= ChunkInformation.TEMPERATURE_FREEZING) {
      return BiomeGenBase.taigaHills;
    } else {
      return BiomeGenBase.forestHills;
    }
  }

  BiomeGenBase getOceanBiome(final double temperature) {
    return temperature <= ChunkInformation.TEMPERATURE_FREEZING ? BiomeGenBase.frozenOcean : BiomeGenBase.ocean;
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
