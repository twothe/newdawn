/*
 */
package two.newdawn.worldgen.thaumcraft;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.biome.BiomeGenBase;
import two.newdawn.API.NewDawnBiome;
import two.newdawn.API.NewDawnBiomeProvider;
import two.newdawn.API.NewDawnBiomeSelector;
import two.newdawn.API.noise.SimplexNoise;
import two.newdawn.Config;
import two.newdawn.NewDawn;

/**
 * @author Two
 */
public class ThaumcraftBiomeProvider implements NewDawnBiomeProvider {

  protected static ThaumcraftConfiguration thaumcraftConfiguration;

  public static void prepareThaumcraftSupport(final Config config) {
    final double thaumcraftThresholdGood = config.getMiscDouble("Thaumcraft threshold good", 0.80, "Threshold value [0,1] for world generation at which point an area is considered good.\nHigher = less magical forest, lower = more magical forest.\n1.0 will effectively disable magical forests.");
    final double thaumcraftThresholdEvil = config.getMiscDouble("Thaumcraft threshold evil", -0.80, "Threshold value [-1,0] for world generation at which point an area is considered evil.\nHigher = more eerie, lower = less eerie.\n-1.0 will effectively disable eerie.");
    final double thaumcraftThresholdTaint = config.getMiscDouble("Thaumcraft threshold taint", -0.92, "Threshold value [-1,evil] for world generation at which point an area is considered tainted.\nHigher = more taint, lower = less taint.\n-1.0 will effectively disable taint.");

    if ((thaumcraftThresholdEvil < -1.0) || (thaumcraftThresholdEvil > 0.0)) {
      throw new IllegalArgumentException("Thaumcraft threshold for evil must be within -1 and 0, but is: " + Double.toString(thaumcraftThresholdEvil));
    }
    if ((thaumcraftThresholdTaint < -1.0) || (thaumcraftThresholdTaint > thaumcraftThresholdEvil)) {
      throw new IllegalArgumentException("Thaumcraft threshold for taint must be within -1 and evil threshold, but is: " + Double.toString(thaumcraftThresholdTaint));
    }
    if ((thaumcraftThresholdGood < 0.0) || (thaumcraftThresholdGood > 1.0)) {
      throw new IllegalArgumentException("Thaumcraft threshold for good must be within 0 and 1, but is: " + Double.toString(thaumcraftThresholdGood));
    }
    if (thaumcraftThresholdEvil >= thaumcraftThresholdGood) {
      throw new IllegalArgumentException("Thaumcraft threshold for good must be > threshold for evil");
    }

    thaumcraftConfiguration = new ThaumcraftConfiguration(thaumcraftThresholdTaint, thaumcraftThresholdEvil, thaumcraftThresholdGood);
  }

  public ThaumcraftBiomeProvider() {
  }

  protected BiomeGenBase getBiome(final Class clazz, final String fieldName) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
    final Field field = clazz.getField(fieldName);
    final Object o = field.get(null);
    if (o instanceof BiomeGenBase) {
      return (BiomeGenBase) o;
    } else {
      throw new IllegalArgumentException("Requested field " + fieldName + " is not of type BiomeGenBase");
    }
  }

  @Override
  public Set<NewDawnBiomeSelector> getBiomeSelectors(final SimplexNoise worldNoise) {
    final HashSet<NewDawnBiomeSelector> result = new HashSet<NewDawnBiomeSelector>();

    try {
      final Class ThaumcraftWorldGeneratorClass = Class.forName("thaumcraft.common.lib.world.ThaumcraftWorldGenerator");
      final BiomeGenBase biomeEerie = getBiome(ThaumcraftWorldGeneratorClass, "biomeEerie");
      final BiomeGenBase biomeTaint = getBiome(ThaumcraftWorldGeneratorClass, "biomeTaint");
      final BiomeGenBase biomeMagicalForest = getBiome(ThaumcraftWorldGeneratorClass, "biomeMagicalForest");

      result.add(new ThaumcraftBiomeSelector(worldNoise, PRIORITY_MEDIUM / 2, thaumcraftConfiguration, NewDawnBiome.copyVanilla(biomeTaint), NewDawnBiome.copyVanilla(biomeEerie), NewDawnBiome.copyVanilla(biomeMagicalForest)));
    } catch (ClassNotFoundException ex) {
      NewDawn.log.error("Unable to find Thaumcraft world generator, Thaumcraft support not possible: %s", ex.toString());
    } catch (NoSuchFieldException ex) {
      NewDawn.log.error("Unable to find Thaumcraft biome field, Thaumcraft support not possible: %s", ex.toString());
    } catch (SecurityException ex) {
      NewDawn.log.error("Access Thaumcraft biome field denied, Thaumcraft support not possible: %s", ex.toString());
    } catch (IllegalArgumentException ex) {
      NewDawn.log.error("Thaumcraft biome field is of unknown type, Thaumcraft support not possible: %s", ex.toString());
    } catch (IllegalAccessException ex) {
      NewDawn.log.error("Unable to access Thaumcraft biome field, Thaumcraft support not possible: %s", ex.toString());
    }

    return result;
  }
}
