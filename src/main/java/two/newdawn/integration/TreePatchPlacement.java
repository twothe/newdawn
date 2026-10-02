package two.newdawn.integration;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import two.newdawn.terrain.BiomePalette;

/** Applies dry/wooded Savanna bands and woodland patches in other open biomes, only in New Dawn worlds. */
public final class TreePatchPlacement extends PlacementFilter {
    public static final TreePatchPlacement INSTANCE = new TreePatchPlacement();
    public static final MapCodec<TreePatchPlacement> CODEC = MapCodec.unit(INSTANCE);
    public static final PlacementModifierType<TreePatchPlacement> TYPE = () -> CODEC;
    public static final TagKey<Biome> OPEN_VEGETATION = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("newdawn", "open_vegetation"));

    public static final TagKey<Biome> SAVANNA_VEGETATION = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("newdawn", "savanna_vegetation"));

    private TreePatchPlacement() {}

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos position) {
        if (!(context.generator() instanceof NewDawnChunkGenerator generator)) return true;
        var biome = context.getLevel().getBiome(position);
        var terrain = ((NewDawnBiomeSource) generator.getBiomeSource()).terrain();
        if (biome.is(SAVANNA_VEGETATION)) {
            return BiomePalette.allowsSavannaTrees(terrain.sampleHumidity(position.getX(), position.getZ()));
        }
        return !biome.is(OPEN_VEGETATION) || terrain.isForestPatch(position.getX(), position.getZ());
    }

    @Override
    public PlacementModifierType<?> type() { return TYPE; }
}
