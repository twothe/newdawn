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
public class VanillaMountainSelector extends NewDawnBiomeSelector {

  protected static final NewDawnBiome biomeIceMountains = NewDawnBiome.copyVanilla(BiomeGenBase.iceMountains);
  protected static final NewDawnBiome biomeIceRockyMountains = new NewDawnBiome(BiomeGenBase.iceMountains, Block.stone.blockID, Block.stone.blockID);
  protected static final NewDawnBiome biomeDesertHills = NewDawnBiome.copyVanilla(BiomeGenBase.desertHills);
  protected static final NewDawnBiome biomeDesertRockyMountains = new NewDawnBiome(BiomeGenBase.desertHills, Block.stone.blockID, Block.stone.blockID);
  protected static final NewDawnBiome biomeExtremeHills = NewDawnBiome.copyVanilla(BiomeGenBase.extremeHills);
  protected static final NewDawnBiome biomeRockyExtremeHills = new NewDawnBiome(BiomeGenBase.extremeHills, Block.stone.blockID, Block.stone.blockID);

  public VanillaMountainSelector(final SimplexNoise worldNoise, final int priority) {
    super(worldNoise, priority);
  }

  @Override
  public boolean isApplicable(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    return chunkInfo.isMountain(blockX, blockZ) && chunkInfo.isAboveSeaLevel(blockX, blockZ);
  }

  @Override
  public NewDawnBiome selectBiome(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (chunkInfo.isTemperatureFreezing(blockX, blockZ)) {
      return chunkInfo.isHumiditySparse(blockX, blockZ) ? biomeIceRockyMountains : biomeIceMountains;
    } else if (chunkInfo.isTemperatureHot(blockX, blockZ)) {
      return chunkInfo.isHumiditySparse(blockX, blockZ) ? biomeDesertHills : biomeDesertRockyMountains;
    } else {
      return chunkInfo.isHumiditySparse(blockX, blockZ) ? biomeRockyExtremeHills : biomeExtremeHills;
    }
  }
}
