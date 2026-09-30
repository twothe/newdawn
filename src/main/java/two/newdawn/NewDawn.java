package two.newdawn;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import two.newdawn.integration.NewDawnBiomeSource;
import two.newdawn.integration.NewDawnChunkGenerator;

/** Loader entry point: only codec registration depends on NeoForge. */
@Mod(NewDawn.MOD_ID)
public final class NewDawn {
    public static final String MOD_ID = "newdawn";

    public NewDawn(IEventBus modBus) {
        DeferredRegister<MapCodec<? extends BiomeSource>> biomes = DeferredRegister.create(Registries.BIOME_SOURCE, MOD_ID);
        DeferredRegister<MapCodec<? extends ChunkGenerator>> generators = DeferredRegister.create(Registries.CHUNK_GENERATOR, MOD_ID);
        biomes.register("climate", () -> NewDawnBiomeSource.CODEC);
        generators.register("terrain", () -> NewDawnChunkGenerator.CODEC);
        biomes.register(modBus);
        generators.register(modBus);
    }
}
