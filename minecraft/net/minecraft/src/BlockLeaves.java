package net.minecraft.src;

import java.io.*;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.client.Minecraft;

public class BlockLeaves extends BlockLeavesBase {

  private static final Logger logger = Logger.getLogger("Minecraft");
  /**
   * Properties
   */
  public static final String FILE_GROWTH_PROPERTIES = "growth.properties";
  public static final String GROWTH_PROPERTIES_DEBUG = "debug";
  public static final String GROWTH_PROPERTIES_MINIMUM_DISTANCE = "minimumDistance";
  public static final String GROWTH_PROPERTIES_DROP_RANGE = "dropRange";
  public static final String GROWTH_PROPERTIES_DROP_CHANCE = "dropChance";
  public static final String GROWTH_PROPERTIES_COMMENT = ""
          + "############################\n"
          + " Settings for growth mod.\n"
          + "#############################\n"
          + " debug             if set to \"true\" leaves will play sounds to indicate what they are doing (click = tried to spawn but failed, ding = spawned new sapling).\n"
          + " dropRange         the maximum range to a leaves block that a sapling can be created.\n"
          + " minimumDistance   a sapling can only be dropped if it is this far away from other saplings and wood blocks.\n"
          + " dropChance        chance as 1:x for a leaves block to try to spawn a sapling. Good values are:\n"
          + "     100000  very slow\n"
          + "      10000  slow\n"
          + "       1000  normal\n"
          + "        100  fast\n"
          + "         10  very fast\n"
          + "          1  OMG the trees are attacking!\n";
  /**
   * Static
   */
  private final int SAPLING_MINIMUM_DISTANCE_TO_OTHER;
  private final int SAPLING_DROP_RANGE;
  private final int SAPLING_RANDOM_RANGE;
  private final int SAPLING_DROP_CHANCE;
  private final boolean SAPLING_DEBUG;
  private int tickCounterForSapling;
  /**
   * The base index in terrain.png corresponding to the fancy version of the
   * leaf texture. This is stored so we can switch the displayed version between
   * fancy and fast graphics (fast is this index + 1).
   */
  private int baseIndexInPNG;
  int adjacentTreeBlocks[];

  private static int strToInt(final String s, final int defaultValue, final int min, final int max) {
    if ((s == null) || (s.length() == 0)) {
      return defaultValue;
    }
    try {
      return Math.min(Math.max(Integer.parseInt(s), min), max);
    } catch (NumberFormatException e) {
      logger.log(Level.WARNING, "{0} is not a valid integer, using {1} instead.", new Object[]{s, defaultValue});
    } catch (Exception e) {
    }
    return defaultValue;
  }

  protected BlockLeaves(int par1, int par2) {
    super(par1, par2, Material.leaves, false);
    baseIndexInPNG = par2;
    setTickRandomly(true);

    final File mcDir = Minecraft.getMinecraftDir();
    final File propertyFile = new File(mcDir, FILE_GROWTH_PROPERTIES);
    final Properties growthSettings = new Properties();
    try {
      final FileInputStream in = new FileInputStream(propertyFile);
      try {
        growthSettings.load(in);
      } finally {
        in.close();
      }
    } catch (FileNotFoundException e) {
    } catch (IOException e) {
      logger.log(Level.WARNING, "Unable to load growth settings from file \"" + FILE_GROWTH_PROPERTIES + "\": {0}", e.getMessage());
    }
    SAPLING_DEBUG = "true".equalsIgnoreCase(growthSettings.getProperty(GROWTH_PROPERTIES_DEBUG));
    SAPLING_MINIMUM_DISTANCE_TO_OTHER = strToInt(growthSettings.getProperty(GROWTH_PROPERTIES_MINIMUM_DISTANCE), 3, 0, 100);
    SAPLING_DROP_RANGE = strToInt(growthSettings.getProperty(GROWTH_PROPERTIES_DROP_RANGE), 5, 1, 100);
    SAPLING_DROP_CHANCE = strToInt(growthSettings.getProperty(GROWTH_PROPERTIES_DROP_CHANCE), 1000, 1, Integer.MAX_VALUE);
    SAPLING_RANDOM_RANGE = 2 * SAPLING_DROP_RANGE + 1;
    tickCounterForSapling = SAPLING_DROP_CHANCE;

    growthSettings.setProperty(GROWTH_PROPERTIES_DEBUG, Boolean.toString(SAPLING_DEBUG));
    growthSettings.setProperty(GROWTH_PROPERTIES_MINIMUM_DISTANCE, Integer.toString(SAPLING_MINIMUM_DISTANCE_TO_OTHER));
    growthSettings.setProperty(GROWTH_PROPERTIES_DROP_RANGE, Integer.toString(SAPLING_DROP_RANGE));
    growthSettings.setProperty(GROWTH_PROPERTIES_DROP_CHANCE, Integer.toString(SAPLING_DROP_CHANCE));

    try {
      final FileOutputStream out = new FileOutputStream(propertyFile);
      try {
        growthSettings.store(out, GROWTH_PROPERTIES_COMMENT);
      } finally {
        out.close();
      }
    } catch (IOException e) {
      logger.log(Level.WARNING, "Unable to save growth settings to file \"" + FILE_GROWTH_PROPERTIES + "\": {0}", e.getMessage());
    }
  }

