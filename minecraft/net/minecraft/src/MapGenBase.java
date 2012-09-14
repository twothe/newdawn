package net.minecraft.src;

import java.util.Random;

public class MapGenBase {

  /**
   * The number of Chunks to gen-check in any given direction.
   */
  protected int range;
  /**
   * The RNG used by the MapGen classes.
   */
  protected final Random rand;
  /**
   * This world object.
   */
  protected World worldObj;

  public MapGenBase() {
    range = 8;
    rand = new Random();
  }

  public void generate(IChunkProvider chunkProvider, World world, int chunkX, int chunkZ, byte chunkData[]) {
    final int scanRange = range;
    worldObj = world;
    rand.setSeed(world.getSeed());
    long randX = rand.nextLong();
    long randZ = rand.nextLong();

    for (int x = chunkX - scanRange; x <= chunkX + scanRange; x++) {
      for (int z = chunkZ - scanRange; z <= chunkZ + scanRange; z++) {
        long randSeedX = (long) x * randX;
        long randSeedZ = (long) z * randZ;
        rand.setSeed(randSeedX ^ randSeedZ ^ world.getSeed());
        recursiveGenerate(world, x, z, chunkX, chunkZ, chunkData);
      }
    }
  }

  /**
   * Recursively called by generate() (generate) and optionally by itself.
   */
  protected void recursiveGenerate(World world, int i, int j, int chunkX, int chunkZ, byte chunkData[]) {
  }
}
