/*
 */
package two.newdawn.worldgen;

import java.util.HashMap;

/**
 * This class collects chunk information during generation and is used to define the biomes later.
 *
 * Mods can read all these values to decide which biome is selected at a given world position.<br>
 * <br>
 * Mods are allowed to modify these values, but only if there is a good reason to.
 * Keep in mind that in this way of generation the terrain, these information define
 * the biome, and not the other way around.
 *
 * @author Two
 */
public class ChunkInformation {

  /* The maximum world height. You must not exceed it, Minecraft forbids it! */
  protected static final int WORLD_HEIGHT = 256;
  /* Global temperature thresholds */
  public static final double TEMPERATURE_FREEZING = -0.5;
  public static final double TEMPERATURE_HOT = 0.5;
  /* Global humidity thresholds */
  public static final double HUMIDITY_SPARSE = -0.5;
  public static final double HUMIDITY_WET = 0.5;
  /* A humidity level at which woodland is suggested */
  public final double MIN_HUMIDITY_WOODLAND = 0.18;
  /* The length of each side of a chunk */
  protected static final int CHUNK_SIZE_X = 16;
  protected static final int CHUNK_SIZE_Z = 16;
  /* The 2D area of a chunk, which is equal to the map sizes */
  protected static final int CHUNK_SIZE_XZ = CHUNK_SIZE_X * CHUNK_SIZE_Z;
  /**
   * Public data values
   */
  /* The chunk coordinate in chunk-space */
  public final int chunkX, chunkZ;
  /* The height of the first block above ocean water.<br>
   * This means that groundLevel - 1 is either a water block (ocean) or a non-water block (shore). */
  public final int groundLevel;
  /* The height-map of this chunk, where height is the first air block */
  public final int[] height;
  /* The region-height-map of this chunk */
  public final int[] regionHeight;
  /* Whether or not a given position is part of a mountain */
  public final boolean[] isMountain;
  /* The temperature-map of this chunk */
  public final float[] temperature;
  /* The humidity-map of this chunk */
  public final float[] humidity;
  /* The average height of this chunk */
  public final int averageHeight;
  /* The average temperature of this chunk */
  public final float averageTemperature;
  /* The average humidity of this chunk */
  public final float averageHumidity;
  /* Additional information. Intended for mods that need to store any kind of extra data for whatever reason. */
  public final HashMap<String, Object> additionalInformation = new HashMap<String, Object>();
  /* 
   * Internal values. 
   */
  protected final int lengthMapping;

  /**
   * Called internally by the terrain generation function. Mods receive the result.
   */
  public ChunkInformation(final int chunkX, final int chunkZ, final int groundLevel,
          final int[] height, final int[] regionHeight, final boolean[] isMountain, final float[] temperature, final float[] humidity) {
    this.chunkX = chunkX;
    this.chunkZ = chunkZ;
    this.height = height;
    this.regionHeight = regionHeight;
    this.isMountain = isMountain;
    this.groundLevel = groundLevel;
    this.temperature = temperature;
    this.humidity = humidity;
    this.lengthMapping = CHUNK_SIZE_X - 1; // assuming that length is a power of 2

    int avgHeight = 0;
    for (int i : this.height) {
      avgHeight += i;
    }
    averageHeight = Math.round(((float) avgHeight) / ((float) height.length));

    float avgTemperature = 0.0f;
    for (float f : this.temperature) {
      avgTemperature += f;
    }
    averageTemperature = avgTemperature / ((float) temperature.length);

    float avgHumidity = 0.0f;
    for (float f : this.humidity) {
      avgHumidity += f;
    }
    averageHumidity = avgHumidity / ((float) humidity.length);
  }

  /**
   * Returns the height of this chunk at the given block position.
   * The height is y-coordinate of the first non-terrain block (either air or water).<br>
   * If height &gt;= groundLevel, a player can walk at this position and the block
   * at height - 1 is a solid (non-water) block.<br>
   * If height &lt; groundLevel, the position is under water.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return the height of this chunk at the given block position.
   */
  public int getHeight(final int blockX, final int blockZ) {
    return height[(blockX & lengthMapping) + (blockZ & lengthMapping) * CHUNK_SIZE_X];
  }

  /**
   * Returns the height of the region at the given block position.
   * The region height is part of the height composition and describes the general
   * height of a given position. This is useful to decipher whether the actual height
   * is above/below a certain value because of local mountains/dales or because the
   * terrain just happens to be at that height in general.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return the height of the region at the given block position.
   */
  public int getRegionHeight(final int blockX, final int blockZ) {
    return regionHeight[(blockX & lengthMapping) + (blockZ & lengthMapping) * CHUNK_SIZE_X];
  }

