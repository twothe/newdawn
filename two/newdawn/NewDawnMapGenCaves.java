/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package two.newdawn;

import java.security.SecureRandom;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.util.MathHelper;
import net.minecraft.world.gen.MapGenCaves;

/**
 *
 * @author Two
 */
public class NewDawnMapGenCaves extends MapGenCaves {

  /**
   * Copied over from Mojang code for modification
   */
  @Override
  protected void generateCaveNode(long seed, int x, int z, byte[] chunkData, double par6, double par8, double par10, float par12, float par13, float par14, int par15, int par16, double par17) {
    final int worldHeight = chunkData.length / 256;
    double var19 = (double) (x * 16 + 8);
    double var21 = (double) (z * 16 + 8);
    float var23 = 0.0F;
    float var24 = 0.0F;
    final Random random = new SecureRandom();
    random.setSeed(seed);

    if (par16 <= 0) {
      int var26 = this.range * 16 - 16;
      par16 = var26 - random.nextInt(var26 / 4);
    }

    boolean var54 = false;

    if (par15 == -1) {
      par15 = par16 / 2;
      var54 = true;
    }

    int var27 = random.nextInt(par16 / 2) + par16 / 4;

    for (boolean var28 = random.nextInt(6) == 0; par15 < par16; ++par15) {
      double var29 = 1.5D + (double) (MathHelper.sin((float) par15 * (float) Math.PI / (float) par16) * par12 * 1.0F);
      double var31 = var29 * par17;
      float var33 = MathHelper.cos(par14);
      float var34 = MathHelper.sin(par14);
      par6 += (double) (MathHelper.cos(par13) * var33);
      par8 += (double) var34;
      par10 += (double) (MathHelper.sin(par13) * var33);

      if (var28) {
        par14 *= 0.92F;
      } else {
        par14 *= 0.7F;
      }

      par14 += var24 * 0.1F;
      par13 += var23 * 0.1F;
      var24 *= 0.9F;
      var23 *= 0.75F;
      var24 += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
      var23 += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

      if (!var54 && par15 == var27 && par12 > 1.0F && par16 > 0) {
        this.generateCaveNode(random.nextLong(), x, z, chunkData, par6, par8, par10, random.nextFloat() * 0.5F + 0.5F, par13 - ((float) Math.PI / 2F), par14 / 3.0F, par15, par16, 1.0D);
        this.generateCaveNode(random.nextLong(), x, z, chunkData, par6, par8, par10, random.nextFloat() * 0.5F + 0.5F, par13 + ((float) Math.PI / 2F), par14 / 3.0F, par15, par16, 1.0D);
        return;
      }

      if (var54 || random.nextInt(4) != 0) {
        double var35 = par6 - var19;
        double var37 = par10 - var21;
        double var39 = (double) (par16 - par15);
        double var41 = (double) (par12 + 2.0F + 16.0F);

        if (var35 * var35 + var37 * var37 - var39 * var39 > var41 * var41) {
          return;
        }

        if (par6 >= var19 - 16.0D - var29 * 2.0D && par10 >= var21 - 16.0D - var29 * 2.0D && par6 <= var19 + 16.0D + var29 * 2.0D && par10 <= var21 + 16.0D + var29 * 2.0D) {
          int var55 = MathHelper.floor_double(par6 - var29) - x * 16 - 1;
          int var36 = MathHelper.floor_double(par6 + var29) - x * 16 + 1;
          int var57 = MathHelper.floor_double(par8 - var31) - 1;
          int var38 = MathHelper.floor_double(par8 + var31) + 1;
          int var56 = MathHelper.floor_double(par10 - var29) - z * 16 - 1;
          int var40 = MathHelper.floor_double(par10 + var29) - z * 16 + 1;

          if (var55 < 0) {
            var55 = 0;
          }

          if (var36 > 16) {
            var36 = 16;
          }

          if (var57 < 1) {
            var57 = 1;
          }

          if (var38 > 120) {
            var38 = 120;
          }

          if (var56 < 0) {
            var56 = 0;
          }

          if (var40 > 16) {
            var40 = 16;
          }

          boolean generatedWater = false;
          int blockX;
          int dataPos;

          for (blockX = var55; !generatedWater && blockX < var36; ++blockX) {
            for (int blockZ = var56; !generatedWater && blockZ < var40; ++blockZ) {
              for (int blockY = var38 + 1; !generatedWater && blockY >= var57 - 1; --blockY) {
                dataPos = (blockX * 16 + blockZ) * worldHeight + blockY;

                if (blockY >= 0 && blockY < worldHeight) {
                  if (chunkData[dataPos] == Block.waterMoving.blockID || chunkData[dataPos] == Block.waterStill.blockID) {
                    generatedWater = true;
                  }

                  if (blockY != var57 - 1 && blockX != var55 && blockX != var36 - 1 && blockZ != var56 && blockZ != var40 - 1) {
                    blockY = var57;
                  }
                }
              }
            }
          }

          if (!generatedWater) {
            for (blockX = var55; blockX < var36; ++blockX) {
              double var59 = ((double) (blockX + x * 16) + 0.5D - par6) / var29;

              for (dataPos = var56; dataPos < var40; ++dataPos) {
                double var46 = ((double) (dataPos + z * 16) + 0.5D - par10) / var29;
                int dataPos2 = (blockX * 16 + dataPos) * worldHeight + var38;
                boolean hasGrassTop = false;

                if (var59 * var59 + var46 * var46 < 1.0D) {
                  for (int var50 = var38 - 1; var50 >= var57; --var50) {
                    double var51 = ((double) var50 + 0.5D - par8) / var31;

                    if (var51 > -0.7D && var59 * var59 + var51 * var51 + var46 * var46 < 1.0D) {
                      byte var53 = chunkData[dataPos2];

                      if (var53 == Block.grass.blockID) {
                        hasGrassTop = true;
                      }

                      if (var53 == Block.stone.blockID || var53 == Block.dirt.blockID || var53 == Block.grass.blockID) {
                        if (var50 < 10) {
                          chunkData[dataPos2] = (byte) Block.lavaMoving.blockID;
                        } else {
                          chunkData[dataPos2] = 0;

//                          if (hasGrassTop && chunkData[dataPos2 - 1] == Block.dirt.blockID) {
//                            chunkData[dataPos2 - 1] = this.worldObj.getBiomeGenForCoords(blockX + x * 16, dataPos + z * 16).topBlock;
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
