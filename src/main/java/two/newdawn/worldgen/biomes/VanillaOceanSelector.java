/*
 */
package two.newdawn.worldgen.biomes;

import net.minecraft.world.biome.BiomeGenBase;
import two.newdawn.API.ChunkInformation;
import two.newdawn.API.NewDawnBiome;
import two.newdawn.API.NewDawnBiomeSelector;
import two.newdawn.API.noise.SimplexNoise;

/**
 * @author Two
 */
public class VanillaOceanSelector extends NewDawnBiomeSelector {
  public static final NewDawnBiome biomeColdOcean = NewDawnBiome.copyVanilla(BiomeGenBase.frozenOcean);
  public static final NewDawnBiome biomeOcean = NewDawnBiome.copyVanilla(BiomeGenBase.ocean);
  public static final NewDawnBiome biomeDeepOcean = NewDawnBiome.copyVanilla(BiomeGenBase.deepOcean);

  public VanillaOceanSelector(final SimplexNoise worldNoise, final int priority) {
    super(worldNoise, priority);
  }

  @Override
  public NewDawnBiome selectBiome(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (chunkInfo.isBelowGroundLevel(blockX, blockZ)) {
      if (chunkInfo.isTemperatureFreezing(blockX, blockZ)) {
        return biomeColdOcean;
      } else if (chunkInfo.isDeepOcean(blockX, blockZ)) {
        return biomeDeepOcean;
      } else {
        return biomeOcean;
      }
    } else {
      return null;
    }
  }
}
