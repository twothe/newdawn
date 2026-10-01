package two.newdawn.terrain;

/**
 * Climate-gated, bounded cliff deformation of a mountain height field.
 * All inputs belong to the current column; no slope probes or neighboring terrain are needed.
 */
final class CliffProfile {
    private CliffProfile() {}

    /**
     * Returns activation in [0, 1] from mountain influence, uncorrected climate and signed patch noise.
     * Zero activation permits the caller to skip the dedicated cliff noise query entirely.
     */
    static double strength(double influence, double temperature, double humidity, double patchNoise) {
        if (influence <= 0.08) return 0.0;
        double dry = 1.0 - smooth(-0.45, 0.30, humidity);
        double frost = smooth(-0.70, -0.25, temperature) * (1.0 - smooth(-0.15, 0.15, temperature))
                * smooth(-0.35, 0.35, humidity);
        double weathering = Math.min(1.0, 0.15 + dry * 0.80 + frost * 0.65);
        // Favorable climates widen the eligible patches as well as increasing their relief.
        double threshold = 0.55 - weathering * 0.85;
        double patch = smooth(threshold, threshold + 0.30, patchNoise);
        return influence * smooth(0.08, 0.40, influence) * weathering * patch;
    }

    /**
     * Steepens a noise contour with short head/foot transitions and gentler fading ends.
     * Returns to zero beyond its shoulders instead of cutting deep holes.
     * Strength is in [0, 1]; noise/local/secondary are signed noise samples in [-1, 1].
     * Overwrites exposedRock when target is supplied, including inactive columns.
     */
    static double height(double strength, double noise, double local, double secondary, TerrainColumn target) {
        double position = noise + local * 0.025 + secondary * 0.06;
        // Keep strong faces steep, but avoid spending the full drop in one or two block columns.
        double endBlend = 1.0 - smooth(0.15, 0.55, strength);
        double halfWidth = 0.075 + (secondary + 1.0) * 0.0125 + endBlend * 0.10;
        double amplitude = strength * 44.0;
        double face = smooth(-halfWidth, halfWidth, position);
        double apron = smooth(-halfWidth * 2.5, halfWidth * 2.5, position);
        // Most relief remains in the rock face; a smaller share connects both sides to the slope.
        double transition = face * 0.80 + apron * 0.20;
        double shoulder = smooth(-0.75, 0.75, position);
        if (target != null) {
            target.exposedRock = amplitude >= 6.0 && Math.abs(position) < halfWidth * 0.85;
        }
        return amplitude * (transition - shoulder);
    }

    private static double smooth(double lower, double upper, double value) {
        double t = Math.max(0.0, Math.min(1.0, (value - lower) / (upper - lower)));
        return t * t * (3.0 - 2.0 * t);
    }
}
