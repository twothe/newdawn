/*
 */
package two.newdawn.worldgen.biomes;

import two.newdawn.API.ChunkInformation;
import two.newdawn.API.NewDawnBiome;
import two.newdawn.API.NewDawnBiomeSelector;
import two.newdawn.API.noise.SimplexNoise;

/**
 * @author Two
 */
public class BeachSelector extends NewDawnBiomeSelector {

  public BeachSelector(final SimplexNoise worldNoise, final int priority) {
    super(worldNoise, priority);
  }

  @Override
  public NewDawnBiome selectBiome(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (chunkInfo.isGroundLevelOrShallowWater(blockX, blockZ)) {
      if (chunkInfo.isHumiditySparse(blockX, blockZ)) {
        if (chunkInfo.isTemperatureFreezing(blockX, blockZ)) {
          return VanillaBiomeProvider.biomeFrozenGravelBeach;
        } else {
          return VanillaBiomeProvider.biomeGravelBeach;
        }
      } else if (chunkInfo.isTemperatureFreezing(blockX, blockZ)) {
        return VanillaBiomeProvider.biomeFrozenBeach;
      } else if (chunkInfo.isTemperatureHot(blockX, blockZ)) {
        return VanillaBiomeProvider.biomeBeach;
      } else {
        return VanillaBiomeProvider.biomeGrassBeach;
      }
    }
    return null;
  }
}
