/*
 */
package two.newdawn.worldgen.thaumcraft;

/**
 * @author Two
 */
public class ThaumcraftConfiguration {

  public final double thresholdTaint;
  public final double thresholdEvil;
  public final double thresholdGood;

  public ThaumcraftConfiguration(final double thresholdTaint, final double thresholdEvil, final double thresholdGood) {    
    this.thresholdTaint = thresholdTaint;
    this.thresholdEvil = thresholdEvil;
    this.thresholdGood = thresholdGood;
  }

}
