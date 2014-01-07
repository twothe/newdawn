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
public class VanillaPlainsSelector extends NewDawnBiomeSelector {

  public VanillaPlainsSelector(final SimplexNoise worldNoise, final int priority) {
    super(worldNoise, priority);
  }

  @Override
  public NewDawnBiome selectBiome(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (chunkInfo.isTemperatureHot(blockX, blockZ)) {
      return getPlainsHot(blockX, blockZ, chunkInfo);
    } else if (chunkInfo.isTemperatureFreezing(blockX, blockZ)) {
      return VanillaBiomeProvider.biomeIcePlains;
    }
    return VanillaBiomeProvider.biomeGrassPlains; // last resort: if nothing else matches, return grass plains
  }

  protected NewDawnBiome getPlainsHot(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (!chunkInfo.isTemperatureHot(blockX, blockZ, -0.1f) && chunkInfo.isWoodland(blockX, blockZ, 0.2f)) { // woodland at desert border?
      return VanillaBiomeProvider.biomeMuddyDesert;
    } else {
      return VanillaBiomeProvider.biomeDesert;
    }
  }
}
