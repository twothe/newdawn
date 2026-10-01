package two.newdawn.terrain;

import static two.newdawn.terrain.NoiseMath.smooth;

/**
 * Climate-gated, bounded cliff deformation of a mountain height field.
 * All inputs belong to the current column; no slope probes or neighboring terrain are needed.
 */
final class CliffProfile {
    private static final double MINIMUM_INFLUENCE = 0.08;
    private static final double HEIGHT_AMPLITUDE = 44.0;
    private static final double FACE_HALF_WIDTH = 0.075;
    private static final double END_WIDENING = 0.10;
    private static final double APRON_WIDTH = 2.5;
    private static final double FACE_WEIGHT = 0.80;
    private static final double APRON_WEIGHT = 0.20;

    private CliffProfile() {}

    /**
     * Returns activation in [0, 1] from mountain influence, uncorrected climate and signed patch noise.
     * Zero activation permits the caller to skip the dedicated cliff noise query entirely.
     */
    static double strength(double influence, double temperature, double humidity, double patchNoise) {
        if (influence <= MINIMUM_INFLUENCE) return 0.0;
        return strengthFromWeathering(influence, ClimateRules.dryness(humidity),
                ClimateRules.frostWeathering(temperature, humidity), patchNoise);
    }

    /** Composition entry point sharing weathering with the mountain body. */
    static double strengthFromWeathering(double influence, double dry, double frost, double patchNoise) {
        if (influence <= MINIMUM_INFLUENCE) return 0.0;
        double weathering = Math.min(1.0, 0.15 + dry * 0.80 + frost * 0.65);
        // Favorable climates widen the eligible patches as well as increasing their relief.
        double threshold = 0.55 - weathering * 0.85;
        double patch = smooth(threshold, threshold + 0.30, patchNoise);
        return influence * smooth(MINIMUM_INFLUENCE, 0.40, influence) * weathering * patch;
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
        double halfWidth = FACE_HALF_WIDTH + (secondary + 1.0) * 0.0125 + endBlend * END_WIDENING;
        double amplitude = strength * HEIGHT_AMPLITUDE;
        double face = smooth(-halfWidth, halfWidth, position);
        double apron = smooth(-halfWidth * APRON_WIDTH, halfWidth * APRON_WIDTH, position);
        // Most relief remains in the rock face; a smaller share connects both sides to the slope.
        double transition = face * FACE_WEIGHT + apron * APRON_WEIGHT;
        double shoulder = smooth(-0.75, 0.75, position);
        if (target != null) {
            target.exposedRock = amplitude >= 6.0 && Math.abs(position) < halfWidth * 0.85;
        }
        return amplitude * (transition - shoulder);
    }

}