  /**
   * Returns whether or not the terrain is part of a huge mountain at the given block position.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return whether or not the terrain is part of a huge mountain at the given block position.
   */
  public boolean isMountain(final int blockX, final int blockZ) {
    return isMountain[(blockX & lengthMapping) + (blockZ & lengthMapping) * CHUNK_SIZE_X];
  }

  /**
   * Returns the temperature at the given block position.
   * The temperature <i>usually</i> is within the range of -1 (cold) &lt;= T &lt;= 1 (hot),
   * but some calculations cause it to exceed these boundaries.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return the temperature at the given block position.
   */
  public float getTemperature(final int blockX, final int blockZ) {
    return temperature[(blockX & lengthMapping) + (blockZ & lengthMapping) * CHUNK_SIZE_X];
  }

  /**
   * Returns the humidity at the given block position.
   * The humidity <i>usually</i> is within the range of -1 (dry) &lt;= H &lt;= 1 (wet),
   * but some calculations cause it to exceed these boundaries.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return the humidity at the given block position.
   */
  public float getHumidity(final int blockX, final int blockZ) {
    return humidity[(blockX & lengthMapping) + (blockZ & lengthMapping) * CHUNK_SIZE_X];
  }

  /**
   * Returns whether or not the given block position has a temperature that is considered hot.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return whether or not the given block position has a temperature that is considered hot.
   */
  public boolean isTemperatureHot(final int blockX, final int blockZ) {
    return (getTemperature(blockX, blockZ) >= TEMPERATURE_HOT);
  }

  /**
   * Returns whether or not the given block position has a temperature that is considered hot, if the given modifier is added.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @param modifier a modifier to be applied to the temperature before checking.
   * @return whether or not the given block position has a temperature that is considered hot, if the given modifier is added.
   */
  public boolean isTemperatureHot(final int blockX, final int blockZ, final float modifier) {
    return ((getTemperature(blockX, blockZ) + modifier) >= TEMPERATURE_HOT);
  }

  /**
   * Returns whether or not the given block position has a temperature that is considered freezing cold.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return whether or not the given block position has a temperature that is considered freezing cold.
   */
  public boolean isTemperatureFreezing(final int blockX, final int blockZ) {
    return (getTemperature(blockX, blockZ) <= TEMPERATURE_FREEZING);
  }

  /**
   * Returns whether or not the given block position has a temperature that is considered freezing cold, if the given modifier is added.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @param modifier a modifier to be applied to the temperature before checking.
   * @return whether or not the given block position has a temperature that is considered freezing cold, if the given modifier is added.
   */
  public boolean isTemperatureFreezing(final int blockX, final int blockZ, final float modifier) {
    return ((getTemperature(blockX, blockZ) + modifier) <= TEMPERATURE_FREEZING);
  }

  /**
   * Returns whether or not the given block position has a temperature that is considered neither freezing nor hot.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return whether or not the given block position has a temperature that is considered neither freezing nor hot.
   */
  public boolean isTemperatureMedium(final int blockX, final int blockZ) {
    final float blockTemperature = getTemperature(blockX, blockZ);
    return ((blockTemperature > TEMPERATURE_FREEZING) && (blockTemperature < TEMPERATURE_HOT));
  }

  /**
   * Returns whether or not the given block position has a temperature that is considered neither freezing nor hot, if the given modifier is added.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @param modifier a modifier to be applied to the temperature before checking.
   * @return whether or not the given block position has a temperature that is considered neither freezing nor hot, if the given modifier is added.
   */
  public boolean isTemperatureMedium(final int blockX, final int blockZ, final float modifier) {
    final float blockTemperature = getTemperature(blockX, blockZ) + modifier;
    return ((blockTemperature > TEMPERATURE_FREEZING) && (blockTemperature < TEMPERATURE_HOT));
  }

  /**
   * Returns whether or not the given block position has a humidity that is considered wet.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return whether or not the given block position has a humidity that is considered wet.
   */
  public boolean isHumidityWet(final int blockX, final int blockZ) {
    return (getHumidity(blockX, blockZ) >= HUMIDITY_WET);
  }

  /**
   * Returns whether or not the given block position has a humidity that is considered wet, if the given modifier is added.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @param modifier a modifier to be applied to the humidity before checking.
   * @return whether or not the given block position has a humidity that is considered wet, if the given modifier is added.
   */
  public boolean isHumidityWet(final int blockX, final int blockZ, final float modifier) {
    return ((getHumidity(blockX, blockZ) + modifier) >= HUMIDITY_WET);
  }

  /**
   * Returns whether or not the given block position has a humidity that is considered sparse.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return whether or not the given block position has a humidity that is considered sparse.
   */
  public boolean isHumiditySparse(final int blockX, final int blockZ) {
    return (getHumidity(blockX, blockZ) <= HUMIDITY_SPARSE);
  }

