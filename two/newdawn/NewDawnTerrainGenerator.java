/*
 * Copyright (c) by Stefan Feldbinder aka Two
 */
package two.newdawn;

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
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraftforge.common.MinecraftForge;
import static net.minecraftforge.event.terraingen.InitMapGenEvent.EventType.*;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.TerrainGen;

/**
 *
 * @author Two
 */
public class NewDawnTerrainGenerator implements IChunkProvider {

  /**
   *
   */
  private static final boolean SHOW_MAP_FEATURES = true;
  private static final boolean SHOW_MAP_DECORATION = true;
  private static final boolean SHOW_OCEAN_WATER = true;
  private static final boolean SHOW_RIVERS = false;
  private static final int WORLD_HEIGHT = 256;
  protected final double TEMPERATURE_FREEZING = -0.5;
  protected final double TEMPERATURE_HOT = 0.5;
  protected final double HUMIDITY_SPARSE = -0.5;
  protected final double HUMIDITY_WET = 0.5;
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
  protected final NoiseStretch heightAreaNoise;
  protected final NoiseStretch heightRegionNoise;
  protected final NoiseStretch fillerNoise;
  protected final NoiseStretch hillsNoise;
  protected final NoiseStretch temperatureChunkNoise;
  protected final NoiseStretch temperatureAreaNoise;
  protected final NoiseStretch temperatureRegionNoise;
  protected final NoiseStretch humidityChunkNoise;
  protected final NoiseStretch humidityAreaNoise;
  protected final NoiseStretch humidityRegionNoise;
  protected final NoiseStretch stretchForestSmallNoise;
  protected final double groundLevel, seaLevel, minimumHeight, availableHeight, blockHeight;
//  private final TimeCounter timeChunk = new TimeCounter("Chunk");
//  private final TimeCounter timeDecorate = new TimeCounter("Decorate");

  public NewDawnTerrainGenerator(World world, long worldSeed, boolean useMapFeatures) {
    this.seedRandom = getRandomGenerator(worldSeed);
    worldNoise = new SimplexNoise(seedRandom);
    this.groundLevel = world.provider.getAverageGroundLevel();
    this.seaLevel = this.groundLevel - 1;
    this.minimumHeight = this.groundLevel - 16;
    this.availableHeight = WORLD_HEIGHT - this.minimumHeight;
    this.blockHeight = WORLD_HEIGHT / 128.0;

    this.heightRegionNoise = worldNoise.generateNoiseStretcher(1220.0, 1211.0);
    this.heightBlockNoise = worldNoise.generateNoiseStretcher(33.0, 37.0);
    this.heightAreaNoise = worldNoise.generateNoiseStretcher(413.0, 427.0);
    this.fillerNoise = worldNoise.generateNoiseStretcher(16.0, 16.0);
    this.hillsNoise = worldNoise.generateNoiseStretcher(897.0, 937.0);
    this.temperatureChunkNoise = worldNoise.generateNoiseStretcher(1.0, 1.0);
    this.temperatureAreaNoise = worldNoise.generateNoiseStretcher(260.0, 361.0);
    this.temperatureRegionNoise = worldNoise.generateNoiseStretcher(1320.0, 1110.0);
    this.humidityChunkNoise = worldNoise.generateNoiseStretcher(4.0, 4.0);
    this.humidityAreaNoise = worldNoise.generateNoiseStretcher(340.0, 243.0);
    this.humidityRegionNoise = worldNoise.generateNoiseStretcher(670.0, 519.0);
    this.stretchForestSmallNoise = worldNoise.generateNoiseStretcher(63.0, 76.0);

    caveGenerator = new NewDawnMapGenCaves();
    strongholdGenerator = (MapGenStronghold) TerrainGen.getModdedMapGen(new MapGenStronghold(), STRONGHOLD);
    villageGenerator = (MapGenVillage) TerrainGen.getModdedMapGen(new MapGenVillage(), VILLAGE);
    mineshaftGenerator = (MapGenMineshaft) TerrainGen.getModdedMapGen(new MapGenMineshaft(), MINESHAFT);
    scatteredFeatureGenerator = (MapGenScatteredFeature) TerrainGen.getModdedMapGen(new MapGenScatteredFeature(), SCATTERED_FEATURE);
    ravineGenerator = new NewDawnMapGenRavine();

    worldObj = world;
    mapFeaturesEnabled = useMapFeatures;
  }

