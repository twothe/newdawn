package two.newdawn.terrain;

/** Functional mountain-profile contracts and seeded production surveys without fixed visual snapshots. */
public final class MountainRegressionTest {
    public static void main(String[] args) {
        verifyProfile();
        verifyBodyAndDetail();
        verifyCliffs();
        verifyCliffTransitions();
        for (long seed : new long[]{123456789L, -8458999313514431577L}) survey(seed);
        System.out.println("PASS: substantial mountain bodies, local detail, climate-shaped cliffs and consistent sampling");
    }

    private static void verifyProfile() {
        require(MountainProfile.influence(-1) == 0, "Negative influence leaves mountain relief");
        double previous = 0;
        for (int i = 0; i <= 10000; i++) {
            double influence = MountainProfile.influence(i / 10000.0);
            require(Double.isFinite(influence) && influence >= previous && influence <= 1,
                    "Mountain influence is not finite, bounded and monotone");
            previous = influence;
        }
        require(MountainProfile.height(0, 0.5, 0.32, 0, 1, 1, 0.2, -0.8) == 0,
                "Zero mountain influence leaves relief");
        for (double temperature : new double[]{-1, -0.25, 0.2, 1}) {
            for (double humidity : new double[]{-1, 0, 1}) {
                for (int i = -9999; i <= 10000; i++) {
                    double height = MountainProfile.height(1, 0.5, i / 10000.0, 0.2, 0, 0,
                            temperature, humidity);
                    require(Double.isFinite(height) && height >= 0, "Invalid mountain relief");
                }
            }
        }
    }

    /** Shape relationships leave tuning free while guarding against hollow bodies and missing detail. */
    private static void verifyBodyAndDetail() {
        double minimum = Double.POSITIVE_INFINITY, maximum = 0;
        double[] signals = {-1, -0.65, -0.32, 0, 0.32, 0.65, 1};
        for (double temperature : new double[]{-1, -0.25, 0.2, 1}) {
            for (double humidity : new double[]{-1, 0, 1}) {
                for (double broad : new double[]{0, 1}) {
                    for (double main : signals) for (double secondary : signals) {
                        double lower = MountainProfile.height(1, broad, main, secondary, -1, -1,
                                temperature, humidity);
                        double upper = MountainProfile.height(1, broad, main, secondary, 1, 1,
                                temperature, humidity);
                        require(upper > lower, "Local detail does not vary the mountain surface");
                        require(upper - lower < lower * 0.25, "Local detail overwhelms the mountain body");
                        minimum = Math.min(minimum, lower);
                        maximum = Math.max(maximum, upper);
                        double boundary = MountainProfile.height(1e-8, broad, main, secondary, 1, 1,
                                temperature, humidity);
                        require(boundary >= 0 && boundary < 1e-4, "Mountain boundary has a height step");
                    }
                }
            }
        }
        // At equal regional influence, valleys must retain most of the massif rather than collapse.
        require(minimum > maximum * 0.5, "Ridge valleys remove the substantial mountain body");
    }

