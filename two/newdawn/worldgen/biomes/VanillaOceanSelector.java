/*
 */
package two.newdawn.worldgen.biomes;

import net.minecraft.block.Block;
import net.minecraft.world.biome.BiomeGenBase;
import two.newdawn.API.NewDawnBiomeSelector;
import two.newdawn.API.ChunkInformation;
import two.newdawn.API.NewDawnBiome;
import two.newdawn.API.noise.SimplexNoise;

/**
 * @author Two
 */
public class VanillaOceanSelector extends NewDawnBiomeSelector {

  protected static final NewDawnBiome biomeFrozenOcean = NewDawnBiome.copyVanilla(BiomeGenBase.frozenOcean);
  protected static final NewDawnBiome biomeBeach = NewDawnBiome.copyVanilla(BiomeGenBase.beach);
  protected static final NewDawnBiome biomeOcean = new NewDawnBiome(BiomeGenBase.ocean, Block.sand.blockID, Block.sand.blockID);

  public VanillaOceanSelector(final SimplexNoise worldNoise, final int priority) {
    super(worldNoise, priority);
  }

  @Override
  public NewDawnBiome selectBiome(int blockX, int blockZ, ChunkInformation chunkInfo) {
    if (chunkInfo.isBelowGroundLevel(blockX, blockZ)) {
      if (chunkInfo.isShallowWater(blockX, blockZ)) {
        if (chunkInfo.isHumiditySparse(blockX, blockZ)) {
          if (chunkInfo.isTemperatureFreezing(blockX, blockZ)) {
            return VanillaPlainsSelector.biomeFrozenGravelPlains;
          } else {
            return VanillaPlainsSelector.biomeGravelPlains;
          }
        } else {
          return biomeBeach;
        }
      } else if (chunkInfo.isTemperatureFreezing(blockX, blockZ)) {
        return biomeFrozenOcean;
      } else {
        return biomeOcean;
      }
    } else {
      return null;
    }
  }
}
