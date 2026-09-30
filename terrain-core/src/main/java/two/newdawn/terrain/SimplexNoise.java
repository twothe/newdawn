package two.newdawn.terrain;

import java.util.Random;

/*
 * A speed-improved simplex noise algorithm for 2D, 3D and 4D in Java.
 *
 * Based on example code by Stefan Gustavson (stegu@itn.liu.se). Optimisations
 * by Peter Eastman (peastman@drizzle.stanford.edu). Better rank ordering method
 * by Stefan Gustavson in 2012.
 *
 * This could be speeded up even further, but it's useful as it is.
 *
 * Version 2012-03-09
 *
 * This code was placed in the public domain by its original author, Stefan
 * Gustavson. You may use it as you see fit, but attribution is appreciated.
 *
 */
/** Immutable legacy 1024-entry 2D noise field. Sampling never advances random state. */
final class SimplexNoise {
  private static final Grad grad3[] = {new Grad(1, 1, 0), new Grad(-1, 1, 0), new Grad(1, -1, 0), new Grad(-1, -1, 0),
    new Grad(1, 0, 1), new Grad(-1, 0, 1), new Grad(1, 0, -1), new Grad(-1, 0, -1),
    new Grad(0, 1, 1), new Grad(0, -1, 1), new Grad(0, 1, -1), new Grad(0, -1, -1)};
  private static final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
  private static final double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;
  private final short[] doubledPermutationTable;
  private final short[] variatedPermutationTable;

  SimplexNoise(Random random) {
    final byte[] bytes = new byte[1024];
    random.nextBytes(bytes);
    doubledPermutationTable = new short[bytes.length * 2];
    variatedPermutationTable = new short[doubledPermutationTable.length];
    for (int i = 0; i < bytes.length; ++i) {
      final short value = (short) (bytes[i] & 0xFF);
      doubledPermutationTable[i] = value;
      variatedPermutationTable[i] = (short) (value % 12);
    }
    System.arraycopy(doubledPermutationTable, 0, doubledPermutationTable, bytes.length, bytes.length);
    System.arraycopy(variatedPermutationTable, 0, variatedPermutationTable, bytes.length, bytes.length);
  }

  private static int fastfloor(double x) {
    int xi = (int) x;
    return x < xi ? xi - 1 : xi;
  }

  private static double dot(Grad g, double x, double y) {
    return g.x * x + g.y * y;
  }

  public double noise(double xin, double yin) {
    // Skew the input space to determine which simplex cell we're in
    final double s = (xin + yin) * F2; // Hairy factor for 2D
    final int i = fastfloor(xin + s);
    final int j = fastfloor(yin + s);
    final double t = ((double) (i + j)) * G2;
    final double X0 = (double) i - t; // Unskew the cell origin back to (x,y) space
    final double Y0 = (double) j - t;
    final double x0 = xin - X0; // The x,y distances from the cell origin
    final double y0 = yin - Y0;
    // For the 2D case, the simplex shape is an equilateral triangle.
    // Determine which simplex we are in.
    final int i1, j1; // Offsets for second (middle) corner of simplex in (i,j) coords
    if (x0 > y0) {
      i1 = 1;
      j1 = 0;
    } else { // lower triangle, XY order: (0,0)->(1,0)->(1,1)
      i1 = 0;
      j1 = 1;
    }      // upper triangle, YX order: (0,0)->(0,1)->(1,1)
    // A step of (1,0) in (i,j) means a step of (1-c,-c) in (x,y), and
    // a step of (0,1) in (i,j) means a step of (-c,1-c) in (x,y), where
    // c = (3-sqrt(3))/6
    final double x1 = x0 - (double) i1 + G2; // Offsets for middle corner in (x,y) unskewed coords
    final double y1 = y0 - (double) j1 + G2;
    final double x2 = x0 - 1.0 + 2.0 * G2; // Offsets for last corner in (x,y) unskewed coords
    final double y2 = y0 - 1.0 + 2.0 * G2;
    // Work out the hashed gradient indices of the three simplex corners
    final int ii = i & 1023;
    final int jj = j & 1023;
    final int gi0 = variatedPermutationTable[ii + doubledPermutationTable[jj]];
    final int gi1 = variatedPermutationTable[ii + i1 + doubledPermutationTable[jj + j1]];
    final int gi2 = variatedPermutationTable[ii + 1 + doubledPermutationTable[jj + 1]];
    final double n0, n1, n2; // Noise contributions from the three corners
    // Calculate the contribution from the three corners
    double t0 = 0.5 - x0 * x0 - y0 * y0;
    if (t0 < 0) {
      n0 = 0.0;
    } else {
      t0 *= t0;
      n0 = t0 * t0 * dot(grad3[gi0], x0, y0);  // (x,y) of grad3 used for 2D gradient
    }
    double t1 = 0.5 - x1 * x1 - y1 * y1;
    if (t1 < 0) {
      n1 = 0.0;
    } else {
      t1 *= t1;
      n1 = t1 * t1 * dot(grad3[gi1], x1, y1);
    }
    double t2 = 0.5 - x2 * x2 - y2 * y2;
    if (t2 < 0) {
      n2 = 0.0;
    } else {
      t2 *= t2;
      n2 = t2 * t2 * dot(grad3[gi2], x2, y2);
    }
    // Add contributions from each corner to get the final noise value.
    // The result is scaled to return values in the interval [-1,1].
    return 70.0 * (n0 + n1 + n2);
  }

  private record Grad(double x, double y, double z) {}
}
