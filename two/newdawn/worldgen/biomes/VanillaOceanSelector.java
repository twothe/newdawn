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
public class VanillaOceanSelector extends NewDawnBiomeSelector {

  public VanillaOceanSelector(final SimplexNoise worldNoise, final int priority) {
    super(worldNoise, priority);
  }

  @Override
  public NewDawnBiome selectBiome(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (chunkInfo.isBelowGroundLevel(blockX, blockZ)) {
      if (chunkInfo.isTemperatureFreezing(blockX, blockZ)) {
        return VanillaBiomeProvider.biomeFrozenOcean;
      } else {
        return VanillaBiomeProvider.biomeOcean;
      }
    } else {
      return null;
    }
  }
}