  @Override
  public int getBlockColor() {
    double d = 0.5D;
    double d1 = 1.0D;
    return ColorizerFoliage.getFoliageColor(d, d1);
  }

  /**
   * Returns the color this block should be rendered. Used by leaves.
   */
  @Override
  public int getRenderColor(int par1) {
    if ((par1 & 3) == 1) {
      return ColorizerFoliage.getFoliageColorPine();
    }

    if ((par1 & 3) == 2) {
      return ColorizerFoliage.getFoliageColorBirch();
    } else {
      return ColorizerFoliage.getFoliageColorBasic();
    }
  }

  /**
   * Returns a integer with hex for 0xrrggbb with this color multiplied against
   * the blocks color. Note only called when first determining what to render.
   */
  @Override
  public int colorMultiplier(IBlockAccess par1IBlockAccess, int par2, int par3, int par4) {
    int i = par1IBlockAccess.getBlockMetadata(par2, par3, par4);

    if ((i & 3) == 1) {
      return ColorizerFoliage.getFoliageColorPine();
    }

    if ((i & 3) == 2) {
      return ColorizerFoliage.getFoliageColorBirch();
    }

    int j = 0;
    int k = 0;
    int l = 0;

    for (int i1 = -1; i1 <= 1; i1++) {
      for (int j1 = -1; j1 <= 1; j1++) {
        int k1 = par1IBlockAccess.getBiomeGenForCoords(par2 + j1, par4 + i1).getBiomeFoliageColor();
        j += (k1 & 0xff0000) >> 16;
        k += (k1 & 0xff00) >> 8;
        l += k1 & 0xff;
      }
    }

    return (j / 9 & 0xff) << 16 | (k / 9 & 0xff) << 8 | l / 9 & 0xff;
  }

  /**
   * Called whenever the block is removed.
   */
  @Override
  public void onBlockRemoval(World par1World, int par2, int par3, int par4) {
    int i = 1;
    int j = i + 1;

    if (par1World.checkChunksExist(par2 - j, par3 - j, par4 - j, par2 + j, par3 + j, par4 + j)) {
      for (int k = -i; k <= i; k++) {
        for (int l = -i; l <= i; l++) {
          for (int i1 = -i; i1 <= i; i1++) {
            int j1 = par1World.getBlockId(par2 + k, par3 + l, par4 + i1);

            if (j1 == Block.leaves.blockID) {
              int k1 = par1World.getBlockMetadata(par2 + k, par3 + l, par4 + i1);
              par1World.setBlockMetadata(par2 + k, par3 + l, par4 + i1, k1 | 8);
            }
          }
        }
      }
    }
  }

