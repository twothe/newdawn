package two.newdawn.terrain;

/**
 * Broad mountain bodies with subordinate multifractal ridges and local surface relief.
 * Climate is the broad, uncorrected climate at this coordinate;
 * neither neighboring heights nor the resulting altitude feed back into this profile.
 */
final class MountainProfile {
    private MountainProfile() {}

    /** Retains the legacy distribution and threshold, with a continuous zero-height boundary. */
    static double influence(double noise) {
        if (noise <= 0.0) return 0.0;
        double legacy = -(Math.cos(Math.PI * Math.pow(noise, 4.0)) - 1.0) / 2.0;
        double influence = Math.max(0.0, (legacy - 0.01) / 0.99);
        // Broaden substantial relief inside the existing footprint without moving its boundary.
        return influence * (2.0 - influence);
    }

    /**
     * Returns added height and optionally overwrites the analytical exposed-rock indicator.
     * Influence and broad variation are in [0, 1]; all other noise inputs are in [-1, 1].
     * Every contribution fades with influence, including local detail and cliffs.
     */
    static double height(double influence, double broadVariation, double large, double small,
                         double local, double detail,
                         double temperature, double humidity, TerrainColumn target) {
        double dry = 1.0 - smooth(-0.45, 0.30, humidity);
        // Moisture and a cool transition climate favor broken rock; permanent cold is not amplified.
        double frost = smooth(-0.70, -0.25, temperature) * (1.0 - smooth(-0.15, 0.15, temperature))
                * smooth(-0.35, 0.35, humidity);
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
        double profile = 0.60 + 0.15 * broadVariation + 0.25 * ridgeProfile * summitVariation;
        // Reused block-scale noise supplies coherent roughness; two-block detail stays subordinate.
        double surfaceRelief = local * 5.0 + detail * (0.5 + ruggedness * 0.75);

        // Reshape one flank into a finite steep band, rather than adding a discontinuous height step.
        double cliffStrength = 0.15 + dry * 0.70 + frost * 0.40;
        double edge = 0.32 + small * 0.035;
        double transition = (edge + 0.045 - large) / 0.09;
        double cliffPatch = smooth(-0.30, 0.55, small);
        double cliffAmplitude = influence * cliffStrength * cliffPatch * 18.0;
        double cliff = cliffAmplitude * (smooth(0.0, 1.0, transition) - smooth(0.07, 0.62, 0.69 - large));
        if (target != null) {
            target.exposedRock = cliffAmplitude >= 3.0 && transition > 0.12 && transition < 0.88;
        }
        return influence * (144.0 * profile + surfaceRelief) + cliff;
    }

    /** Cubic blend with constant endpoints; all profile transitions use bounded arithmetic. */
    private static double smooth(double lower, double upper, double value) {
        double t = Math.max(0.0, Math.min(1.0, (value - lower) / (upper - lower)));
        return t * t * (3.0 - 2.0 * t);
    }
}
