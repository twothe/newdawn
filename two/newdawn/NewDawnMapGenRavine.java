/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package two.newdawn;

import java.security.SecureRandom;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.util.MathHelper;
import net.minecraft.world.gen.MapGenRavine;

/**
 *
 * @author Two
 */
public class NewDawnMapGenRavine extends MapGenRavine {

  private float[] field_75046_d = new float[1024];

  /**
   * Copied over from Mojang code for modification
   */
  protected void generateRavine(long seed, int chunkX, int chunkZ, byte[] chunkData, double par6, double par8, double par10, float par12, float par13, float par14, int par15, int par16, double par17) {
    final int worldHeight = chunkData.length / 256;
    final Random random = new SecureRandom();
    random.setSeed(seed);
    double var20 = (double) (chunkX * 16 + 8);
    double var22 = (double) (chunkZ * 16 + 8);
    float var24 = 0.0F;
    float var25 = 0.0F;

    if (par16 <= 0) {
      int var26 = this.range * 16 - 16;
      par16 = var26 - random.nextInt(var26 / 4);
    }

    boolean var54 = false;

    if (par15 == -1) {
      par15 = par16 / 2;
      var54 = true;
    }

    float var27 = 1.0F;

    for (int var28 = 0; var28 < 128; ++var28) {
      if (var28 == 0 || random.nextInt(3) == 0) {
        var27 = 1.0F + random.nextFloat() * random.nextFloat() * 1.0F;
      }

      this.field_75046_d[var28] = var27 * var27;
    }

    for (; par15 < par16; ++par15) {
      double var53 = 1.5D + (double) (MathHelper.sin((float) par15 * (float) Math.PI / (float) par16) * par12 * 1.0F);
      double var30 = var53 * par17;
      var53 *= (double) random.nextFloat() * 0.25D + 0.75D;
      var30 *= (double) random.nextFloat() * 0.25D + 0.75D;
      float var32 = MathHelper.cos(par14);
      float var33 = MathHelper.sin(par14);
      par6 += (double) (MathHelper.cos(par13) * var32);
      par8 += (double) var33;
      par10 += (double) (MathHelper.sin(par13) * var32);
      par14 *= 0.7F;
      par14 += var25 * 0.05F;
      par13 += var24 * 0.05F;
      var25 *= 0.8F;
      var24 *= 0.5F;
      var25 += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
      var24 += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

      if (var54 || random.nextInt(4) != 0) {
        double var34 = par6 - var20;
        double var36 = par10 - var22;
        double var38 = (double) (par16 - par15);
        double var40 = (double) (par12 + 2.0F + 16.0F);

        if (var34 * var34 + var36 * var36 - var38 * var38 > var40 * var40) {
          return;
        }

        if (par6 >= var20 - 16.0D - var53 * 2.0D && par10 >= var22 - 16.0D - var53 * 2.0D && par6 <= var20 + 16.0D + var53 * 2.0D && par10 <= var22 + 16.0D + var53 * 2.0D) {
          int var56 = MathHelper.floor_double(par6 - var53) - chunkX * 16 - 1;
          int var35 = MathHelper.floor_double(par6 + var53) - chunkX * 16 + 1;
          int var55 = MathHelper.floor_double(par8 - var30) - 1;
          int var37 = MathHelper.floor_double(par8 + var30) + 1;
          int var57 = MathHelper.floor_double(par10 - var53) - chunkZ * 16 - 1;
          int var39 = MathHelper.floor_double(par10 + var53) - chunkZ * 16 + 1;

          if (var56 < 0) {
            var56 = 0;
          }

          if (var35 > 16) {
            var35 = 16;
          }

          if (var55 < 1) {
            var55 = 1;
          }

          if (var37 > 120) {
            var37 = 120;
          }

          if (var57 < 0) {
            var57 = 0;
          }

          if (var39 > 16) {
            var39 = 16;
          }

          boolean var58 = false;
          int x;
          int dataPos;

          for (x = var56; !var58 && x < var35; ++x) {
            for (int z = var57; !var58 && z < var39; ++z) {
              for (int y = var37 + 1; !var58 && y >= var55 - 1; --y) {
                dataPos = (x * 16 + z) * worldHeight + y;

                if (y >= 0 && y < worldHeight) {
                  if (chunkData[dataPos] == Block.waterMoving.blockID || chunkData[dataPos] == Block.waterStill.blockID) {
                    var58 = true;
                  }

                  if (y != var55 - 1 && x != var56 && x != var35 - 1 && z != var57 && z != var39 - 1) {
                    y = var55;
                  }
                }
              }
            }
          }

          if (!var58) {
            for (x = var56; x < var35; ++x) {
              double var59 = ((double) (x + chunkX * 16) + 0.5D - par6) / var53;

              for (dataPos = var57; dataPos < var39; ++dataPos) {
                double var45 = ((double) (dataPos + chunkZ * 16) + 0.5D - par10) / var53;
                int dataPos2 = (x * 16 + dataPos) * worldHeight + var37;
                boolean hasGrassTop = false;

                if (var59 * var59 + var45 * var45 < 1.0D) {
                  for (int var49 = var37 - 1; var49 >= var55; --var49) {
                    double var50 = ((double) var49 + 0.5D - par8) / var30;

                    if ((var59 * var59 + var45 * var45) * (double) this.field_75046_d[var49] + var50 * var50 / 6.0D < 1.0D) {
                      byte blockID = chunkData[dataPos2];

                      if (blockID == Block.grass.blockID) {
                        hasGrassTop = true;
                      }

                      if (blockID == Block.stone.blockID || blockID == Block.dirt.blockID || blockID == Block.grass.blockID) {
                        if (var49 < 10) {
                          chunkData[dataPos2] = (byte) Block.lavaMoving.blockID;
                        } else {
                          chunkData[dataPos2] = 0;
//
//                          if (hasGrassTop && chunkData[dataPos2 - 1] == Block.dirt.blockID) {
//                            chunkData[dataPos2 - 1] = this.worldObj.getBiomeGenForCoords(x + chunkX * 16, dataPos + chunkZ * 16).topBlock;
//                          }
                        }
                      }
                    }

                    --dataPos2;
                  }
                }
              }
            }

            if (var54) {
              break;
            }
          }
        }
      }
    }
  }
}
