/*
 */
package two.newdawn.worldgen.vanilla;

import java.util.HashSet;
import java.util.Set;
import two.newdawn.API.NewDawnBiomeProvider;
import static two.newdawn.API.NewDawnBiomeProvider.PRIORITY_LOWEST;
import two.newdawn.API.NewDawnBiomeSelector;
import two.newdawn.API.noise.SimplexNoise;

/**
 * @author Two
 */
public class VanillaBiomeProvider implements NewDawnBiomeProvider {

  /** Class */
  @Override
  public Set<NewDawnBiomeSelector> getBiomeSelectors(final SimplexNoise worldNoise) {
    final HashSet<NewDawnBiomeSelector> result = new HashSet<NewDawnBiomeSelector>();

    result.add(new VanillaMountainSelector(worldNoise, PRIORITY_LOWEST - 1));
    result.add(new VanillaBeachSelector(worldNoise, PRIORITY_LOWEST - 2));
    result.add(new VanillaOceanSelector(worldNoise, PRIORITY_LOWEST - 3));
    result.add(new VanillaBaseSelector(worldNoise, PRIORITY_LOWEST - 4));

    return result;
  }
}
