package two.newdawn.terrain;

/**
 * Caller-owned mutable output for allocation-free point queries. Reuse sequentially or keep one per worker;
 * never read or write the same instance concurrently. Getters reject an output that has not been sampled.
 * Use snapshot() when a result must outlive the next write to this buffer.
 */
public final class TerrainColumn {
    int height;
    int regionHeight;
    boolean mountain;
    boolean exposedRock;
    float temperature;
    float humidity;
    int fillerDepth;
    float biomeHeightOffset;

    public int height() { requireSampled(); return height; }
    public int regionHeight() { requireSampled(); return regionHeight; }
    public boolean mountain() { requireSampled(); return mountain; }
    /** True on climate-shaped cliff faces; surface layers should expose underlying rock. */
    public boolean exposedRock() { requireSampled(); return exposedRock; }
    public float temperature() { requireSampled(); return temperature; }
    public float humidity() { requireSampled(); return humidity; }
    public int fillerDepth() { requireSampled(); return fillerDepth; }
    /** Local offset in blocks for upland biome bands; it does not alter terrain height. */
    public float biomeHeightOffset() { requireSampled(); return biomeHeightOffset; }

    public TerrainSample snapshot() {
        requireSampled();
        return new TerrainSample(height, regionHeight, mountain, temperature, humidity, fillerDepth, exposedRock, biomeHeightOffset);
    }

    private void requireSampled() {
        if (height == 0) throw new IllegalStateException("Terrain column has not been sampled");
    }
}
