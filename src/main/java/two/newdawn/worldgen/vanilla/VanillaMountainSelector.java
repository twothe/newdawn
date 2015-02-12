/*
 */
package two.newdawn.worldgen.vanilla;

import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeGenBase;
import two.newdawn.API.ChunkInformation;
import two.newdawn.API.ChunkInformation.Humidity;
import two.newdawn.API.ChunkInformation.Temperature;
import two.newdawn.API.NewDawnBiome;
import two.newdawn.API.NewDawnBiomeSelector;
import two.newdawn.API.noise.NoiseStretch;
import two.newdawn.API.noise.SimplexNoise;

/**
 * @author Two
 */
public class VanillaMountainSelector extends NewDawnBiomeSelector {

  /* Noises for mountain generation */
  protected final NoiseStretch hillsNoise;
  protected final NoiseStretch hillsNoiseBlock;
  protected final NoiseStretch hillsNoiseAreaSmall;
  protected final NoiseStretch hillsNoiseAreaLarge;
  protected final NewDawnBiome[][] selection = new NewDawnBiome[ChunkInformation.Temperature.values().length][ChunkInformation.Humidity.values().length];

  public VanillaMountainSelector(final SimplexNoise worldNoise, final int priority) {
    super(worldNoise, priority);

    final Random random = worldNoise.getRandom();
    this.hillsNoise = worldNoise.generateNoiseStretcher(897.0, 957.0, random.nextDouble(), random.nextDouble());
    this.hillsNoiseBlock = worldNoise.generateNoiseStretcher(2.0, 2.2, random.nextDouble(), random.nextDouble());
    this.hillsNoiseAreaSmall = worldNoise.generateNoiseStretcher(41.0, 45.0, random.nextDouble(), random.nextDouble());
    this.hillsNoiseAreaLarge = worldNoise.generateNoiseStretcher(127.0, 119.0, random.nextDouble(), random.nextDouble());

    this.selection[Temperature.cold.ordinal()][Humidity.sparse.ordinal()] = new NewDawnBiome(BiomeGenBase.iceMountains, Blocks.stone, Blocks.stone);
    this.selection[Temperature.medium.ordinal()][Humidity.sparse.ordinal()] = new NewDawnBiome(BiomeGenBase.extremeHills, Blocks.stone, Blocks.stone);
    this.selection[Temperature.hot.ordinal()][Humidity.sparse.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.desertHills);

    this.selection[Temperature.cold.ordinal()][Humidity.medium.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.iceMountains);
    this.selection[Temperature.medium.ordinal()][Humidity.medium.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.extremeHills);
    this.selection[Temperature.hot.ordinal()][Humidity.medium.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.savannaPlateau);

    this.selection[Temperature.cold.ordinal()][Humidity.woodland.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.coldTaigaHills);
    this.selection[Temperature.medium.ordinal()][Humidity.woodland.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.forestHills);
    this.selection[Temperature.hot.ordinal()][Humidity.woodland.ordinal()] = new NewDawnBiome(BiomeGenBase.mesaPlateau, Blocks.grass, Blocks.hardened_clay);

    this.selection[Temperature.cold.ordinal()][Humidity.wet.ordinal()] = this.selection[Temperature.cold.ordinal()][Humidity.woodland.ordinal()];
    this.selection[Temperature.medium.ordinal()][Humidity.wet.ordinal()] = this.selection[Temperature.medium.ordinal()][Humidity.woodland.ordinal()];
    this.selection[Temperature.hot.ordinal()][Humidity.wet.ordinal()] = new NewDawnBiome(BiomeGenBase.mesaPlateau_F, Blocks.hardened_clay, Blocks.hardened_clay);
  }

  @Override
  public boolean modifiesTerrain() {
    return true;
  }

  @Override
  public boolean modifiesLocation(int blockX, int blockZ, final ChunkInformation chunkInfo) {
    return (getEffectiveHillsNoise(blockX, blockZ) > 0.0);
  }

  @Override
  public double modifyHeight(final int blockX, final int blockZ, final double baseHeight, final double regionHeight, final double roughness, final double currentModification, final boolean isModified, final ChunkInformation chunkInfo) {
    final double hillsNoiseEffective = getEffectiveHillsNoise(blockX, blockZ);
    final double hillsHeight = (this.hillsNoiseAreaLarge.getNoise(blockX, blockZ) * 0.4
            + this.hillsNoiseAreaSmall.getNoise(blockX, blockZ) * 0.59
            + this.hillsNoiseBlock.getNoise(blockX, blockZ) * 0.01
            + 0.5)
            * hillsNoiseEffective * 32.0 * ChunkInformation.BLOCK_SCALE; // the effective height of the hill at this point
    chunkInfo.isMountain[ChunkInformation.blockToChunk(blockX, blockZ)] = (hillsHeight > 4);
    return hillsHeight;
  }

  protected double getEffectiveHillsNoise(int blockX, int blockZ) {
    double hillsNoiseEffective = this.hillsNoise.getNoise(blockX, blockZ);
    if (hillsNoiseEffective >= 0.0) {
      hillsNoiseEffective = -(Math.cos(Math.PI * Math.pow(hillsNoiseEffective, 4.0)) - 1.0) / 2.0; // create a cosine spike from the sinus noise
      if (hillsNoiseEffective >= 0.01) { // the above calculation tends to lower hillsNoiseEffective to zero
        return hillsNoiseEffective;
      }
    }
    return 0.0;
  }

  @Override
  public NewDawnBiome selectBiome(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (chunkInfo.isMountain(blockX, blockZ) && chunkInfo.isAboveSeaLevel(blockX, blockZ)) {
      final int temperature = chunkInfo.getTemperatureType(blockX, blockZ).ordinal();
      final int humidity = chunkInfo.getHumidityType(blockX, blockZ).ordinal();
      return selection[temperature][humidity];
    } else {
      return null;
    }
  }
}
