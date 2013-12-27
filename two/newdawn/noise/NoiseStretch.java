/*
 * Copyright (c) by Stefan Feldbinder aka Two
 */
package two.newdawn.noise;

/**
 *
 * @author Two
 */
public class NoiseStretch {

  protected final double stretchX;
  protected final double stretchY;
  protected final double stretchZ;
  protected final double offsetX;
  protected final double offsetY;
  protected final double offsetZ;
  protected final SimplexNoise noise;

  public NoiseStretch(SimplexNoise noise, double stretchX, double stretchZ, double offsetX, double offsetZ) {
    this(noise, stretchX, 1.0, stretchZ, offsetX, 0.0, offsetZ);
  }

  public NoiseStretch(SimplexNoise noise, double stretchX, double stretchY, double stretchZ, double offsetX, double offsetY, double offsetZ) {
    this.noise = noise;
    this.stretchX = stretchX;
    this.stretchY = stretchY;
    this.stretchZ = stretchZ;
    this.offsetX = offsetX;
    this.offsetY = offsetY;
    this.offsetZ = offsetZ;
  }

  public double getNoise(final double blockX, final double blockY, final double blockZ) {
    return this.noise.noise(blockX / this.stretchX + offsetX, blockY / this.stretchY + offsetY, blockZ / this.stretchZ + offsetZ);
  }

  public double getNoise(final double blockX, final double blockZ) {
    return this.noise.noise(blockX / this.stretchX + offsetX, blockZ / this.stretchZ + offsetZ);
  }
}
