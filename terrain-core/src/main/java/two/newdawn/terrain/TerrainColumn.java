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
    float temperature;
    float humidity;
    int fillerDepth;

    public int height() { requireSampled(); return height; }
    public int regionHeight() { requireSampled(); return regionHeight; }
    public boolean mountain() { requireSampled(); return mountain; }
    public float temperature() { requireSampled(); return temperature; }
    public float humidity() { requireSampled(); return humidity; }
    public int fillerDepth() { requireSampled(); return fillerDepth; }

    public TerrainSample snapshot() {
        requireSampled();
        return new TerrainSample(height, regionHeight, mountain, temperature, humidity, fillerDepth);
    }

    private void requireSampled() {
        if (height == 0) throw new IllegalStateException("Terrain column has not been sampled");
    }
}
