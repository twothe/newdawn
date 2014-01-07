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
public class VanillaPlainsSelector extends NewDawnBiomeSelector {

  /* temperate */
  protected static final NewDawnBiome biomeGrassPlains = NewDawnBiome.copyVanilla(BiomeGenBase.plains);
  public static final NewDawnBiome biomeGravelPlains = new NewDawnBiome(BiomeGenBase.plains, Block.gravel.blockID, Block.stone.blockID);
  /* cold */
  protected static final NewDawnBiome biomeIcePlains = NewDawnBiome.copyVanilla(BiomeGenBase.icePlains);
  public static final NewDawnBiome biomeFrozenGravelPlains = new NewDawnBiome(BiomeGenBase.icePlains, Block.gravel.blockID, Block.stone.blockID);
  /* hot */
  protected static final NewDawnBiome biomeDesert = NewDawnBiome.copyVanilla(BiomeGenBase.desert);
  protected static final NewDawnBiome biomeMuddyDesert = new NewDawnBiome(BiomeGenBase.desert, Block.hardenedClay.blockID, Block.sand.blockID);

  public VanillaPlainsSelector(final SimplexNoise worldNoise, final int priority) {
    super(worldNoise, priority);
  }

  @Override
  public NewDawnBiome selectBiome(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (chunkInfo.isTemperatureHot(blockX, blockZ)) {
      return getPlainsHot(blockX, blockZ, chunkInfo);
    } else if (chunkInfo.isTemperatureFreezing(blockX, blockZ)) {
      return getPlainsCold(blockX, blockZ, chunkInfo);
    } else {
      return getPlainsMedium(blockX, blockZ, chunkInfo);
    }
  }

  protected NewDawnBiome getPlainsHot(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (!chunkInfo.isTemperatureHot(blockX, blockZ, -0.1f) && chunkInfo.isWoodland(blockX, blockZ, 0.2f)) { // woodland at desert border?
      return biomeMuddyDesert;
    } else {
      return biomeDesert;
    }
  }

  protected NewDawnBiome getPlainsCold(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (chunkInfo.isHumiditySparse(blockX, blockZ) && chunkInfo.isGroundLevelOrShallowWater(blockX, blockZ)) {
      return biomeFrozenGravelPlains;
    } else {
      return biomeIcePlains;
    }
  }

  protected NewDawnBiome getPlainsMedium(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (chunkInfo.isHumiditySparse(blockX, blockZ) && chunkInfo.isGroundLevelOrShallowWater(blockX, blockZ)) {
      return biomeGravelPlains;
    } else {
      return biomeGrassPlains;
    }
  }
}
