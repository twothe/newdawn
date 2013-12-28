/*
 */
package two.newdawn.API;

import java.util.Arrays;
import net.minecraft.block.Block;
import net.minecraft.world.biome.BiomeGenBase;
import two.newdawn.API.noise.SimplexNoise;

/**
 * This class allows to generate any kind of biomes which are similar to vanilla
 * biomes, just with different blocks.
 *
 * During world creation each X/Z coordinate is filled from 0 to <i>height</i>
 * using blocks in this order:<br>
 * bedrock &lt; ground block &lt; filler block &lt; top block &lt; air<br>
 * Where the filler block is a random amount between 1 and 4.<br>
 * <br>
 * The benefit of using this intermediate biome class is that you can create any number of
 * biomes based on a vanilla biome without having to register a new biome type,
 * as long as only the blocks are changed. See the VanillaPlainsSelector for
 * a good example.
 *
 * @author Two
 */
public class NewDawnBiome {

  /**
   * Convenience function to properly convert block ids for terrain generation.
   *
   * @param block the block to convert.
   * @return the block's id as byte, properly converted.
   */
  public static byte getBlockID(final Block block) {
    return (byte) (block.blockID & 0xFF);
  }

  /**
   * Convenience function to create a NewDawnBiome as copy of a vanilla biome.
   *
   * @param vanillaBiome the vanilla biome to copy.
   * @return a NewDawnBiome which is an exact copy of a vanilla biome.
   */
  public static NewDawnBiome copyVanilla(final BiomeGenBase vanillaBiome) {
    return new NewDawnBiome(vanillaBiome, vanillaBiome.topBlock, vanillaBiome.fillerBlock, Block.stone.blockID);
  }

  /* The vanilla biome set during world creation */
  public final BiomeGenBase vanillaBiome;
  /* The top block used for this biome during generation */
  public final byte topBlockID;
  /* The filler block used for this biome during generation (between ground and top block) */
  public final byte fillerBlockID;
  /* The ground block used for this biome during generation (below filler block) */
  public final byte groundBlockID;

  /**
   * Convenience constructor that uses stone as ground block.
   *
   * @param vanillaBiome the vanilla biome set during world creation.
   * @param topBlockID the top block used for this biome during generation.
   * @param fillerBlockID the filler block used for this biome during generation (between ground and top block).
   */
  public NewDawnBiome(final BiomeGenBase vanillaBiome, final int topBlockID, final int fillerBlockID) {
    this(vanillaBiome, topBlockID, fillerBlockID, Block.stone.blockID);
  }

  /**
   * Creates a new NewDawnBiome using the given values during creation.
   *
   * @param vanillaBiome the vanilla biome set during world creation.
   * @param topBlockID the top block used for this biome during generation.
   * @param fillerBlockID the filler block used for this biome during generation (between ground and top block).
   * @param groundBlockID the ground block used for this biome during generation (below filler block).
   */
  public NewDawnBiome(final BiomeGenBase vanillaBiome, final int topBlockID, final int fillerBlockID, final int groundBlockID) {
    this.vanillaBiome = vanillaBiome;
    this.topBlockID = (byte) (topBlockID & 0xFF);
    this.fillerBlockID = (byte) (fillerBlockID & 0xFF);
    this.groundBlockID = (byte) (groundBlockID & 0xFF);
  }

  public void fillLocation(final SimplexNoise worldNoise, final byte[] chunkData, int dataPos, final int height, final int fillerHeight, final int blockX, final int blockZ, final NewDawnBiome blockBiome, final ChunkInformation chunkInfo) {
    final int bedrockHeight = fillBedrock(worldNoise, chunkData, dataPos, height, fillerHeight, blockX, blockZ, blockBiome, chunkInfo);
    fillGround(worldNoise, chunkData, dataPos, height, bedrockHeight, fillerHeight, blockX, blockZ, blockBiome, chunkInfo);

    if (chunkInfo.isBelowGroundLevel(blockX, blockZ)) { // this this part of the world below ocean level?
      fillWater(worldNoise, chunkData, dataPos, height, fillerHeight, blockX, blockZ, blockBiome, chunkInfo);
    }
  }

  protected int fillBedrock(final SimplexNoise worldNoise, final byte[] chunkData, int dataPos, final int height, final int fillerHeight, final int blockX, final int blockZ, final NewDawnBiome blockBiome, final ChunkInformation chunkInfo) {
    final byte bedrockID = getBlockID(Block.bedrock);
    final int bedrockHeight = Math.min(height, 5);
    chunkData[dataPos] = bedrockID;
    for (int i = 1; i < bedrockHeight; ++i) {
      chunkData[dataPos + i] = worldNoise.noise(blockX, i, blockZ) <= 0.0 ? bedrockID : blockBiome.groundBlockID; // bedrock mixed with some noise
    }
    return bedrockHeight;
  }

  protected void fillGround(final SimplexNoise worldNoise, final byte[] chunkData, int dataPos, final int height, final int bedrockHeight, final int fillerHeight, final int blockX, final int blockZ, final NewDawnBiome blockBiome, final ChunkInformation chunkInfo) {
    if (fillerHeight > bedrockHeight) {
      Arrays.fill(chunkData, dataPos + bedrockHeight, dataPos + fillerHeight, blockBiome.groundBlockID); // bedrock -> almost at top
      Arrays.fill(chunkData, dataPos + fillerHeight, dataPos + height - 1, blockBiome.fillerBlockID); // almost at top -> top
      if (blockBiome.fillerBlockID == getBlockID(Block.sand)) {
        Arrays.fill(chunkData, dataPos + fillerHeight, dataPos + (height - 1 + fillerHeight) / 2, getBlockID(Block.sandStone)); // ad some sandstone so all the sand won't wall into caves
      }
    } else {
      Arrays.fill(chunkData, dataPos + bedrockHeight, dataPos + height - 1, blockBiome.groundBlockID); // special case: world height is almost at bedrock level
    }
    chunkData[dataPos + height - 1] = blockBiome.topBlockID; // top block by biome and modifiers    
  }

  protected void fillWater(final SimplexNoise worldNoise, final byte[] chunkData, int dataPos, final int height, final int fillerHeight, final int blockX, final int blockZ, final NewDawnBiome blockBiome, final ChunkInformation chunkInfo) {
    if (blockBiome.topBlockID == Block.grass.blockID) {
      chunkData[dataPos + height - 1] = getBlockID(Block.dirt); // fixes underwater grass
    }
    Arrays.fill(chunkData, dataPos + height, dataPos + chunkInfo.baseValues.groundLevel, getBlockID(Block.waterStill));
  }
}