  private void generateMyTerrain(final int chunkX, final int chunkZ, final byte[] chunkData, final BiomeGenBase chunkBiomes[]) {
//    timeChunk.start();
    final byte bedrockID = (byte) Block.bedrock.blockID;
    final byte stoneID = (byte) Block.stone.blockID;
    final byte waterID = (byte) Block.waterStill.blockID;
    final byte iceID = (byte) Block.ice.blockID;
    final byte sandID = (byte) Block.sand.blockID;
    final byte sandStoneID = (byte) Block.sandStone.blockID;
    final byte dirtID = (byte) Block.dirt.blockID;
    final byte grassID = (byte) Block.grass.blockID;
    final byte savannahID = (byte) Block.hardenedClay.blockID;

    BiomeGenBase.ocean.fillerBlock = sandID;
    BiomeGenBase.ocean.topBlock = sandID;

    final double x0 = chunkX << 4;
    final double z0 = chunkZ << 4;
    BiomeGenBase blockBiome;

    int x, y, z, dataPos = 0;
    for (x = 0; x < 16; ++x) {
      for (z = 0; z < 16; ++z) {
        final double blockX = x0 + (double) x;
        final double blockZ = z0 + (double) z;

        final double terrainRoughness = (this.worldNoise.noise(blockX / 1524.0, blockZ / 1798.0) + 1.0);
        final double localBlockHeight = this.heightBlockNoise.getNoise(blockX, blockZ) * terrainRoughness * 1.0 * this.blockHeight;
        final double areaHeight = this.heightAreaNoise.getNoise(blockX, blockZ) * terrainRoughness * 3.0 * this.blockHeight;
        final double regionHeight = this.heightRegionNoise.getNoise(blockX, blockZ) * terrainRoughness * 8.0 * this.blockHeight;
        final double temperature = this.temperatureRegionNoise.getNoise(blockX, blockZ) * 0.7
                + this.temperatureAreaNoise.getNoise(blockX, blockZ) * 0.25
                + this.temperatureChunkNoise.getNoise(blockX, blockZ) * 0.05;
        final double humidity = this.humidityRegionNoise.getNoise(blockX, blockZ) * 0.65
                + this.humidityAreaNoise.getNoise(blockX, blockZ) * 0.3
                + this.humidityChunkNoise.getNoise(blockX, blockZ) * 0.05;
        final boolean forestSmall = this.stretchForestSmallNoise.getNoise(blockX, blockZ) > (temperature >= TEMPERATURE_HOT ? 0.95 : 0.83);
        double hillsHeight = 0.0;
        double hillsNoiseEffective = this.hillsNoise.getNoise(blockX, blockZ);
        if (hillsNoiseEffective >= 0.0) {
          hillsNoiseEffective = -(Math.cos(Math.PI * Math.pow(hillsNoiseEffective, 4.0)) - 1.0) / 2.0;
          if (hillsNoiseEffective >= 0.01) {
            hillsNoiseEffective *= (this.worldNoise.noise(blockX / 67.0, blockZ / 69.0) / 2.5 + 1.0)
                    * (this.worldNoise.noise(blockX / 41.0, blockZ / 45.0) / 2.0 + 1.0)
                    * (this.worldNoise.noise(blockX / 2.2, blockZ / 2.0) / 38.0 / 2.0 + 1.0);
            hillsHeight = hillsNoiseEffective * 14.0 * this.blockHeight;
          }
        } else {
          hillsNoiseEffective = 0.0;
        }
        int riverY = 1;
        if (SHOW_RIVERS) {
          final double RIVER_THRESHOLD = 3.0 / 260.0;
          final double riverNoise = Math.abs(worldNoise.noise(blockX / 1212.0, blockZ / 1493.0));
          if (riverNoise <= RIVER_THRESHOLD) {
            riverY = (int) Math.round(-2.0 + 2.0 * Math.pow(riverNoise / RIVER_THRESHOLD, 2.0));
            if (riverY > 0) {
              riverY = 0;
            }
          }
        }
        int height = Math.min(WORLD_HEIGHT - 1, (int) seaLevel + 1 + (int) Math.round(regionHeight + areaHeight + hillsHeight + localBlockHeight));

        chunkData[dataPos] = bedrockID;
        final int bedrockMaxHeight = Math.min(5, height);
        for (int i = 1; i < bedrockMaxHeight; ++i) {
          chunkData[dataPos + i] = this.worldNoise.noise(blockX, i, blockZ) <= 0.0 ? bedrockID : stoneID;
        }

        if (height < seaLevel) {
          if (SHOW_RIVERS && (height - riverY >= seaLevel)) {
            blockBiome = BiomeGenBase.river;
          } else if ((humidity > HUMIDITY_SPARSE) && (temperature > TEMPERATURE_FREEZING) && (height + 1 >= seaLevel)) {
            blockBiome = BiomeGenBase.swampland;
          } else {
            blockBiome = getOceanBiome(temperature);
          }
        } else if (SHOW_RIVERS && (riverY <= 0)) {
          blockBiome = BiomeGenBase.river;
          if (riverY == 0) {
            height = (int) Math.round((double) (height + seaLevel) / 2.0);
          }
        } else if ((hillsNoiseEffective >= 0.4) && (hillsNoiseEffective + this.worldNoise.noise(blockX, blockZ) / 2.0 >= 0.4)) {
          blockBiome = getHillsBiome(temperature);
        } else {
          if (forestSmall || (humidity >= 0.0)) {
            if (areaHeight + regionHeight >= 24) {
              blockBiome = getForestHillsBiome(temperature);
            } else if ((height - 1 <= seaLevel) && (temperature >= 0.0)) {
              blockBiome = BiomeGenBase.swampland;
            } else {
              blockBiome = getForestsBiome(temperature);
            }
          } else if ((height < seaLevel + 1) && (temperature >= TEMPERATURE_HOT)) {
            blockBiome = BiomeGenBase.beach;
          } else {
            blockBiome = getPlainsBiome(temperature);
          }
        }
        chunkBiomes[(x << 4) | z & 0x0F] = blockBiome;

        final int heightFiller = height - (int) Math.round((this.fillerNoise.getNoise(blockX, blockZ) + 1.0) * 2.0 * this.blockHeight);
        if (heightFiller > bedrockMaxHeight) {
          Arrays.fill(chunkData, dataPos + bedrockMaxHeight, dataPos + heightFiller, stoneID);
          Arrays.fill(chunkData, dataPos + heightFiller, dataPos + height, blockBiome.fillerBlock);
          if (blockBiome.fillerBlock == sandID) {
            Arrays.fill(chunkData, dataPos + heightFiller, dataPos + (height + heightFiller) / 2, sandStoneID);
          }
        } else {
          Arrays.fill(chunkData, dataPos + bedrockMaxHeight, dataPos + height, stoneID);
        }
        if ((blockBiome.topBlock == grassID) && (height < seaLevel)) {
          chunkData[dataPos + height] = dirtID;
        } else if ((blockBiome.topBlock == sandID) && (temperature >= TEMPERATURE_HOT) &&(temperature < TEMPERATURE_HOT + 0.15) && (humidity < HUMIDITY_WET)) {
          chunkData[dataPos + height] = savannahID;
        } else {
          chunkData[dataPos + height] = blockBiome.topBlock;
        }
        if (SHOW_OCEAN_WATER) {
          if (SHOW_RIVERS && (riverY < 0)) {
            final int riverHigh = (int) seaLevel;
            final int riverLow = Math.min(height + 1, riverHigh + riverY + 1);
            if (riverLow <= riverHigh) {
              Arrays.fill(chunkData, dataPos + riverLow, dataPos + riverHigh, waterID);
            }
            chunkData[dataPos + riverHigh] = blockBiome.temperature <= 0.15F ? iceID : waterID;
            if (riverHigh < height) {
              Arrays.fill(chunkData, dataPos + riverHigh + 1, dataPos + height + 1, (byte) 0);
            }
          } else if (height < seaLevel) {
            if (height + 1 < seaLevel) {
              Arrays.fill(chunkData, dataPos + height + 1, dataPos + (int) seaLevel, waterID);
            }
            chunkData[dataPos + (int) seaLevel] = blockBiome.temperature <= 0.15F ? iceID : waterID;
          }
        }
        dataPos += WORLD_HEIGHT;
      }
    }

//    timeChunk.stop();
//    System.out.println(timeChunk);
  }

