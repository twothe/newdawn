/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package two.newdawn.worldgen;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 *
 * @author Two
 */
public class NewDawnChunk extends Chunk {

  public NewDawnChunk(final World world, final byte[] chunkData, final int chunkX, final int chunkZ) {
    super(world, chunkX, chunkZ);

    final ExtendedBlockStorage[] storageArrays = new ExtendedBlockStorage[16];
    // Copied from MC Chunk constructor
    final int worldHeight = chunkData.length / 256;

    for (int x = 0; x < 16; ++x) {
      for (int z = 0; z < 16; ++z) {
        for (int y = 0; y < worldHeight; ++y) {
          final int blockID = ((worldHeight == 256) ? chunkData[x << 12 | z << 8 | y] : chunkData[x << 11 | z << 7 | y]) & 0xFF;

          if (blockID != 0) {
            int storageId = y >> 4;

            if (storageArrays[storageId] == null) {
              storageArrays[storageId] = new ExtendedBlockStorage(storageId << 4, !world.provider.hasNoSky);
            }

            storageArrays[storageId].setExtBlockID(x, y & 15, z, blockID);
          }
        }
      }
    }

    super.setStorageArrays(storageArrays);
  }
}
