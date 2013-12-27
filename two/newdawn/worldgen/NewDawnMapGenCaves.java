/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package two.newdawn.worldgen;

import java.security.SecureRandom;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.util.MathHelper;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.MapGenCaves;
import two.newdawn.util.TwoMath;

/**
 *
 * @author Two
 */
public class NewDawnMapGenCaves extends MapGenCaves {

  /**
   * Copied over from Mojang code for modification
   */
  @Override
  protected void generateCaveNode(final long seed, int chunkX, int chunkZ, byte[] chunkData, double blockX, double blockY, double blockZ, float scale, float leftRightRadian, float upDownRadian, int currentY, int targetY, double scaleHeight) {
    final int worldHeight = chunkData.length / (16 * 16); // 128 or 256

    final double chunkCenterX = (double) (chunkX * 16 + 8);
    final double chunkCenterZ = (double) (chunkZ * 16 + 8);
    float leftRightChange = 0.0F;
    float upDownChange = 0.0F;
    final Random random = new SecureRandom();
    random.setSeed(seed);

    if (targetY <= 0) {
      final int blockRangeY = this.range * 16 - 16;
      targetY = blockRangeY - random.nextInt(blockRangeY / 4);
    }

    boolean createFinalRoom = false;

    if (currentY == -1) {
      currentY = targetY / 2;
      createFinalRoom = true;
    }

    final int nextIntersectionHeight = random.nextInt(targetY / 2) + targetY / 4;

    for (final boolean strongVerticalChange = random.nextInt(6) == 0; currentY < targetY; ++currentY) {
      final double roomWidth = 1.5D + (double) (MathHelper.sin((float) currentY * (float) Math.PI / (float) targetY) * scale * 1.0F);
      final double roomHeight = roomWidth * scaleHeight;
      final float blockMoveHorizontal = MathHelper.cos(upDownRadian);
      final float blockMoveVertical = MathHelper.sin(upDownRadian);
      blockX += (double) (MathHelper.cos(leftRightRadian) * blockMoveHorizontal);
      blockY += (double) blockMoveVertical;
      blockZ += (double) (MathHelper.sin(leftRightRadian) * blockMoveHorizontal);

      upDownRadian *= strongVerticalChange ? 0.92F : 0.7F;
      upDownRadian += upDownChange * 0.1F;
      leftRightRadian += leftRightChange * 0.1F;
      upDownChange *= 0.9F;
      leftRightChange *= 0.75F;
      upDownChange += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
      leftRightChange += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

      if (!createFinalRoom && currentY == nextIntersectionHeight && scale > 1.0F && targetY > 0) {
        this.generateCaveNode(random.nextLong(), chunkX, chunkZ, chunkData, blockX, blockY, blockZ, random.nextFloat() * 0.5F + 0.5F, leftRightRadian - ((float) Math.PI / 2F), upDownRadian / 3.0F, currentY, targetY, 1.0D);
        this.generateCaveNode(random.nextLong(), chunkX, chunkZ, chunkData, blockX, blockY, blockZ, random.nextFloat() * 0.5F + 0.5F, leftRightRadian + ((float) Math.PI / 2F), upDownRadian / 3.0F, currentY, targetY, 1.0D);
        return;
      }

      if (createFinalRoom || random.nextInt(4) != 0) {
        final double distanceX = blockX - chunkCenterX;
        final double distanceZ = blockZ - chunkCenterZ;
        final double distanceY = (double) (targetY - currentY);
        final double maxDistance = (double) (scale + 2.0F + 16.0F);

        if (distanceX * distanceX + distanceZ * distanceZ - distanceY * distanceY > maxDistance * maxDistance) {
          return;
        }

        if (blockX >= chunkCenterX - 16.0D - roomWidth * 2.0D && blockZ >= chunkCenterZ - 16.0D - roomWidth * 2.0D && blockX <= chunkCenterX + 16.0D + roomWidth * 2.0D && blockZ <= chunkCenterZ + 16.0D + roomWidth * 2.0D) {
          final int xLow = TwoMath.withinBounds(MathHelper.floor_double(blockX - roomWidth) - chunkX * 16 - 1, 0, 16);
          final int xHigh = TwoMath.withinBounds(MathHelper.floor_double(blockX + roomWidth) - chunkX * 16 + 1, 0, 16);
          final int yLow = TwoMath.withinBounds(MathHelper.floor_double(blockY - roomHeight) - 1, 1, worldHeight - 8);
          final int yHigh = TwoMath.withinBounds(MathHelper.floor_double(blockY + roomHeight) + 1, 1, worldHeight - 8);
          final int zLow = TwoMath.withinBounds(MathHelper.floor_double(blockZ - roomWidth) - chunkZ * 16 - 1, 0, 16);
          final int zHigh = TwoMath.withinBounds(MathHelper.floor_double(blockZ + roomWidth) - chunkZ * 16 + 1, 0, 16);

          boolean underWater = false;

          for (int x = xLow; !underWater && x < xHigh; ++x) {
            for (int z = zLow; !underWater && z < zHigh; ++z) {
              for (int y = yHigh + 1; !underWater && y >= yLow - 1; --y) {
                final int index = (x * 16 + z) * worldHeight + y;

                if (y >= 0 && y < worldHeight) {
                  if (isOceanBlock(chunkData, index, x, y, z, chunkX, chunkZ)) {
                    underWater = true;
                  }

                  if (y != yLow - 1 && x != xLow && x != xHigh - 1 && z != zLow && z != zHigh - 1) {
                    y = yLow;
                  }
                }
              }
            }
          }

          if (!underWater) {
            for (int x = xLow; x < xHigh; ++x) {
              final double xScale = ((double) (x + chunkX * 16) + 0.5D - blockX) / roomWidth;

              for (int z = zLow; z < zHigh; ++z) {
                final double zScale = ((double) (z + chunkZ * 16) + 0.5D - blockZ) / roomWidth;
                int index = (x * 16 + z) * worldHeight + yHigh;
                boolean foundTop = false;

                if (xScale * xScale + zScale * zScale < 1.0D) {
                  for (int y = yHigh - 1; y >= yLow; --y) {
                    final double yScale = ((double) y + 0.5D - blockY) / roomHeight;

                    if (yScale > -0.7D && xScale * xScale + yScale * yScale + zScale * zScale < 1.0D) {
                      if (isTopBlock(chunkData, index, x, y, z, chunkX, chunkZ)) {
                        foundTop = true;
                      }

                      digBlock(chunkData, index, x, y, z, chunkX, chunkZ, foundTop);
                    }

                    --index;
                  }
                }
              }
            }

            if (createFinalRoom) {
              break;
            }
          }
        }
      }
    }
  }

  //----------------------------------------------------------------------------
  //-- Unmodified copy of private function -------------------------------------
  //----------------------------------------------------------------------------
  private boolean isExceptionBiome(BiomeGenBase biome) {
    if (biome == BiomeGenBase.mushroomIsland) {
      return true;
    }
    if (biome == BiomeGenBase.beach) {
      return true;
    }
    if (biome == BiomeGenBase.desert) {
      return true;
    }
    return false;
  }

  private boolean isTopBlock(byte[] data, int index, int x, int y, int z, int chunkX, int chunkZ) {
    BiomeGenBase biome = worldObj.getBiomeGenForCoords(x + chunkX * 16, z + chunkZ * 16);
    return (isExceptionBiome(biome) ? data[index] == Block.grass.blockID : data[index] == biome.topBlock);
  }
}
