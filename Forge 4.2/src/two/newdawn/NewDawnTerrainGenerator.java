/*
 * Copyright (c) by Stefan Feldbinder aka Two
 */
package two.newdawn;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.Block;
import net.minecraft.src.BlockSand;
import net.minecraft.src.Chunk;
import net.minecraft.src.ChunkPosition;
import net.minecraft.src.EnumCreatureType;
import net.minecraft.src.IChunkProvider;
import net.minecraft.src.IProgressUpdate;
import net.minecraft.src.MapGenBase;
import net.minecraft.src.MapGenCaves;
import net.minecraft.src.MapGenMineshaft;
import net.minecraft.src.MapGenRavine;
import net.minecraft.src.MapGenStronghold;
import net.minecraft.src.MapGenVillage;
import net.minecraft.src.Material;
import net.minecraft.src.SpawnerAnimals;
import net.minecraft.src.World;
import net.minecraft.src.WorldGenDungeons;
import net.minecraft.src.WorldGenLakes;

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
  private static final int WORLD_HEIGHT = 128;
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
  protected final double temperatureCold = 0.5;
  protected final double temperatureHot = 1.5;
//  private final TimeCounter timeChunk = new TimeCounter("Chunk");
//  private final TimeCounter timeDecorate = new TimeCounter("Decorate");

  public NewDawnTerrainGenerator(World par1World, long worldSeed, boolean useMapFeatures) {
    this.seedRandom = getRandomGenerator(worldSeed);
    worldNoise = new SimplexNoise(seedRandom);
    this.heightBlockNoise = new NoiseStretch(worldNoise, 33.0, 37.0);
    this.heightAreaNoise = new NoiseStretch(worldNoise, 313.0, 327.0);
    this.heightRegionNoise = new NoiseStretch(worldNoise, 1020.0, 1011.0);
    this.fillerNoise = new NoiseStretch(worldNoise, 16.0, 16.0);
    this.hillsNoise = new NoiseStretch(worldNoise, 897.0, 937.0);
    this.temperatureChunkNoise = new NoiseStretch(worldNoise, 1.0, 1.0);
    this.temperatureAreaNoise = new NoiseStretch(worldNoise, 160.0, 161.0);
    this.temperatureRegionNoise = new NoiseStretch(worldNoise, 1020.0, 1010.0);
    this.humidityChunkNoise = new NoiseStretch(worldNoise, 4.0, 4.0);
    this.humidityAreaNoise = new NoiseStretch(worldNoise, 140.0, 143.0);
    this.humidityRegionNoise = new NoiseStretch(worldNoise, 620.0, 619.0);
    this.stretchForestSmallNoise = new NoiseStretch(worldNoise, 63.0, 76.0);
    caveGenerator = new MapGenCaves();
    strongholdGenerator = new MapGenStronghold();
    villageGenerator = new MapGenVillage(0);
    mineshaftGenerator = new MapGenMineshaft();
    ravineGenerator = new MapGenRavine();
    worldObj = par1World;
    mapFeaturesEnabled = useMapFeatures;
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
    byte chunkData[] = new byte[16 * 16 * WORLD_HEIGHT];
    final BiomeGenBase[] generatedBiomes = new BiomeGenBase[16 * 16];
    generateMyTerrain(chunkX, chunkZ, chunkData, generatedBiomes);
    if (SHOW_MAP_FEATURES) {
      caveGenerator.generate(this, worldObj, chunkX, chunkZ, chunkData);
      ravineGenerator.generate(this, worldObj, chunkX, chunkZ, chunkData);

      if (mapFeaturesEnabled) {
        mineshaftGenerator.generate(this, worldObj, chunkX, chunkZ, chunkData);
        villageGenerator.generate(this, worldObj, chunkX, chunkZ, chunkData);
        strongholdGenerator.generate(this, worldObj, chunkX, chunkZ, chunkData);
      }
    }
    final Chunk chunk = new Chunk(worldObj, chunkData, chunkX, chunkZ);
    byte chunkBiomes[] = chunk.getBiomeArray();

    int i = -1;
    for (final BiomeGenBase biome : generatedBiomes) {
      chunkBiomes[++i] = (byte) biome.biomeID;
    }

    chunk.setBiomeArray(chunkBiomes);
    chunk.generateSkylightMap();

    return chunk;
  }

  private void updateChunkTopBlocks(final Chunk chunk) {
    final int waterID = Block.waterStill.blockID;
    final int iceID = Block.ice.blockID;
    final int snowID = Block.snow.blockID;
    final int sandID = Block.sand.blockID;
    final int gravelID = Block.gravel.blockID;

    int height, blockID, checkHeight;
    final byte[] chunkBiomes = chunk.getBiomeArray();
    for (int x = 0; x < 16; ++x) {
      for (int z = 0; z < 16; ++z) {
        final BiomeGenBase blockBiome = BiomeGenBase.biomeList[chunkBiomes[x * 16 + z]];
        if (blockBiome.temperature <= 0.15F) {
          height = chunk.getHeightValue(x, z);
          blockID = chunk.getBlockID(x, height, z);
          while (blockID == 0) {
            --height;
            blockID = chunk.getBlockID(x, height, z);
          }
          if (blockID == waterID) {
            chunk.setBlockID(x, height, z, iceID);
          } else if (blockID != iceID) {
            checkHeight = height;
            while ((checkHeight > 0) && ((blockID == sandID) || (blockID == gravelID) || (blockID == 0))) {
              --checkHeight;
              blockID = chunk.getBlockID(x, checkHeight, z);
            }
            ++checkHeight;
            if (checkHeight < height) {
              blockID = chunk.getBlockID(x, checkHeight, z);
              while ((checkHeight < height) && ((blockID == sandID) || (blockID == gravelID))) {
                ++checkHeight;
                blockID = chunk.getBlockID(x, checkHeight, z);
              }
              if (checkHeight < height) {
                while (checkHeight < height) {
                  chunk.setBlockID(x, height, z, 0);
                  --height;
                }
                --height;
              }
            }

            if (canPlaceSnowOn(blockID)) {
              chunk.setBlockID(x, height + 1, z, snowID);
            }
          }
        }
      }
    }
  }

  protected static boolean canPlaceSnowOn(final int blockID) {
    if (blockID == 0) {
      return true;
    } else {
      final Material material = Block.blocksList[blockID].blockMaterial;
      return !(material.isGroundCover() || material.isLiquid() || (material == Material.plants));
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

      if (mapFeaturesEnabled) {
        mineshaftGenerator.generateStructuresInChunk(worldObj, seedRandom, chunkX, chunkZ);
        if (biomegenbase.biomeID != BiomeGenBase.ocean.biomeID) {
          hasVillage = villageGenerator.generateStructuresInChunk(worldObj, seedRandom, chunkX, chunkZ);
        }
        strongholdGenerator.generateStructuresInChunk(worldObj, seedRandom, chunkX, chunkZ);
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

      updateChunkTopBlocks(chunk);

      SpawnerAnimals.performWorldGenSpawning(worldObj, biomegenbase, x0 + 8, z0 + 8, 16, 16, seedRandom);

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
   * Unloads the 100 oldest chunks from memory, due to a bug with chunkSet.add()
   * never being called it thinks the list is always empty and will not remove
   * any chunks.
   */
  @Override
  public boolean unload100OldestChunks() {
    return false;
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

    BiomeGenBase.ocean.fillerBlock = sandID;
    BiomeGenBase.ocean.topBlock = sandID;

    final double x0 = chunkX << 4;
    final double z0 = chunkZ << 4;
    final int seaLevel = 63;
    BiomeGenBase blockBiome;

    int x, y, z, dataPos = 0;
    for (x = 0; x < 16; ++x) {
      for (z = 0; z < 16; ++z) {
        final double blockX = x0 + (double) x;
        final double blockZ = z0 + (double) z;

        final double varianceBlock = (this.worldNoise.noise(blockX / 224.0, blockZ / 198.0) + 1.0) * 1.0;
        final double varianceArea = (this.worldNoise.noise(blockX / 344.0, blockZ / 378.0) + 1.0) * 6.0;
        final double varianceRegion = (this.worldNoise.noise(blockX / 1000.0, blockZ / 978.0) + 1.0) * 8.0;
        final double temperature = (this.temperatureRegionNoise.getNoise(blockX, blockZ)
                + this.temperatureAreaNoise.getNoise(blockX, blockZ) / 4.0
                + this.temperatureChunkNoise.getNoise(blockX, blockZ) / 50.0) + 1.0;
        final double humidity = (this.humidityRegionNoise.getNoise(blockX, blockZ) / 1.2
                + this.humidityAreaNoise.getNoise(blockX, blockZ)
                + this.humidityChunkNoise.getNoise(blockX, blockZ) / 50.0) + 1.0;
        final boolean forestSmall = this.stretchForestSmallNoise.getNoise(blockX, blockZ) > (temperature >= temperatureHot ? 0.95 : 0.83);
        final double blockHeight = this.heightBlockNoise.getNoise(blockX, blockZ) * varianceBlock;
        final double areaHeight = this.heightAreaNoise.getNoise(blockX, blockZ) * varianceArea;
        final double regionHeight = this.heightRegionNoise.getNoise(blockX, blockZ) * varianceRegion;
        double hillsHeight = 0.0;
        double hillsNoiseEffective = this.hillsNoise.getNoise(blockX, blockZ);
        if (hillsNoiseEffective >= 0.0) {
          hillsNoiseEffective = -(Math.cos(Math.PI * Math.pow(hillsNoiseEffective, 4.0)) - 1.0) / 2.0;
          if (hillsNoiseEffective >= 0.01) {
            hillsNoiseEffective *= (this.worldNoise.noise(blockX / 67.0, blockZ / 69.0) / 2.5 + 1.0)
                    * (this.worldNoise.noise(blockX / 41.0, blockZ / 45.0) / 2.0 + 1.0)
                    * (this.worldNoise.noise(blockX / 2.2, blockZ / 2.0) / 38.0 / 2.0 + 1.0);
            hillsHeight = hillsNoiseEffective * 14.0;
          }
        } else {
          hillsNoiseEffective = 0.0;
        }
        int riverY = 0;
        if (SHOW_RIVERS) {
          final double riverNoise = Math.abs(worldNoise.noise(blockX / 512.0, blockZ / 493.0));
          if (riverNoise <= 3.0 / 60.0) {
            riverY = -3 + (int) Math.round(riverNoise * (worldNoise.noise(blockX / 128.0, blockZ / 113.0) / 20.0 + 1.0) * 64.0);
            if (riverY > 0) {
              riverY = 0;
            }
          }
        }
        final int elevationRegion = seaLevel + 1 + (int) Math.round(regionHeight);
        final int height = Math.min(WORLD_HEIGHT - 1, seaLevel + 1 + (int) Math.round(regionHeight + areaHeight + hillsHeight + blockHeight));

        final int heightFiller = height - (int) Math.round((this.fillerNoise.getNoise(blockX, blockZ) + 1.0) * 2.0);
        chunkData[dataPos] = bedrockID;
        final int bedrockMaxHeight = Math.min(5, height);
        for (int i = 1; i < bedrockMaxHeight; ++i) {
          chunkData[dataPos + i] = this.worldNoise.noise(blockX, i, blockZ) <= 0.0 ? bedrockID : stoneID;
        }

        if (height < seaLevel) {
          if (SHOW_RIVERS && (height - riverY >= seaLevel)) {
            blockBiome = BiomeGenBase.river;
          } else if ((humidity >= 1.0) && (temperature >= 1.0) && (height + 1 >= seaLevel)) {
            blockBiome = BiomeGenBase.swampland;
          } else {
            blockBiome = getOceanBiome(temperature);
          }
        } else if (SHOW_RIVERS && (riverY < 0)) {
          blockBiome = BiomeGenBase.river;
        } else if ((hillsNoiseEffective >= 0.4) && (hillsNoiseEffective + this.worldNoise.noise(blockX, blockZ) / 2.0 >= 0.4)) {
          blockBiome = getHillsBiome(temperature);
        } else {
          if (forestSmall || (humidity >= 1.42)) {
            if (areaHeight + regionHeight >= 24) {
              blockBiome = getForestHillsBiome(temperature);
            } else if ((height - 1 <= seaLevel) && (temperature >= 1.0)) {
              blockBiome = BiomeGenBase.swampland;
            } else {
              blockBiome = getForestsBiome(temperature);
            }
          } else if ((height < seaLevel + 1) && (temperature >= temperatureHot)) {
            blockBiome = BiomeGenBase.beach;
          } else {
            blockBiome = getPlainsBiome(temperature);
          }
        }
        chunkBiomes[(x << 4) | z & 0x0F] = blockBiome;

        if (heightFiller > bedrockMaxHeight) {
          Arrays.fill(chunkData, dataPos + bedrockMaxHeight, dataPos + heightFiller, stoneID);
          Arrays.fill(chunkData, dataPos + heightFiller, dataPos + height, blockBiome.fillerBlock);
          if (blockBiome.fillerBlock == sandID) {
            Arrays.fill(chunkData, dataPos + heightFiller, dataPos + (height + heightFiller) / 2, sandStoneID);
          }
        } else {
          Arrays.fill(chunkData, dataPos + bedrockMaxHeight, dataPos + height, stoneID);
        }
        if ((height < seaLevel) && ((blockBiome.topBlock == grassID))) {
          chunkData[dataPos + height] = dirtID;
        } else {
          chunkData[dataPos + height] = blockBiome.topBlock;
        }
        if (SHOW_OCEAN_WATER) {
          if (height < seaLevel) {
            if (height + 1 < seaLevel) {
              Arrays.fill(chunkData, dataPos + height + 1, dataPos + seaLevel, waterID);
            }
            chunkData[dataPos + seaLevel] = blockBiome.temperature <= 0.15F ? iceID : waterID;
          } else if (SHOW_RIVERS && (riverY < 0)) {
            final int riverHigh = Math.min(elevationRegion, height);
            final int riverLow = riverHigh + riverY;
            if (riverY < -1) {
              Arrays.fill(chunkData, dataPos + riverLow, dataPos + riverHigh, waterID);
            }
            chunkData[dataPos + riverHigh] = blockBiome.temperature <= 0.15F ? iceID : waterID;
            if (riverHigh < height) {
              Arrays.fill(chunkData, dataPos + riverHigh + 1, dataPos + height + 1, (byte) 0);
            }
          }
        }
        dataPos += WORLD_HEIGHT;
      }
    }

//    timeChunk.stop();
//    System.out.println(timeChunk);
  }

  BiomeGenBase getHillsBiome(final double temperature) {
    if (temperature <= temperatureCold) {
      return BiomeGenBase.iceMountains;
    } else if (temperature >= temperatureHot) {
      return BiomeGenBase.desertHills;
    } else {
      return BiomeGenBase.extremeHills;
    }
  }

  BiomeGenBase getPlainsBiome(final double temperature) {
    if (temperature <= temperatureCold) {
      return BiomeGenBase.icePlains;
    } else if (temperature >= temperatureHot) {
      return BiomeGenBase.desert;
    } else {
      return BiomeGenBase.plains;
    }
  }

  BiomeGenBase getForestsBiome(final double temperature) {
    if (temperature >= temperatureHot) {
      return BiomeGenBase.jungle;
    } else if (temperature <= temperatureCold) {
      return BiomeGenBase.taiga;
    } else {
      return BiomeGenBase.forest;
    }
  }

  BiomeGenBase getForestHillsBiome(final double temperature) {
    if (temperature >= temperatureHot) {
      return BiomeGenBase.jungleHills;
    } else if (temperature <= temperatureCold) {
      return BiomeGenBase.taigaHills;
    } else {
      return BiomeGenBase.forestHills;
    }
  }

  BiomeGenBase getOceanBiome(final double temperature) {
    return temperature <= temperatureCold ? BiomeGenBase.frozenOcean : BiomeGenBase.ocean;
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
}
