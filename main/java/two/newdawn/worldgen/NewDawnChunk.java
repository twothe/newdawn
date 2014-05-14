/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package two.newdawn.worldgen;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 *
 * @author Two
 */
public class NewDawnChunk extends Chunk {

  public NewDawnChunk(final World world, final Block[] chunkData, final int chunkX, final int chunkZ) {
    super(world, chunkX, chunkZ);
    final int worldHeight = chunkData.length / 256;
    final ExtendedBlockStorage[] extendedStorage = this.getBlockStorageArray();
    final boolean worldHasSky = !world.provider.hasNoSky;

    for (int x = 0; x < 16; ++x) {
      for (int z = 0; z < 16; ++z) {
        for (int y = 0; y < worldHeight; ++y) {
          Block block = chunkData[x << 12 | z << 8 | y];

          if (block != null && block.getMaterial() != Material.air) {
            int storageIndex = y >> 4;

            if (extendedStorage[storageIndex] == null) {
              extendedStorage[storageIndex] = new ExtendedBlockStorage(storageIndex << 4, worldHasSky);
            }

            extendedStorage[storageIndex].func_150818_a(x, y & 15, z, block);
          }
        }
      }
    }
  }
}
