package two.newdawn.integration;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.WorldPresetTags;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.Properties;

/** Development-only checks of the actual client default and server preset resolution paths. */
final class SmokePresetChecks {
    private SmokePresetChecks() {}

    static void verify(ServerLevel level) {
        var registries = level.registryAccess();
        var presets = registries.registryOrThrow(Registries.WORLD_PRESET);
        var vanilla = presets.getHolder(ResourceLocation.fromNamespaceAndPath("newdawn", "vanilla")).orElseThrow();
        var explicit = presets.getHolder(ResourceLocation.fromNamespaceAndPath("newdawn", "new_dawn")).orElseThrow();
        require(WorldPresets.createNormalWorldDimensions(registries).overworld() instanceof NewDawnChunkGenerator,
                "Fresh client world would not default to New Dawn");
        require(new DedicatedServerProperties(new Properties()).createDimensions(registries).overworld() instanceof NewDawnChunkGenerator,
                "Server without level-type would not default to New Dawn");
        require(explicit.value().createWorldDimensions().overworld() instanceof NewDawnChunkGenerator,
                "Explicit New Dawn preset lost compatibility");
        var menu = presets.getTagOrEmpty(WorldPresetTags.NORMAL);
        boolean vanillaVisible = false, defaultVisible = false;
        for (var preset : menu) {
            vanillaVisible |= preset.equals(vanilla);
            defaultVisible |= preset.is(WorldPresets.NORMAL);
        }
        require(vanillaVisible && defaultVisible, "Menu lost New Dawn or Vanilla selection");
        Properties properties = new Properties();
        properties.setProperty("level-type", "newdawn:vanilla");
        var vanillaGenerator = new DedicatedServerProperties(properties).createDimensions(registries).overworld();
        require(vanillaGenerator.getClass() == NoiseBasedChunkGenerator.class, "Explicit Vanilla selection uses a custom generator");
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        var json = ChunkGenerator.CODEC.encodeStart(ops, vanillaGenerator).getOrThrow().getAsJsonObject();
        require(json.get("settings").getAsString().equals("minecraft:overworld"), "Vanilla noise settings changed");
        require(json.getAsJsonObject("biome_source").get("preset").getAsString().equals("minecraft:overworld"),
                "Vanilla biome preset changed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
