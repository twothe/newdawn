/*
 */
package two.newdawn.util;

import java.util.Iterator;
import joptsimple.internal.Objects;
import net.minecraft.world.ChunkCoordIntPair;

/**
 * @author Two
 */
public class SpiralPatternGenerator implements Iterable<ChunkCoordIntPair> {

  private enum SpiralDirection {

    right, down, left, up
  };

  protected final ChunkCoordIntPair center;
  protected final int rangeMax;

  public SpiralPatternGenerator(final ChunkCoordIntPair center) {
    this(center, Integer.MAX_VALUE);
  }

  public SpiralPatternGenerator(final ChunkCoordIntPair center, final int rangeMax) {
    Objects.ensureNotNull(center);
    if (rangeMax < 0) {
      throw new IllegalArgumentException("RangeMax must be >= 0");
    }
    this.center = center;
    this.rangeMax = rangeMax;
  }

  @Override
  public Iterator<ChunkCoordIntPair> iterator() {
    return new Iterator<ChunkCoordIntPair>() {
      private int currentX, currentZ;
      protected SpiralDirection currentDirection;
      private int currentRange = 0;

      @Override
      public boolean hasNext() {
        return (currentRange <= rangeMax);
      }

      @Override
      public ChunkCoordIntPair next() {
        if (currentRange == 0) {
          ++currentRange;
          currentX = center.chunkXPos - currentRange;
          currentZ = center.chunkZPos - currentRange;
          currentDirection = SpiralDirection.right;
          return center;
        } else {
          final ChunkCoordIntPair result = new ChunkCoordIntPair(currentX, currentZ);
          step();
          return result;
        }
      }

      protected void step() {
        switch (currentDirection) {
          case right:
            if (currentX < center.chunkXPos + currentRange) {
              ++currentX;
            } else {
              currentDirection = SpiralDirection.down;
              step();
            }
          case down:
            if (currentZ < center.chunkZPos + currentRange) {
              ++currentZ;
            } else {
              currentDirection = SpiralDirection.left;
              step();
            }
          case left:
            if (currentX > center.chunkXPos - currentRange) {
              --currentX;
            } else {
              currentDirection = SpiralDirection.up;
              step();
            }
          case up:
            if (currentZ - 1 > center.chunkZPos - currentRange) { // -1 to skip the top-left here
              --currentZ;
            } else {
              currentDirection = SpiralDirection.right;
              ++currentRange;
              currentX = center.chunkXPos - currentRange;
              currentZ = center.chunkZPos - currentRange;
            }
        }
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("Not supported yet."); // TODO: implement
      }
    };
  }

  public ChunkCoordIntPair getCenter() {
    return center;
  }

  public int getRangeMax() {
    return rangeMax;
  }

}
