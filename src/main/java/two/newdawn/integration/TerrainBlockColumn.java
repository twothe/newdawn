package two.newdawn.integration;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import two.newdawn.terrain.BiomePalette;
import two.newdawn.terrain.TerrainSampler;

import java.util.Arrays;

/**
 * Reusable, invocation-owned material layers for one column. Both raw fill and base-column queries
 * consume these same intervals. Preparation selects the surface once; inner block loops write one state.
 */
final class TerrainBlockColumn {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private final int[] ends = new int[7];
    private final BlockState[] states = new BlockState[7];
    private int minimum;
    private int count;
    private int cursor;

    void prepare(int height, int fillerDepth, BiomePalette.Entry palette, int minimumY) {
        minimum = minimumY;
        cursor = minimumY;
        count = 0;
        int fillerStart = height - 1 - fillerDepth;
        if (minimumY < height) addUntil(minimumY + 1, Blocks.BEDROCK.defaultBlockState());
        addUntil(Math.min(fillerStart, 0), Blocks.DEEPSLATE.defaultBlockState());
        addUntil(fillerStart, Blocks.STONE.defaultBlockState());
        if (palette.filler() == BiomePalette.Material.SAND || palette.filler() == BiomePalette.Material.RED_SAND) {
            addUntil((height - 1 + fillerStart) / 2, (palette.filler() == BiomePalette.Material.SAND ? Blocks.SANDSTONE : Blocks.RED_SANDSTONE).defaultBlockState());
        }
        addUntil(height - 1, material(palette.filler()));
        BlockState top = palette.top() == BiomePalette.Material.GRASS && height < TerrainSampler.SEA_LEVEL
                ? Blocks.DIRT.defaultBlockState() : material(palette.top());
        addUntil(height, top);
        addUntil(TerrainSampler.SEA_LEVEL, Blocks.WATER.defaultBlockState());
    }

    /** Writes only within [lower, upper). The caller owns the chunk and holds the affected section locks. */
    void fill(ChunkAccess chunk, int x, int z, int lower, int upper) {
        int start = minimum;
        for (int layer = 0; layer < count; layer++) {
            int y = Math.max(start, lower);
            int end = Math.min(ends[layer], upper);
            while (y < end) {
                var section = chunk.getSection(chunk.getSectionIndex(y));
                int sectionEnd = Math.min(end, (y | 15) + 1);
                for (; y < sectionEnd; y++) section.setBlockState(x, y & 15, z, states[layer], false);
            }
            start = ends[layer];
        }
    }

    BlockState stateAt(int y) {
        if (y >= minimum) {
            for (int layer = 0; layer < count; layer++) if (y < ends[layer]) return states[layer];
        }
        return AIR;
    }

    BlockState[] toStates(int lower, int upper) {
        BlockState[] result = new BlockState[upper - lower];
        Arrays.fill(result, AIR);
        int start = minimum;
        for (int layer = 0; layer < count; layer++) {
            int from = Math.max(start, lower), to = Math.min(ends[layer], upper);
            if (from < to) Arrays.fill(result, from - lower, to - lower, states[layer]);
            start = ends[layer];
        }
        return result;
    }

    private void addUntil(int end, BlockState state) {
        if (end > cursor) {
            ends[count] = end;
            states[count++] = state;
            cursor = end;
        }
    }

    private static BlockState material(BiomePalette.Material material) {
        return switch (material) {
            case GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
            case DIRT -> Blocks.DIRT.defaultBlockState();
            case STONE -> Blocks.STONE.defaultBlockState();
            case GRAVEL -> Blocks.GRAVEL.defaultBlockState();
            case SAND -> Blocks.SAND.defaultBlockState();
            case TERRACOTTA -> Blocks.TERRACOTTA.defaultBlockState();
            case RED_SAND -> Blocks.RED_SAND.defaultBlockState();
            case MYCELIUM -> Blocks.MYCELIUM.defaultBlockState();
            case PODZOL -> Blocks.PODZOL.defaultBlockState();
            case MUD -> Blocks.MUD.defaultBlockState();
            case SNOW_BLOCK -> Blocks.SNOW_BLOCK.defaultBlockState();
            case PACKED_ICE -> Blocks.PACKED_ICE.defaultBlockState();
        };
    }
}
