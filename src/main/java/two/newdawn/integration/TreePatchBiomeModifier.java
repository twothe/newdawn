package two.newdawn.integration;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

/** Decorates tree placement without replacing biome identities or changing feature encounter order. */
public final class TreePatchBiomeModifier implements BiomeModifier {
    public static final MapCodec<TreePatchBiomeModifier> CODEC = MapCodec.unit(TreePatchBiomeModifier::new);
    public static final TagKey<PlacedFeature> TREE_FEATURES = TagKey.create(Registries.PLACED_FEATURE,
            ResourceLocation.fromNamespaceAndPath("newdawn", "tree_features"));
    // One wrapper per original across ALL biomes preserves shared features and avoids ordering cycles.
    // This cache belongs to the loaded modifier instance, never to a world or a global singleton.
    private final Map<PlacedFeature, Holder<PlacedFeature>> replacements = new IdentityHashMap<>();

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.AFTER_EVERYTHING) return;
        for (var stage : GenerationStep.Decoration.values()) {
            var features = builder.getGenerationSettings().getFeatures(stage);
            for (int index = 0; index < features.size(); index++) {
                var original = features.get(index);
                if (original.value().placement().contains(TreePatchPlacement.INSTANCE)) continue;
                if (original.is(TREE_FEATURES) || original.value().getFeatures()
                        .anyMatch(configured -> configured.feature() instanceof TreeFeature)) {
                    features.set(index, replacement(original.value()));
                }
            }
        }
    }

    private synchronized Holder<PlacedFeature> replacement(PlacedFeature original) {
        return replacements.computeIfAbsent(original, feature -> {
            var placement = new ArrayList<>(feature.placement());
            placement.add(TreePatchPlacement.INSTANCE);
            return Holder.direct(new PlacedFeature(feature.feature(), placement));
        });
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() { return CODEC; }
}