    /** Direct production-profile checks separate climate eligibility from contour deformation. */
    private static void verifyCliffs() {
        int dryPatches = 0, wetPatches = 0, frostPatches = 0;
        for (int i = -1000; i <= 1000; i++) {
            double patch = i / 1000.0;
            double dry = CliffProfile.strength(1, 0.3, -0.8, patch);
            double wet = CliffProfile.strength(1, 0.3, 0.8, patch);
            double frost = CliffProfile.strength(1, -0.25, 0.8, patch);
            require(dry >= wet && frost >= wet, "Weathering does not increase cliff strength");
            if (dry > 0) dryPatches++;
            if (wet > 0) wetPatches++;
            if (frost > 0) frostPatches++;
            require(CliffProfile.strength(0, 0.3, -0.8, patch) == 0, "Cliffs escape mountain regions");
        }
        require(dryPatches > wetPatches && frostPatches > wetPatches && wetPatches > 0,
                "Climate does not vary cliff frequency");
        require(dryPatches < 2001 && frostPatches < 2001, "Favorable climate activates every cliff patch");

        TerrainColumn column = new TerrainColumn();
        for (double strength : new double[]{0, 1e-8, 0.25, 1}) {
            for (double local : new double[]{-1, 0, 1}) for (double secondary : new double[]{-1, 0, 1}) {
                for (int i = -1000; i <= 1000; i++) {
                    double noise = i / 1000.0;
                    double height = CliffProfile.height(strength, noise, local, secondary, column);
                    require(Double.isFinite(height) && Math.abs(height) <= 24 * strength,
                            "Cliff relief can overwhelm the mountain body");
                    require(!column.exposedRock || strength > 0, "Inactive cliff retains rock exposure");
                }
                require(CliffProfile.height(strength, -1, local, secondary, null) == 0
                        && CliffProfile.height(strength, 1, local, secondary, null) == 0,
                        "Cliff deformation does not return to the original body beyond its shoulders");
            }
        }
        column.exposedRock = true;
        require(CliffProfile.height(0, 0, 0, 0, column) == 0 && !column.exposedRock,
                "Inactive cliff does not reset the output buffer");
        require(CliffProfile.height(1, -0.1, 0, 0, null) < 0
                        && CliffProfile.height(1, 0.1, 0, 0, null) > 0,
                "Cliff does not displace both sides of the contour");
        double face = CliffProfile.height(1, 0.02, 0, 0, null) - CliffProfile.height(1, -0.02, 0, 0, null);
        double shoulder = CliffProfile.height(1, 0.44, 0, 0, null) - CliffProfile.height(1, 0.40, 0, 0, null);
        require(face > Math.abs(shoulder), "Cliff face is no steeper than its shoulders");
        require(CliffProfile.height(1, 0, -1, 0, null) != CliffProfile.height(1, 0, 1, 0, null),
                "Local noise does not break up the cliff contour");
        System.out.printf("Cliff eligibility: dry=%d coolMoist=%d warmMoist=%d of 2001 patch inputs%n",
                dryPatches, frostPatches, wetPatches);
    }

    /** Cliff heads/feet retain a transition, and fading ends soften faster than the central rock face. */
    private static void verifyCliffTransitions() {
        double center = CliffProfile.height(1, 0.02, 0, 0, null)
                - CliffProfile.height(1, -0.02, 0, 0, null);
        double transition = CliffProfile.height(1, 0.12, 0, 0, null)
                - CliffProfile.height(1, -0.12, 0, 0, null);
        require(center > 0 && center < transition * 0.75,
                "Cliff consumes nearly all relief in its central strip without a head/foot transition");
        double fadingCenter = (CliffProfile.height(0.2, 0.02, 0, 0, null)
                - CliffProfile.height(0.2, -0.02, 0, 0, null)) / 0.2;
        require(fadingCenter < center, "Cliff ends only shrink in height instead of easing into the slope");
    }

    private static void survey(long seed) {
        TerrainSampler sampler = new TerrainSampler(seed);
        TerrainColumn column = new TerrainColumn(), copied = new TerrainColumn();
        TerrainChunk chunk = new TerrainChunk();
        int rocks = 0, mountains = 0, maximum = 0, targetX = 0, targetZ = 0, rockHeight = 0, peakX = 0, peakZ = 0;
        long nearest = Long.MAX_VALUE;
        for (int z = -384; z < 384; z++) for (int x = -384; x < 384; x++) {
            int worldX = x * 8, worldZ = z * 8;
            sampler.sampleInto(worldX, worldZ, column);
            require(column.height() == sampler.sampleHeight(worldX, worldZ), "Height-only mountain path differs");
            if (column.mountain()) mountains++;
            if (column.height() > maximum) { maximum = column.height(); peakX = worldX; peakZ = worldZ; }
            if (column.exposedRock()) {
                rocks++;
                long distance = (long) worldX * worldX + (long) worldZ * worldZ;
                if (distance < nearest && column.height() >= 80) {
                    nearest = distance; targetX = worldX; targetZ = worldZ; rockHeight = column.height();
                }
            }
        }
        require(rocks > 0 && mountains > 0, "Survey found no mountains or exposed cliffs");
        sampler.sampleChunkInto(targetX >> 4, targetZ >> 4, chunk);
        chunk.copyColumn((targetX & 15) + (targetZ & 15) * 16, copied);
        require(copied.exposedRock() && copied.snapshot().equals(sampler.sample(targetX, targetZ)),
                "Exposed-rock state lost through chunk/snapshot APIs");
        System.out.printf("Mountain survey: seed=%d columns=589824 mountains=%d cliffs=%d peak=%d,%d,%d cliff=%d,%d,%d%n",
                seed, mountains, rocks, peakX, maximum, peakZ, targetX, rockHeight, targetZ);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