  /**
   * loads or generates the chunk at the chunk location specified
   */
  @Override
  public Chunk loadChunk(int par1, int par2) {
    return provideChunk(par1, par2);
  }

  /**
   * Will return back a chunk, if it doesn't exist and its not a MP client it
   * will generates all the blocks for the specified chunk from the map seed and
   * chunk seed
   */
  @Override
  public Chunk provideChunk(int chunkX, int chunkZ) {
    final byte chunkData[] = new byte[16 * 16 * WORLD_HEIGHT];
    final BiomeGenBase[] generatedBiomes = new BiomeGenBase[16 * 16];
    generateMyTerrain(chunkX, chunkZ, chunkData, generatedBiomes);
    if (SHOW_MAP_FEATURES) {
      caveGenerator.generate(this, worldObj, chunkX, chunkZ, chunkData);
      ravineGenerator.generate(this, worldObj, chunkX, chunkZ, chunkData);

      if (mapFeaturesEnabled) {
        mineshaftGenerator.generate(this, worldObj, chunkX, chunkZ, chunkData);
        villageGenerator.generate(this, worldObj, chunkX, chunkZ, chunkData);
        strongholdGenerator.generate(this, worldObj, chunkX, chunkZ, chunkData);
        scatteredFeatureGenerator.generate(this, this.worldObj, chunkX, chunkZ, chunkData);
      }
    }
    final NewDawnChunk chunk = new NewDawnChunk(worldObj, chunkData, chunkX, chunkZ);
    byte chunkBiomes[] = chunk.getBiomeArray();

    int i = -1;
    for (final BiomeGenBase biome : generatedBiomes) {
      chunkBiomes[++i] = (byte) biome.biomeID;
    }

    chunk.setBiomeArray(chunkBiomes);
    chunk.generateSkylightMap();

    return chunk;
  }

