/*
 */
package two.newdawn.worldgen.biomes.modsupport.thaumcraft;

import java.util.HashSet;
import java.util.Set;
import two.newdawn.API.NewDawnBiomeProvider;
import two.newdawn.API.NewDawnBiomeSelector;
import two.newdawn.API.noise.SimplexNoise;

/**
 * @author Two
 */
public class ThaumcraftBiomeProvider implements NewDawnBiomeProvider {

  @Override
  public Set<NewDawnBiomeSelector> getBiomeSelectors(final SimplexNoise worldNoise) {
    final HashSet<NewDawnBiomeSelector> result = new HashSet<NewDawnBiomeSelector>();
    result.add(new ThaumcraftSelector(worldNoise, PRIORITY_MEDIUM));
    return result;
  }
}
