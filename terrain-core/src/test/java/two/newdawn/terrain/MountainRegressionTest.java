package two.newdawn.terrain;

/** Functional mountain-profile contracts and seeded production surveys without fixed visual snapshots. */
public final class MountainRegressionTest {
    public static void main(String[] args) {
        verifyProfile();
        verifyBodyAndDetail();
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
        TerrainColumn column = new TerrainColumn();
        double dryDrop = profile(0.28, 0.2, -0.8) - profile(0.36, 0.2, -0.8);
        double wetDrop = profile(0.28, 0.2, 0.8) - profile(0.36, 0.2, 0.8);
        require(dryDrop > wetDrop, "Dry climate does not strengthen the cliff face");
        column.exposedRock = true;
        require(MountainProfile.height(0, 0.5, 0.32, 0, 1, 1, 0.2, -0.8, column) == 0 && !column.exposedRock,
                "Zero mountain influence leaves relief or stale rock data");
        for (double temperature : new double[]{-1, -0.25, 0.2, 1}) {
            for (double humidity : new double[]{-1, 0, 1}) {
                for (int i = -9999; i <= 10000; i++) {
                    double height = MountainProfile.height(1, 0.5, i / 10000.0, 0.2, 0, 0,
                            temperature, humidity, null);
                    require(Double.isFinite(height) && height >= 0, "Invalid mountain relief");
                }
            }
        }
    }

    private static double profile(double large, double temperature, double humidity) {
        return MountainProfile.height(1, 0.5, large, 0.2, 0, 0, temperature, humidity, null);
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
                                temperature, humidity, null);
                        double upper = MountainProfile.height(1, broad, main, secondary, 1, 1,
                                temperature, humidity, null);
                        require(upper > lower, "Local detail does not vary the mountain surface");
                        require(upper - lower < lower * 0.25, "Local detail overwhelms the mountain body");
                        minimum = Math.min(minimum, lower);
                        maximum = Math.max(maximum, upper);
                        double boundary = MountainProfile.height(1e-8, broad, main, secondary, 1, 1,
                                temperature, humidity, null);
                        require(boundary >= 0 && boundary < 1e-4, "Mountain boundary has a height step");
                    }
                }
            }
        }
        // At equal regional influence, valleys must retain most of the massif rather than collapse.
        require(minimum > maximum * 0.5, "Ridge valleys remove the substantial mountain body");
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
