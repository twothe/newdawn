package two.newdawn.terrain;

import static two.newdawn.terrain.NoiseMath.smooth;

/**
 * Broad mountain bodies with subordinate multifractal ridges and local surface relief.
 * Climate is the broad, uncorrected climate at this coordinate;
 * neither neighboring heights nor the resulting altitude feed back into this profile.
 */
final class MountainProfile {
    private static final double DISTRIBUTION_THRESHOLD = 0.01;
    private static final double HEIGHT_AMPLITUDE = 144.0;
    private static final double BODY_WEIGHT = 0.60;
    private static final double BROAD_VARIATION_WEIGHT = 0.15;
    private static final double RIDGE_WEIGHT = 0.25;
    private static final double LOCAL_RELIEF = 5.0;

    private MountainProfile() {}

    /** Retains the legacy distribution and threshold, with a continuous zero-height boundary. */
    static double influence(double noise) {
        if (noise <= 0.0) return 0.0;
        double legacy = -(Math.cos(Math.PI * Math.pow(noise, 4.0)) - 1.0) / 2.0;
        double influence = Math.max(0.0, (legacy - DISTRIBUTION_THRESHOLD) / (1.0 - DISTRIBUTION_THRESHOLD));
        // Broaden substantial relief inside the existing footprint without moving its boundary.
        return influence * (2.0 - influence);
    }

    /**
     * Returns the mountain body, ridges and local detail before independent cliff deformation.
     * Influence and broad variation are in [0, 1]; all other noise inputs are in [-1, 1].
     * Every contribution fades with influence, including local detail.
     */
    static double height(double influence, double broadVariation, double large, double small,
                         double local, double detail,
                         double temperature, double humidity) {
        return heightFromWeathering(influence, broadVariation, large, small, local, detail,
                ClimateRules.dryness(humidity), ClimateRules.frostWeathering(temperature, humidity));
    }

    /** Composition entry point when the caller already evaluated climate weathering. */
    static double heightFromWeathering(double influence, double broadVariation, double large, double small,
                                       double local, double detail, double dry, double frost) {
        double ruggedness = 0.55 + dry * 0.30 + frost * 0.15;
        double ridge = 1.0 - Math.abs(large);
        double signal = ridge * ridge;
        double shoulder = 1.0 - large * large;
        double secondary = 1.0 - Math.abs(small);
        // A broad body survives between ridges; the folded signals only sculpt its upper relief.
        double feedback = Math.min(1.0, signal * 2.0);
        double ridgeProfile = 0.75 * (shoulder + (signal - shoulder) * ruggedness)
                + 0.25 * secondary * secondary * feedback;
        // Signed finer noise breaks long ridge contours into summit segments and saddles.
        double summitVariation = 0.55 + 0.45 * smooth(-0.65, 0.65, small);
        double profile = BODY_WEIGHT + BROAD_VARIATION_WEIGHT * broadVariation + RIDGE_WEIGHT * ridgeProfile * summitVariation;
        // Reused block-scale noise supplies coherent roughness; two-block detail stays subordinate.
        double surfaceRelief = local * LOCAL_RELIEF + detail * (0.5 + ruggedness * 0.75);

        return influence * (HEIGHT_AMPLITUDE * profile + surfaceRelief);
    }

}