  protected static boolean canPlaceSnowOn(final int blockID) {
    if (blockID == 0) {
      return true;
    } else {
      final Material material = Block.blocksList[blockID].blockMaterial;
      return material.blocksMovement() && !(material.isLiquid() || (material == Material.plants));
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
  public void populate(IChunkProvider world, int chunkX, int chunkZ) {
    if (SHOW_MAP_DECORATION) {
//      timeDecorate.start();
      BlockSand.fallInstantly = true;
      final Chunk chunk = worldObj.getChunkFromChunkCoords(chunkX, chunkZ);
      int x0 = chunkX * 16;
      int z0 = chunkZ * 16;
      BiomeGenBase biomegenbase = worldObj.getBiomeGenForCoords(x0 + 16, z0 + 16);
      seedRandom.setSeed((long) ((worldNoise.noise(chunkX, chunkZ) + 1.0) / 2.0 * Long.MAX_VALUE));
      boolean hasVillage = false;

      MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Pre(world, worldObj, this.seedRandom, chunkX, chunkZ, hasVillage));

      if (mapFeaturesEnabled) {
        mineshaftGenerator.generateStructuresInChunk(worldObj, seedRandom, chunkX, chunkZ);
        if (biomegenbase.biomeID != BiomeGenBase.ocean.biomeID) {
          hasVillage = villageGenerator.generateStructuresInChunk(worldObj, seedRandom, chunkX, chunkZ);
        }
        strongholdGenerator.generateStructuresInChunk(worldObj, seedRandom, chunkX, chunkZ);
        scatteredFeatureGenerator.generateStructuresInChunk(worldObj, seedRandom, chunkX, chunkZ);
      }

      if (!hasVillage && seedRandom.nextInt(4) == 0) {
        int x = x0 + seedRandom.nextInt(16) + 8;
        int y = seedRandom.nextInt(128);
        int z = z0 + seedRandom.nextInt(16) + 8;
        (new WorldGenLakes(Block.waterStill.blockID)).generate(worldObj, seedRandom, x, y, z);
      }

      if (!hasVillage && seedRandom.nextInt(8) == 0) {
        int x = x0 + seedRandom.nextInt(16) + 8;
        int y = seedRandom.nextInt(seedRandom.nextInt(120) + 8);
        int z = z0 + seedRandom.nextInt(16) + 8;

        if (y < 63 || seedRandom.nextInt(10) == 0) {
          (new WorldGenLakes(Block.lavaStill.blockID)).generate(worldObj, seedRandom, x, y, z);
        }
      }

      for (int j1 = 0; j1 < 8; j1++) {
        int x = x0 + seedRandom.nextInt(16) + 8;
        int y = seedRandom.nextInt(128);
        int z = z0 + seedRandom.nextInt(16) + 8;

        if (!(new WorldGenDungeons()).generate(worldObj, seedRandom, x, y, z));
      }

      biomegenbase.decorate(worldObj, seedRandom, x0, z0);

      SpawnerAnimals.performWorldGenSpawning(worldObj, biomegenbase, x0 + 8, z0 + 8, 16, 16, seedRandom);

      MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(world, worldObj, this.seedRandom, chunkX, chunkZ, hasVillage));

      BlockSand.fallInstantly = false;

//      timeDecorate.stop();
//      System.out.println(timeDecorate);
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
    if ("Stronghold".equals(par2Str) && strongholdGenerator != null) {
      return strongholdGenerator.getNearestInstance(par1World, par3, par4, par5);
    } else {
      return null;
    }
  }

  BiomeGenBase getHillsBiome(final double temperature) {
    if (temperature <= TEMPERATURE_FREEZING) {
      return BiomeGenBase.iceMountains;
    } else if (temperature >= TEMPERATURE_HOT) {
      return BiomeGenBase.desertHills;
    } else {
      return BiomeGenBase.extremeHills;
    }
  }

  BiomeGenBase getPlainsBiome(final double temperature) {
    if (temperature <= TEMPERATURE_FREEZING) {
      return BiomeGenBase.icePlains;
    } else if (temperature >= TEMPERATURE_HOT) {
      return BiomeGenBase.desert;
    } else {
      return BiomeGenBase.plains;
    }
  }

  BiomeGenBase getForestsBiome(final double temperature) {
    if (temperature >= TEMPERATURE_HOT) {
      return BiomeGenBase.jungle;
    } else if (temperature <= TEMPERATURE_FREEZING) {
      return BiomeGenBase.taiga;
    } else {
      return BiomeGenBase.forest;
    }
  }

  BiomeGenBase getForestHillsBiome(final double temperature) {
    if (temperature >= TEMPERATURE_HOT) {
      return BiomeGenBase.jungleHills;
    } else if (temperature <= TEMPERATURE_FREEZING) {
      return BiomeGenBase.taigaHills;
    } else {
      return BiomeGenBase.forestHills;
    }
  }

  BiomeGenBase getOceanBiome(final double temperature) {
    return temperature <= TEMPERATURE_FREEZING ? BiomeGenBase.frozenOcean : BiomeGenBase.ocean;
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
}
