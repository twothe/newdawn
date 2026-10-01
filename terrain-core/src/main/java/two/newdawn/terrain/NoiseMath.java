package two.newdawn.terrain;

/** Bounded interpolation shared by pointwise terrain profiles; contains no noise sampling or state. */
final class NoiseMath {
    private NoiseMath() {}

    /** Cubic blend with constant endpoints. Callers provide lower < upper. */
    static double smooth(double lower, double upper, double value) {
        double t = Math.max(0.0, Math.min(1.0, (value - lower) / (upper - lower)));
        return t * t * (3.0 - 2.0 * t);
    }
}