  /**
   * Ticks the block if it's been scheduled
   */
  @Override
  public void updateTick(World world, int x, int y, int z, Random random) {
    if (world.isRemote) {
      return;
    }

    final int metaData = world.getBlockMetadata(x, y, z);

    if ((metaData & 8) != 0 && (metaData & 4) == 0) {
      byte byte0 = 4;
      int j = byte0 + 1;
      byte byte1 = 32;
      int k = byte1 * byte1;
      int l = byte1 / 2;

      if (adjacentTreeBlocks == null) {
        adjacentTreeBlocks = new int[byte1 * byte1 * byte1];
      }

      if (world.checkChunksExist(x - j, y - j, z - j, x + j, y + j, z + j)) {
        for (int i1 = -byte0; i1 <= byte0; i1++) {
          for (int l1 = -byte0; l1 <= byte0; l1++) {
            for (int j2 = -byte0; j2 <= byte0; j2++) {
              int l2 = world.getBlockId(x + i1, y + l1, z + j2);

              if (l2 == Block.wood.blockID) {
                adjacentTreeBlocks[(i1 + l) * k + (l1 + l) * byte1 + (j2 + l)] = 0;
                continue;
              }

              if (l2 == Block.leaves.blockID) {
                adjacentTreeBlocks[(i1 + l) * k + (l1 + l) * byte1 + (j2 + l)] = -2;
              } else {
                adjacentTreeBlocks[(i1 + l) * k + (l1 + l) * byte1 + (j2 + l)] = -1;
              }
            }
          }
        }

        for (int j1 = 1; j1 <= 4; j1++) {
          for (int i2 = -byte0; i2 <= byte0; i2++) {
            for (int k2 = -byte0; k2 <= byte0; k2++) {
              for (int i3 = -byte0; i3 <= byte0; i3++) {
                if (adjacentTreeBlocks[(i2 + l) * k + (k2 + l) * byte1 + (i3 + l)] != j1 - 1) {
                  continue;
                }

                if (adjacentTreeBlocks[((i2 + l) - 1) * k + (k2 + l) * byte1 + (i3 + l)] == -2) {
                  adjacentTreeBlocks[((i2 + l) - 1) * k + (k2 + l) * byte1 + (i3 + l)] = j1;
                }

                if (adjacentTreeBlocks[(i2 + l + 1) * k + (k2 + l) * byte1 + (i3 + l)] == -2) {
                  adjacentTreeBlocks[(i2 + l + 1) * k + (k2 + l) * byte1 + (i3 + l)] = j1;
                }

                if (adjacentTreeBlocks[(i2 + l) * k + ((k2 + l) - 1) * byte1 + (i3 + l)] == -2) {
                  adjacentTreeBlocks[(i2 + l) * k + ((k2 + l) - 1) * byte1 + (i3 + l)] = j1;
                }

                if (adjacentTreeBlocks[(i2 + l) * k + (k2 + l + 1) * byte1 + (i3 + l)] == -2) {
                  adjacentTreeBlocks[(i2 + l) * k + (k2 + l + 1) * byte1 + (i3 + l)] = j1;
                }

                if (adjacentTreeBlocks[(i2 + l) * k + (k2 + l) * byte1 + ((i3 + l) - 1)] == -2) {
                  adjacentTreeBlocks[(i2 + l) * k + (k2 + l) * byte1 + ((i3 + l) - 1)] = j1;
                }

                if (adjacentTreeBlocks[(i2 + l) * k + (k2 + l) * byte1 + (i3 + l + 1)] == -2) {
                  adjacentTreeBlocks[(i2 + l) * k + (k2 + l) * byte1 + (i3 + l + 1)] = j1;
                }
              }
            }
          }
        }
      }

      int k1 = adjacentTreeBlocks[l * k + l * byte1 + l];

      if (k1 >= 0) {
        world.setBlockMetadata(x, y, z, metaData & -9);
      } else {
        removeLeaves(world, x, y, z);
      }
    } else {
      Profiler.startSection("grow sapling");
      --tickCounterForSapling;
      if (tickCounterForSapling == 0) {
        tickCounterForSapling = SAPLING_DROP_CHANCE;
        tryDropSapling(world, x, y, z, random);
      }
      Profiler.endSection();
    }
  }

  private void removeLeaves(World par1World, int par2, int par3, int par4) {
    dropBlockAsItem(par1World, par2, par3, par4, par1World.getBlockMetadata(par2, par3, par4), 0);
    par1World.setBlockWithNotify(par2, par3, par4, 0);
  }

  /**
   * Returns the quantity of items to drop on block destruction.
   */
  @Override
  public int quantityDropped(Random par1Random) {
    return par1Random.nextInt(20) != 0 ? 0 : 1;
  }

  /**
   * Returns the ID of the items to drop on destruction.
   */
  @Override
  public int idDropped(int par1, Random par2Random, int par3) {
    return Block.sapling.blockID;
  }

  /**
   * Drops the block items with a specified chance of dropping the specified
   * items
   */
  @Override
  public void dropBlockAsItemWithChance(World par1World, int par2, int par3, int par4, int par5, float par6, int par7) {
    if (!par1World.isRemote) {
      byte byte0 = 20;

      if ((par5 & 3) == 3) {
        byte0 = 40;
      }

      if (par1World.rand.nextInt(byte0) == 0) {
        int i = idDropped(par5, par1World.rand, par7);
        dropBlockAsItem_do(par1World, par2, par3, par4, new ItemStack(i, 1, damageDropped(par5)));
      }

      if ((par5 & 3) == 0 && par1World.rand.nextInt(200) == 0) {
        dropBlockAsItem_do(par1World, par2, par3, par4, new ItemStack(Item.appleRed, 1, 0));
      }
    }
  }

  /**
   * Called when the player destroys a block with an item that can harvest it.
   * (i, j, k) are the coordinates of the block and l is the block's
   * subtype/damage.
   */
  @Override
  public void harvestBlock(World par1World, EntityPlayer par2EntityPlayer, int par3, int par4, int par5, int par6) {
    if (!par1World.isRemote && par2EntityPlayer.getCurrentEquippedItem() != null && par2EntityPlayer.getCurrentEquippedItem().itemID == Item.shears.shiftedIndex) {
      par2EntityPlayer.addStat(StatList.mineBlockStatArray[blockID], 1);
      dropBlockAsItem_do(par1World, par3, par4, par5, new ItemStack(Block.leaves.blockID, 1, par6 & 3));
    } else {
      super.harvestBlock(par1World, par2EntityPlayer, par3, par4, par5, par6);
    }
  }

