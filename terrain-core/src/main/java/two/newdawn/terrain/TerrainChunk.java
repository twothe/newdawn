package two.newdawn.terrain;

import java.util.Objects;

/**
 * Caller-owned 16x16 terrain buffer backed by primitive arrays, indexed by local X + 16 * local Z.
 * Repeated sampleChunkInto calls overwrite all columns without allocating. Like an array, a buffer
 * must have one writer at a time; completed buffers may be safely published to readers by the caller.
 */
public final class TerrainChunk {
    public static final int SIDE = 16;
    public static final int COLUMN_COUNT = SIDE * SIDE;
    private final int[] heights = new int[COLUMN_COUNT];
    private final int[] regionHeights = new int[COLUMN_COUNT];
    private final boolean[] mountains = new boolean[COLUMN_COUNT];
    private final boolean[] exposedRocks = new boolean[COLUMN_COUNT];
    private final float[] temperatures = new float[COLUMN_COUNT];
    private final float[] humidities = new float[COLUMN_COUNT];
    private final int[] fillerDepths = new int[COLUMN_COUNT];
    private final float[] biomeHeightOffsets = new float[COLUMN_COUNT];
    final TerrainColumn workspace = new TerrainColumn();

    public int height(int index) { requireSampled(index); return heights[index]; }
    public int regionHeight(int index) { requireSampled(index); return regionHeights[index]; }
    public boolean mountain(int index) { requireSampled(index); return mountains[index]; }
    public boolean exposedRock(int index) { requireSampled(index); return exposedRocks[index]; }
    public float temperature(int index) { requireSampled(index); return temperatures[index]; }
    public float humidity(int index) { requireSampled(index); return humidities[index]; }
    public int fillerDepth(int index) { requireSampled(index); return fillerDepths[index]; }
    public float biomeHeightOffset(int index) { requireSampled(index); return biomeHeightOffsets[index]; }

    /** Copies into an independent reusable column; no reference to this buffer escapes. */
    public void copyColumn(int index, TerrainColumn target) {
        Objects.requireNonNull(target, "target");
        requireSampled(index);
        target.height = heights[index];
        target.regionHeight = regionHeights[index];
        target.mountain = mountains[index];
        target.exposedRock = exposedRocks[index];
        target.temperature = temperatures[index];
        target.humidity = humidities[index];
        target.fillerDepth = fillerDepths[index];
        target.biomeHeightOffset = biomeHeightOffsets[index];
    }

    /** Maximum first-air height; useful for skipping empty sections above the terrain. */
    public int maximumHeight() {
        requireSampled(COLUMN_COUNT - 1);
        int maximum = 0;
        for (int height : heights) maximum = Math.max(maximum, height);
        return maximum;
    }

    void store(int index, TerrainColumn column) {
        heights[index] = column.height;
        regionHeights[index] = column.regionHeight;
        mountains[index] = column.mountain;
        exposedRocks[index] = column.exposedRock;
        temperatures[index] = column.temperature;
        humidities[index] = column.humidity;
        fillerDepths[index] = column.fillerDepth;
        biomeHeightOffsets[index] = column.biomeHeightOffset;
    }

    private void requireSampled(int index) {
        Objects.checkIndex(index, COLUMN_COUNT);
        if (heights[index] == 0) throw new IllegalStateException("Terrain chunk has not been sampled");
    }
}
