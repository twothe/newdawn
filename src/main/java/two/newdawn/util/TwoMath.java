/*
 */
package two.newdawn.util;

/**
 * @author Two
 */
public class TwoMath {

  public static int withinBounds(final int value, final int lowerBound, final int upperBound) {
    if (value < lowerBound) {
      return lowerBound;
    } else if (value > upperBound) {
      return upperBound;
    } else {
      return value;
    }
  }
}
