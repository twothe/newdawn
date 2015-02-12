/*
 */
package two.newdawn.worldgen.vanilla;

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
public class VanillaBaseSelector extends NewDawnBiomeSelector {

  protected final NewDawnBiome[][] selection = new NewDawnBiome[ChunkInformation.Temperature.values().length][ChunkInformation.Humidity.values().length];

  public VanillaBaseSelector(final SimplexNoise worldNoise, final int priority) {
    super(worldNoise, priority);

    this.selection[Temperature.cold.ordinal()][Humidity.sparse.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.icePlains);
    this.selection[Temperature.medium.ordinal()][Humidity.sparse.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.plains);
    this.selection[Temperature.hot.ordinal()][Humidity.sparse.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.desert);

    this.selection[Temperature.cold.ordinal()][Humidity.medium.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.icePlains);
    this.selection[Temperature.medium.ordinal()][Humidity.medium.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.plains);
    this.selection[Temperature.hot.ordinal()][Humidity.medium.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.savanna);

    this.selection[Temperature.cold.ordinal()][Humidity.woodland.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.coldTaiga);
    this.selection[Temperature.medium.ordinal()][Humidity.woodland.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.forest);
    this.selection[Temperature.hot.ordinal()][Humidity.woodland.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.jungleEdge);

    this.selection[Temperature.cold.ordinal()][Humidity.wet.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.megaTaiga);
    this.selection[Temperature.medium.ordinal()][Humidity.wet.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.roofedForest);
    this.selection[Temperature.hot.ordinal()][Humidity.wet.ordinal()] = NewDawnBiome.copyVanilla(BiomeGenBase.jungle);
  }

  @Override
  public NewDawnBiome selectBiome(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    final int temperature = chunkInfo.getTemperatureType(blockX, blockZ).ordinal();
    final int humidity = chunkInfo.getHumidityType(blockX, blockZ).ordinal();
    return selection[temperature][humidity];
  }
}
