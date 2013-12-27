/*
 */
package two.newdawn.API;

/**
 * @author Two
 */
public class WorldBaseValues {
  /* Global temperature thresholds */

  public final float temperatureFreezing;
  public final float temperatureHot;
  /* Global humidity thresholds */
  public final float humiditySparse;
  public final float humidityWet;
  /* A humidity level at which woodland is suggested */
  public final float minHumidityWoodland;
  /* The height of the first block above ocean water.<br>
   * This means that groundLevel - 1 is either a water block (ocean) or a non-water block (shore). */
  public final int groundLevel;

  public WorldBaseValues(final float temperatureFreezing, final float temperatureHot, 
          final float humiditySparse, final float humidityWet, 
          final float minHumidityWoodland, 
          final int groundLevel) {
    this.temperatureFreezing = temperatureFreezing;
    this.temperatureHot = temperatureHot;
    this.humiditySparse = humiditySparse;
    this.humidityWet = humidityWet;
    this.minHumidityWoodland = minHumidityWoodland;
    this.groundLevel = groundLevel;
  }
}