  /**
   * Determines the damage on the item the block drops. Used in cloth and wood.
   */
  @Override
  protected int damageDropped(int par1) {
    return par1 & 3;
  }

  /**
   * Is this block (a) opaque and (b) a full 1m cube? This determines whether or
   * not to render the shared face of two adjacent blocks and also whether the
   * player can attach torches, redstone wire, etc to this block.
   */
  @Override
  public boolean isOpaqueCube() {
    return !graphicsLevel;
  }

  /**
   * From the specified side and block metadata retrieves the blocks texture.
   * Args: side, metadata
   */
  @Override
  public int getBlockTextureFromSideAndMetadata(int par1, int par2) {
    if ((par2 & 3) == 1) {
      return blockIndexInTexture + 80;
    }

    if ((par2 & 3) == 3) {
      return blockIndexInTexture + 144;
    } else {
      return blockIndexInTexture;
    }
  }

  /**
   * Pass true to draw this block using fancy graphics, or false for fast
   * graphics.
   */
  public void setGraphicsLevel(boolean par1) {
    graphicsLevel = par1;
    blockIndexInTexture = baseIndexInPNG + (par1 ? 0 : 1);
  }

  /**
   * Called whenever an entity is walking on top of this block. Args: world, x,
   * y, z, entity
   */
  @Override
  public void onEntityWalking(World par1World, int par2, int par3, int par4, Entity par5Entity) {
    super.onEntityWalking(par1World, par2, par3, par4, par5Entity);
  }

  public boolean tryDropSapling(World world, int x, int y, int z, Random random) {
    final int spawnX = x + random.nextInt(SAPLING_RANDOM_RANGE) - SAPLING_DROP_RANGE;
    final int spawnZ = z + random.nextInt(SAPLING_RANDOM_RANGE) - SAPLING_DROP_RANGE;
    int spawnY = y + 5;
    if (world.checkChunksExist(spawnX - SAPLING_MINIMUM_DISTANCE_TO_OTHER, spawnY, spawnZ - SAPLING_MINIMUM_DISTANCE_TO_OTHER, spawnX + SAPLING_MINIMUM_DISTANCE_TO_OTHER, spawnY, spawnZ + SAPLING_MINIMUM_DISTANCE_TO_OTHER)) {
      while (!Block.sapling.canPlaceBlockAt(world, spawnX, spawnY, spawnZ)) {
        if (world.getBlockId(spawnX, spawnY, spawnZ) != 0) {
          if (SAPLING_DEBUG) {
            makeDebugSound(world, spawnX, spawnY, spawnZ, false);
          }
          return false;
        }
        --spawnY;
      }

      if (canSpawnSapling(world, spawnX, spawnY, spawnZ)) {
        world.setBlockAndMetadataWithNotify(spawnX, spawnY, spawnZ, Block.sapling.blockID, world.getBlockMetadata(x, y, z) & 3);
        if (SAPLING_DEBUG) {
          makeDebugSound(world, spawnX, spawnY, spawnZ, true);
        }
        return true;
      }
    }
    if (SAPLING_DEBUG) {
      makeDebugSound(world, spawnX, spawnY, spawnZ, false);
    }
    return false;
  }

  private void makeDebugSound(World world, int x, int y, int z, boolean success) {
    if (success) {
      world.playSoundEffect((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, "note.harp", 3F, 1);
      world.spawnParticle("note", (double) x + 0.5D, (double) y + 1.2D, (double) z + 0.5D, (double) 1 / 24D, 0.0D, 0.0D);
    } else {
      world.playSoundEffect((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, "note.hat", 3F, 1);
      world.spawnParticle("note", (double) x + 0.5D, (double) y + 1.2D, (double) z + 0.5D, (double) 1, 0.0D, 0.0D);
    }
  }

  protected boolean canSpawnSapling(World world, int worldX, int worldY, int worldZ) {
    int worldBlockID;
    for (int x = worldX - SAPLING_MINIMUM_DISTANCE_TO_OTHER; x <= worldX + SAPLING_MINIMUM_DISTANCE_TO_OTHER; ++x) {
      for (int y = worldY - SAPLING_MINIMUM_DISTANCE_TO_OTHER; y <= worldY + SAPLING_MINIMUM_DISTANCE_TO_OTHER; ++y) {
        for (int z = worldZ - SAPLING_MINIMUM_DISTANCE_TO_OTHER; z <= worldZ + SAPLING_MINIMUM_DISTANCE_TO_OTHER; ++z) {
          worldBlockID = world.getBlockId(x, y, z);
          if ((worldBlockID == Block.wood.blockID) || (worldBlockID == Block.sapling.blockID)) {
            return false;
          }
        }
      }
    }
    return true;
  }
}
