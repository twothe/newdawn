/*
 * Copyright (c) by Stefan Feldbinder aka Two
 */
package two.newdawn;

/**
 *
 * @author Two
 */
public class NoiseStretch {

  protected final double stretchX;
  protected final double stretchY;
  protected final double stretchZ;
  protected final SimplexNoise noise;

  public NoiseStretch(SimplexNoise noise, double stretchX, double stretchZ) {
    this(noise, stretchX, 1.0, stretchZ);
  }

  public NoiseStretch(SimplexNoise noise, double stretchX, double stretchY, double stretchZ) {
    this.noise = noise;
    this.stretchX = stretchX;
    this.stretchY = stretchY;
    this.stretchZ = stretchZ;
  }

  public double getNoise(final double blockX, final double blockY, final double blockZ) {
    return this.noise.noise(blockX / this.stretchX, blockY / this.stretchY, blockZ / this.stretchZ);
  }

  public double getNoise(final double blockX, final double blockZ) {
    return this.noise.noise(blockX / this.stretchX, blockZ / this.stretchZ);
  }
}
