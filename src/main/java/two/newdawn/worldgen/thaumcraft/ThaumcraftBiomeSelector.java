/*
 */
package two.newdawn.worldgen.thaumcraft;

import java.util.Random;
import two.newdawn.API.ChunkInformation;
import two.newdawn.API.NewDawnBiome;
import two.newdawn.API.NewDawnBiomeSelector;
import two.newdawn.API.noise.NoiseStretch;
import two.newdawn.API.noise.SimplexNoise;

/**
 * @author Two
 */
public class ThaumcraftBiomeSelector extends NewDawnBiomeSelector {

  protected final ThaumcraftConfiguration thaumcraftConfiguration;
  protected final NewDawnBiome biomeTainted;
  protected final NewDawnBiome biomeEvil;
  protected final NewDawnBiome biomeGood;

  protected final NoiseStretch areaNoise;
  protected final NoiseStretch goodEvilNoise;
  protected final NoiseStretch blockNoise;

  public ThaumcraftBiomeSelector(final SimplexNoise worldNoise, final int priority, final ThaumcraftConfiguration thaumcraftConfiguration, final NewDawnBiome biomeTainted, final NewDawnBiome biomeEvil, final NewDawnBiome biomeGood) {
    super(worldNoise, priority);

    this.thaumcraftConfiguration = thaumcraftConfiguration;
    this.biomeTainted = biomeTainted;
    this.biomeEvil = biomeEvil;
    this.biomeGood = biomeGood;

    final Random random = worldNoise.getRandom();
    this.goodEvilNoise = new NoiseStretch(worldNoise, 1230.0, 1190.0, random.nextDouble(), random.nextDouble());
    this.areaNoise = new NoiseStretch(worldNoise, 62.3, 71.6, random.nextDouble(), random.nextDouble());
    this.blockNoise = new NoiseStretch(worldNoise, 8.2, 7.9, random.nextDouble(), random.nextDouble());
  }

  /**
   * Tries to select a biome based on the given parameters and returns it.
   * This is called during generation and can decide whether or not this selector
   * defines the biome at the given location. A return value of null will cause
   * the generation to continue to query selectors, while a non-null return value
   * will define the biome at this location.
   *
   * @param blockX the world-space block x-coordinate.
   * @param blockZ the world-space block z-coordinate.
   * @param chunkInfo information about the chunk this block is located in.
   * @return a valid NewDawnBiome, if this selector is applicable, null otherwise.
   */
  @Override
  public NewDawnBiome selectBiome(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (chunkInfo.isGroundLevelOrShallowWater(blockX, blockZ)) {
      final double goodEvil = this.goodEvilNoise.getNoise(blockX, blockZ);
      if (goodEvil >= thaumcraftConfiguration.thresholdGood) {
        return biomeGood;
      } else {
        final double modifiedGoodEvil = goodEvil * 0.95 + this.areaNoise.getNoise(blockX, blockZ) * 0.025 + this.blockNoise.getNoise(blockX, blockZ) * 0.025;
        if (modifiedGoodEvil <= thaumcraftConfiguration.thresholdTaint) {
          return biomeTainted;
        } else if (modifiedGoodEvil <= thaumcraftConfiguration.thresholdEvil) {
          return biomeEvil;
        }
      }
    }
    return null;
  }
}