  /**
   * Returns whether or not the given block position has a humidity that is considered sparse, if the given modifier is added.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @param modifier a modifier to be applied to the humidity before checking.
   * @return whether or not the given block position has a humidity that is considered sparse, if the given modifier is added.
   */
  public boolean isHumiditySparse(final int blockX, final int blockZ, final float modifier) {
    return ((getHumidity(blockX, blockZ) + modifier) <= HUMIDITY_SPARSE);
  }

  /**
   * Returns whether or not the given block position has a humidity that is considered neither wet nor sparse.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return whether or not the given block position has a humidity that is considered neither wet nor sparse.
   */
  public boolean isHumidityMedium(final int blockX, final int blockZ) {
    final float blockHumidity = getHumidity(blockX, blockZ);
    return ((blockHumidity > HUMIDITY_SPARSE) && (blockHumidity < HUMIDITY_WET));
  }

  /**
   * Returns whether or not the given block position has a humidity that is considered neither wet nor sparse, if the given modifier is added.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @param modifier a modifier to be applied to the humidity before checking.
   * @return whether or not the given block position has a humidity that is considered neither wet nor sparse, if the given modifier is added.
   */
  public boolean isHumidityMedium(final int blockX, final int blockZ, final float modifier) {
    final float blockHumidity = getHumidity(blockX, blockZ) + modifier;
    return ((blockHumidity > HUMIDITY_SPARSE) && (blockHumidity < HUMIDITY_WET));
  }

  /**
   * Returns whether or not the given block position has a humidity that is sufficient for woodland.
   * The threshold is an arbitrary choice that causes the world to have a reasonable amount of woodland.
   *
   * @param blockXthe block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return whether or not the given block position has a humidity that is sufficient for woodland.
   */
  public boolean isHumidityWoodland(final int blockX, final int blockZ) {
    return (getHumidity(blockX, blockZ) >= MIN_HUMIDITY_WOODLAND);
  }

  /**
   * Returns whether or not the given block position has a humidity that is sufficient for woodland, if the given modifier is added.
   * The threshold is an arbitrary choice that causes the world to have a reasonable amount of woodland.
   *
   * @param blockXthe block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return whether or not the given block position has a humidity that is sufficient for woodland, if the given modifier is added.
   */
  public boolean isHumidityWoodland(final int blockX, final int blockZ, final float modifier) {
    return (getHumidity(blockX, blockZ) + modifier >= MIN_HUMIDITY_WOODLAND);
  }

  /**
   * Returns whether or not a given block position is exactly at ground level.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return whether or not a given block position is exactly at ground level.
   */
  public boolean isGroundLevel(final int blockX, final int blockZ) {
    return (getHeight(blockX, blockZ) == groundLevel);
  }

  /**
   * Returns whether or not a given block position is above sea level.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return whether or not a given block position is above sea level.
   */
  public boolean isAboveSeaLevel(final int blockX, final int blockZ) {
    return (getHeight(blockX, blockZ) >= groundLevel);
  }

  /**
   * Returns whether or not a given block position is below or at sea level.
   * Any terrain at a height where this returns true is usually filled with water.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return whether or not a given block position is below or at sea level.
   */
  public boolean isBelowGroundLevel(final int blockX, final int blockZ) {
    return (getHeight(blockX, blockZ) < groundLevel);
  }

  /**
   * Returns if the given block position is deep water.
   * A player needs to swim if in water that is considered deep.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return if the given block position is deep water.
   */
  public boolean isDeepWater(final int blockX, final int blockZ) {
    return (getHeight(blockX, blockZ) + 1 < groundLevel);
  }

  /**
   * Returns if the given block position is shallow water.
   * A player can still walk if in water that is considered shallow.<br>
   * Returns false if the given block position is not below ground level.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return if the given block position is shallow water.
   */
  public boolean isShallowWater(final int blockX, final int blockZ) {
    return (getHeight(blockX, blockZ) + 1 == groundLevel);
  }

  /**
   * Returns if the given block position is either ground level or shallow water.
   * This is used for biomes that can exist in both cases like swampland.
   *
   * @param blockX the block x-coordinate in world space.
   * @param blockZ the block z-coordinate in world space.
   * @return if the given block position is either ground level or shallow water.
   */
  public boolean isGroundLevelOrShallowWater(final int blockX, final int blockZ) {
    final int blockHeight = getHeight(blockX, blockZ);
    return ((blockHeight == groundLevel) || (blockHeight + 1 == groundLevel));
  }

  /**
   * Returns the average humidity of this chunk.
   *
   * @return the average humidity of this chunk.
   */
  public float getAverageHumidity() {
    return averageHumidity;
  }

  /**
   * Returns the average temperature of this chunk.
   *
   * @return the average temperature of this chunk.
   */
  public float getAverageTemperature() {
    return averageTemperature;
  }

  /**
   * Returns the average height of this chunk.
   *
   * @return the average height of this chunk.
   */
  public int getAverageHeight() {
    return averageHeight;
  }
}
