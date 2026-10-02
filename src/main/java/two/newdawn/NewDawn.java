package two.newdawn;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import two.newdawn.integration.NewDawnBiomeSource;
import two.newdawn.integration.NewDawnChunkGenerator;
import two.newdawn.integration.TreePatchBiomeModifier;
import two.newdawn.integration.TreePatchPlacement;

/** Loader entry point: registers generation codecs and the biome decoration modifier through NeoForge. */
@Mod(NewDawn.MOD_ID)
public final class NewDawn {
    public static final String MOD_ID = "newdawn";

    public NewDawn(IEventBus modBus) {
        DeferredRegister<MapCodec<? extends BiomeSource>> biomes = DeferredRegister.create(Registries.BIOME_SOURCE, MOD_ID);
        DeferredRegister<MapCodec<? extends ChunkGenerator>> generators = DeferredRegister.create(Registries.CHUNK_GENERATOR, MOD_ID);
        biomes.register("climate", () -> NewDawnBiomeSource.CODEC);
        generators.register("terrain", () -> NewDawnChunkGenerator.CODEC);
        DeferredRegister<MapCodec<? extends BiomeModifier>> modifiers = DeferredRegister.create(
                NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MOD_ID);
        DeferredRegister<PlacementModifierType<?>> placements = DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, MOD_ID);
        modifiers.register("tree_patches", () -> TreePatchBiomeModifier.CODEC);
        placements.register("tree_patch", () -> TreePatchPlacement.TYPE);
        modifiers.register(modBus);
        placements.register(modBus);
        biomes.register(modBus);
        generators.register(modBus);
    }
}
