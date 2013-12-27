/*
 */
package two.newdawn.worldgen.biomes;

import net.minecraft.world.biome.BiomeGenBase;
import two.newdawn.API.NewDawnBiomeSelector;
import two.newdawn.API.ChunkInformation;
import two.newdawn.API.NewDawnBiome;
import two.newdawn.API.noise.SimplexNoise;

/**
 * @author Two
 */
public class VanillaWoodlandSelector extends NewDawnBiomeSelector {

  protected static final NewDawnBiome biomeSwampland = NewDawnBiome.copyVanilla(BiomeGenBase.swampland);
  protected static final NewDawnBiome biomeForest = NewDawnBiome.copyVanilla(BiomeGenBase.forest);
  protected static final NewDawnBiome biomeJungle = NewDawnBiome.copyVanilla(BiomeGenBase.jungle);
  protected static final NewDawnBiome biomeTaiga = NewDawnBiome.copyVanilla(BiomeGenBase.taiga);
  protected static final NewDawnBiome biomeForestHills = NewDawnBiome.copyVanilla(BiomeGenBase.forestHills);
  protected static final NewDawnBiome biomeJungleHills = NewDawnBiome.copyVanilla(BiomeGenBase.jungleHills);
  protected static final NewDawnBiome biomeTaigaHills = NewDawnBiome.copyVanilla(BiomeGenBase.taigaHills);

  public VanillaWoodlandSelector(final SimplexNoise worldNoise, final int priority) {
    super(worldNoise, priority);
  }

  @Override
  public boolean isApplicable(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    return chunkInfo.isWoodland(blockX, blockZ) && (chunkInfo.isAboveSeaLevel(blockX, blockZ) || isSwampLandApplicable(blockX, blockZ, chunkInfo));
  }

  @Override
  public NewDawnBiome selectBiome(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (isSwampLandApplicable(blockX, blockZ, chunkInfo)) {
      return biomeSwampland;
    } else if (chunkInfo.getElevation(blockX, blockZ) >= 16) {
      return getForestHillsBiome(blockX, blockZ, chunkInfo);
    } else {
      return getForestsBiome(blockX, blockZ, chunkInfo);
    }
  }

  protected boolean isSwampLandApplicable(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    return (chunkInfo.isTemperatureMedium(blockX, blockZ) && chunkInfo.isGroundLevelOrShallowWater(blockX, blockZ));
  }

  protected NewDawnBiome getForestsBiome(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (chunkInfo.isTemperatureHot(blockX, blockZ)) {
      return biomeJungle;
    } else if (chunkInfo.isTemperatureFreezing(blockX, blockZ)) {
      return biomeTaiga;
    } else {
      return biomeForest;
    }
  }

  protected NewDawnBiome getForestHillsBiome(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (chunkInfo.isTemperatureHot(blockX, blockZ)) {
      return biomeJungleHills;
    } else if (chunkInfo.isTemperatureFreezing(blockX, blockZ)) {
      return biomeTaigaHills;
    } else {
      return biomeForestHills;
    }
  }
}
