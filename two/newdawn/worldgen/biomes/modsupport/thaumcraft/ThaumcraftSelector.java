/*
 */
package two.newdawn.worldgen.biomes.modsupport.thaumcraft;

import cpw.mods.fml.common.FMLLog;
import net.minecraft.world.biome.BiomeGenBase;
import two.newdawn.API.ChunkInformation;
import two.newdawn.API.NewDawnBiome;
import two.newdawn.API.NewDawnBiomeSelector;
import two.newdawn.API.noise.NoiseStretch;
import two.newdawn.API.noise.SimplexNoise;

/**
 * @author Two
 */
class ThaumcraftSelector extends NewDawnBiomeSelector {

  protected static final String TC_BIOME_PACKAGE_NAME = "thaumcraft.common.lib.world.biomes";
  public static final double THRESHOLD_HIGH_MAGIC = 0.60;
  public static final double THRESHOLD_LOW_MAGIC = -0.60;
  public static final double THRESHOLD_TAINT_MAGIC = -0.85;
  public final NewDawnBiome biomeTaint;
  public final NewDawnBiome biomeEerie;
  public final NewDawnBiome biomeMagicalForest;
  protected final NoiseStretch magicLevelStretch;
  protected final NoiseStretch magicLevelStretchBlock;

  public ThaumcraftSelector(final SimplexNoise worldNoise, final int priority) {
    super(worldNoise, priority);
    magicLevelStretch = worldNoise.generateNoiseStretcher(1024.0, 1011.0, worldNoise.getRandom().nextDouble(), worldNoise.getRandom().nextDouble());
    magicLevelStretchBlock = worldNoise.generateNoiseStretcher(2.1, 2.2, worldNoise.getRandom().nextDouble(), worldNoise.getRandom().nextDouble());

    FMLLog.info("Thaumcraft compatibility activated.");
    NewDawnBiome taint = null, eerie = null, magicalForest = null;
    for (final BiomeGenBase biome : BiomeGenBase.biomeList) {
      if (biome != null) {
        if (biome.getClass().getSimpleName().equals("BiomeGenEerie")) {
          FMLLog.info("|-Found Thaumcraft biome %s", biome.getClass().getSimpleName());
          eerie = NewDawnBiome.copyVanilla(biome);
        } else if (biome.getClass().getSimpleName().equals("BiomeGenTaint")) {
          FMLLog.info("|-Found Thaumcraft biome %s", biome.getClass().getSimpleName());
          taint = NewDawnBiome.copyVanilla(biome);
        } else if (biome.getClass().getSimpleName().equals("BiomeGenMagicalForest")) {
          FMLLog.info("|-Found Thaumcraft biome %s", biome.getClass().getSimpleName());
          magicalForest = NewDawnBiome.copyVanilla(biome);
        }
      }
    }

    this.biomeEerie = eerie;
    this.biomeTaint = taint;
    this.biomeMagicalForest = magicalForest;
  }

  @Override
  public NewDawnBiome selectBiome(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    if (chunkInfo.isWoodland(blockX, blockZ) && (chunkInfo.isAboveSeaLevel(blockX, blockZ) || chunkInfo.isShallowWater(blockX, blockZ))) {
      final double magicLevel = getMagicLevel(blockX, blockZ, chunkInfo);
      if (magicLevel > THRESHOLD_HIGH_MAGIC) {
        return biomeMagicalForest;
      } else if (magicLevel < THRESHOLD_TAINT_MAGIC) {
        return biomeTaint;
      } else if (magicLevel < THRESHOLD_LOW_MAGIC) {
        return biomeEerie;
      }
    }
    return null;
  }

  protected double getMagicLevel(final int blockX, final int blockZ, final ChunkInformation chunkInfo) {
    return magicLevelStretch.getNoise(blockX, blockZ) * 0.95 + magicLevelStretchBlock.getNoise(blockX, blockZ) * 0.05;
  }
}
