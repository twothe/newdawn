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
import net.minecraft.world.gen.MapGenRavine;
import two.newdawn.util.TwoMath;

/**
 *
 * @author Two
 */
public class NewDawnMapGenRavine extends MapGenRavine {

  private float[] field_75046_d = new float[1024];

  /**
   * Copied over from Mojang code for modification
   */
  @Override
  protected void generateRavine(long seed, int chunkX, int chunkZ, byte[] chunkData, double blockX, double blockY, double blockZ, float scale, float leftRightRadian, float upDownRadian, int currentY, int targetY, double scaleHeight) {
    final int worldHeight = chunkData.length / (16 * 16); // 128 or 256
    final Random random = new SecureRandom();
    random.setSeed(seed);

    final double chunkCenterX = (double) (chunkX * 16 + 8);
    final double chunkCenterZ = (double) (chunkZ * 16 + 8);
    float leftRightChange = 0.0F;
    float upDownChange = 0.0F;

    if (targetY <= 0) {
      final int blockRangeY = this.range * 16 - 16;
      targetY = blockRangeY - random.nextInt(blockRangeY / 4);
    }

    boolean createFinalRoom = false;

    if (currentY == -1) {
      currentY = targetY / 2;
      createFinalRoom = true;
    }

    float nextIntersectionHeight = 1.0F;

    for (int k1 = 0; k1 < worldHeight; ++k1) {
      if (k1 == 0 || random.nextInt(3) == 0) {
        nextIntersectionHeight = 1.0F + random.nextFloat() * random.nextFloat() * 1.0F;
      }

      this.field_75046_d[k1] = nextIntersectionHeight * nextIntersectionHeight;
    }

    for (; currentY < targetY; ++currentY) {
      double roomWidth = 1.5D + (double) (MathHelper.sin((float) currentY * (float) Math.PI / (float) targetY) * scale * 1.0F);
      double roomHeight = roomWidth * scaleHeight;
      roomWidth *= (double) random.nextFloat() * 0.25D + 0.75D;
      roomHeight *= (double) random.nextFloat() * 0.25D + 0.75D;
      float f6 = MathHelper.cos(upDownRadian);
      float f7 = MathHelper.sin(upDownRadian);
      blockX += (double) (MathHelper.cos(leftRightRadian) * f6);
      blockY += (double) f7;
      blockZ += (double) (MathHelper.sin(leftRightRadian) * f6);
      upDownRadian *= 0.7F;
      upDownRadian += upDownChange * 0.05F;
      leftRightRadian += leftRightChange * 0.05F;
      upDownChange *= 0.8F;
      leftRightChange *= 0.5F;
      upDownChange += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
      leftRightChange += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

      if (createFinalRoom || random.nextInt(4) != 0) {
        double d8 = blockX - chunkCenterX;
        double d9 = blockZ - chunkCenterZ;
        double d10 = (double) (targetY - currentY);
        double d11 = (double) (scale + 2.0F + 16.0F);

        if (d8 * d8 + d9 * d9 - d10 * d10 > d11 * d11) {
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
              double d12 = ((double) (x + chunkX * 16) + 0.5D - blockX) / roomWidth;

              for (int z = zLow; z < zHigh; ++z) {
                double d13 = ((double) (z + chunkZ * 16) + 0.5D - blockZ) / roomWidth;
                int index = (x * 16 + z) * worldHeight + yHigh;
                boolean flag2 = false;

                if (d12 * d12 + d13 * d13 < 1.0D) {
                  for (int y = yHigh - 1; y >= yLow; --y) {
                    double yScale = ((double) y + 0.5D - blockY) / roomHeight;

                    if ((d12 * d12 + d13 * d13) * (double) this.field_75046_d[y] + yScale * yScale / 6.0D < 1.0D) {
                      if (isTopBlock(chunkData, index, x, y, z, chunkX, chunkZ)) {
                        flag2 = true;
                      }

                      digBlock(chunkData, index, x, y, z, chunkX, chunkZ, flag2);
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
