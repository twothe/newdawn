/*
 */
package two.newdawn.worldgen.biomes;

import two.newdawn.API.NewDawnBiomeSelector;
import two.newdawn.API.ChunkInformation;
import two.newdawn.API.NewDawnBiome;
import two.newdawn.API.noise.SimplexNoise;

/**
 * @author Two
 */
public class VanillaWoodlandSelector extends NewDawnBiomeSelector {

  public VanillaWoodlandSelector(final SimplexNoise worldNoise, final int priority) {
    super(worldNoise, priority);
  }

  @Override
  public NewDawnBiome selectBiome(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (chunkInfo.isWoodland(blockX, blockZ)) {
      if (isSwampLandApplicable(blockX, blockZ, chunkInfo)) {
        return VanillaBiomeProvider.biomeSwampland;
      } else if (chunkInfo.isAboveSeaLevel(blockX, blockZ)) {
        if ((chunkInfo.getElevation(blockX, blockZ) >= 16) && (chunkInfo.getHeightDifference() >= 4)) {
          return getForestHillsBiome(blockX, blockZ, chunkInfo);
        } else {
          return getForestsBiome(blockX, blockZ, chunkInfo);
        }
      }
    }
    return null;
  }

  protected boolean isSwampLandApplicable(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    return (chunkInfo.isTemperatureMedium(blockX, blockZ) && chunkInfo.isGroundLevelOrShallowWater(blockX, blockZ));
  }

  protected NewDawnBiome getForestsBiome(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (chunkInfo.isTemperatureHot(blockX, blockZ)) {
      return VanillaBiomeProvider.biomeJungle;
    } else if (chunkInfo.isTemperatureFreezing(blockX, blockZ)) {
      return VanillaBiomeProvider.biomeTaiga;
    } else {
      return VanillaBiomeProvider.biomeForest;
    }
  }

  protected NewDawnBiome getForestHillsBiome(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (chunkInfo.isTemperatureHot(blockX, blockZ)) {
      return VanillaBiomeProvider.biomeJungleHills;
    } else if (chunkInfo.isTemperatureFreezing(blockX, blockZ)) {
      return VanillaBiomeProvider.biomeTaigaHills;
    } else {
      return VanillaBiomeProvider.biomeForestHills;
    }
  }
}
