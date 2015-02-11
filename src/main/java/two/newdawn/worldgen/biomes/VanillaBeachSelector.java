/*
 */
package two.newdawn.worldgen.biomes;

import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeGenBase;
import two.newdawn.API.ChunkInformation;
import two.newdawn.API.ChunkInformation.Humidity;
import two.newdawn.API.ChunkInformation.Temperature;
import two.newdawn.API.NewDawnBiome;
import two.newdawn.API.NewDawnBiomeSelector;
import two.newdawn.API.noise.SimplexNoise;

/**
 * @author Two
 */
public class VanillaBeachSelector extends NewDawnBiomeSelector {

  protected final NewDawnBiome[][] selection = new NewDawnBiome[ChunkInformation.Temperature.values().length][ChunkInformation.Humidity.values().length];

  public VanillaBeachSelector(final SimplexNoise worldNoise, final int priority) {
    super(worldNoise, priority);

    this.selection[Temperature.cold.ordinal()][Humidity.sparse.ordinal()] = new NewDawnBiome(BiomeGenBase.coldBeach, Blocks.stone, Blocks.stone);
    this.selection[Temperature.medium.ordinal()][Humidity.sparse.ordinal()] = new NewDawnBiome(BiomeGenBase.beach, Blocks.gravel, Blocks.stone);
    this.selection[Temperature.hot.ordinal()][Humidity.sparse.ordinal()] = new NewDawnBiome(BiomeGenBase.beach, Blocks.sand, Blocks.sand);

    this.selection[Temperature.cold.ordinal()][Humidity.medium.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.coldBeach);
    this.selection[Temperature.medium.ordinal()][Humidity.medium.ordinal()] = new NewDawnBiome(BiomeGenBase.beach, Blocks.grass, Blocks.dirt);
    this.selection[Temperature.hot.ordinal()][Humidity.medium.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.savanna);

    this.selection[Temperature.cold.ordinal()][Humidity.woodland.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.coldTaiga);
    this.selection[Temperature.medium.ordinal()][Humidity.woodland.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.forest);
    this.selection[Temperature.hot.ordinal()][Humidity.woodland.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.jungle);

    this.selection[Temperature.cold.ordinal()][Humidity.wet.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.coldTaiga);
    this.selection[Temperature.medium.ordinal()][Humidity.wet.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.swampland);
    this.selection[Temperature.hot.ordinal()][Humidity.wet.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.swampland);
  }

  @Override
  public NewDawnBiome selectBiome(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (chunkInfo.isGroundLevelOrShallowWater(blockX, blockZ)) {
      final int temperature = chunkInfo.getTemperatureType(blockX, blockZ).ordinal();
      final int humidity = chunkInfo.getHumidityType(blockX, blockZ).ordinal();
      return selection[temperature][humidity];
    }
    return null;
  }
}
