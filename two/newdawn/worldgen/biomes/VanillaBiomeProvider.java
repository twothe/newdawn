/*
 */
package two.newdawn.worldgen.biomes;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.world.biome.BiomeGenBase;
import two.newdawn.API.NewDawnBiome;
import two.newdawn.API.NewDawnBiomeProvider;
import static two.newdawn.API.NewDawnBiomeProvider.PRIORITY_LOWEST;
import two.newdawn.API.NewDawnBiomeSelector;
import two.newdawn.API.noise.SimplexNoise;

/**
 * @author Two
 */
public class VanillaBiomeProvider implements NewDawnBiomeProvider {
  // cold

  public static final NewDawnBiome biomeIcePlains = NewDawnBiome.copyVanilla(BiomeGenBase.icePlains);
  public static final NewDawnBiome biomeFrozenGravelBeach = new NewDawnBiome(BiomeGenBase.icePlains, Block.gravel.blockID, Block.stone.blockID);
  public static final NewDawnBiome biomeFrozenBeach = new NewDawnBiome(BiomeGenBase.icePlains, Block.sand.blockID, Block.sand.blockID);
  public static final NewDawnBiome biomeFrozenOcean = NewDawnBiome.copyVanilla(BiomeGenBase.frozenOcean);
  public static final NewDawnBiome biomeIceMountains = NewDawnBiome.copyVanilla(BiomeGenBase.iceMountains);
  public static final NewDawnBiome biomeIceRockyMountains = new NewDawnBiome(BiomeGenBase.iceMountains, Block.stone.blockID, Block.stone.blockID);
  public static final NewDawnBiome biomeTaiga = NewDawnBiome.copyVanilla(BiomeGenBase.taiga);
  public static final NewDawnBiome biomeTaigaHills = NewDawnBiome.copyVanilla(BiomeGenBase.taigaHills);
  // temperate
  public static final NewDawnBiome biomeGrassPlains = NewDawnBiome.copyVanilla(BiomeGenBase.plains);
  public static final NewDawnBiome biomeGravelBeach = new NewDawnBiome(BiomeGenBase.beach, Block.gravel.blockID, Block.stone.blockID);
  public static final NewDawnBiome biomeOcean = new NewDawnBiome(BiomeGenBase.ocean, Block.sand.blockID, Block.sand.blockID);
  public static final NewDawnBiome biomeExtremeHills = NewDawnBiome.copyVanilla(BiomeGenBase.extremeHills);
  public static final NewDawnBiome biomeRockyExtremeHills = new NewDawnBiome(BiomeGenBase.extremeHills, Block.stone.blockID, Block.stone.blockID);
  public static final NewDawnBiome biomeSwampland = NewDawnBiome.copyVanilla(BiomeGenBase.swampland);
  public static final NewDawnBiome biomeForest = NewDawnBiome.copyVanilla(BiomeGenBase.forest);
  public static final NewDawnBiome biomeForestHills = NewDawnBiome.copyVanilla(BiomeGenBase.forestHills);
  public static final NewDawnBiome biomeGrassBeach = new NewDawnBiome(BiomeGenBase.beach, Block.grass.blockID, Block.dirt.blockID);
  // hot
  public static final NewDawnBiome biomeDesert = NewDawnBiome.copyVanilla(BiomeGenBase.desert);
  public static final NewDawnBiome biomeBeach = new NewDawnBiome(BiomeGenBase.beach, Block.sand.blockID, Block.sand.blockID);
  public static final NewDawnBiome biomeMuddyDesert = new NewDawnBiome(BiomeGenBase.desert, Block.hardenedClay.blockID, Block.sand.blockID);
  public static final NewDawnBiome biomeJungle = NewDawnBiome.copyVanilla(BiomeGenBase.jungle);
  public static final NewDawnBiome biomeJungleHills = NewDawnBiome.copyVanilla(BiomeGenBase.jungleHills);
  public static final NewDawnBiome biomeDesertHills = NewDawnBiome.copyVanilla(BiomeGenBase.desertHills);
  public static final NewDawnBiome biomeDesertRockyMountains = new NewDawnBiome(BiomeGenBase.desertHills, Block.stone.blockID, Block.stone.blockID);

  /** Class */
  @Override
  public Set<NewDawnBiomeSelector> getBiomeSelectors(final SimplexNoise worldNoise) {
    final HashSet<NewDawnBiomeSelector> result = new HashSet<NewDawnBiomeSelector>();

    result.add(new VanillaMountainSelector(worldNoise, PRIORITY_LOWEST + 1));
    result.add(new VanillaWoodlandSelector(worldNoise, PRIORITY_LOWEST + 2));
    result.add(new BeachSelector(worldNoise, PRIORITY_LOWEST + 3));
    result.add(new VanillaOceanSelector(worldNoise, PRIORITY_LOWEST + 4));
    result.add(new VanillaPlainsSelector(worldNoise, PRIORITY_LOWEST + 5));

    return result;
  }
}
