/*
 * Copyright (c) by Stefan Feldbinder aka Two
 */
package two.newdawn;

import net.minecraft.src.IChunkProvider;
import net.minecraft.src.World;
import net.minecraft.src.WorldType;

/**
 *
 * @author Two
 */
public class NewDawnWorldType extends WorldType {

  public static NewDawnWorldType worldType;

  public static void register() {
    for (int i = WorldType.worldTypes.length - 1; i >= 3; --i) {
      if (WorldType.worldTypes[i] == null) {
//    int i = 0; // hack to make this the default world
        worldType = new NewDawnWorldType(i, "newDawn");
        WorldType.worldTypes[i] = worldType;
      }
    }
  }

  public NewDawnWorldType(int par1, String par2Str) {
    this(par1, par2Str, 0);
  }

  public NewDawnWorldType(int par1, String par2Str, int par3) {
    super(par1, par2Str, par3);
  }

  @Override
  public String getTranslateName() {
    return "New Dawn";
  }

  @Override
  public IChunkProvider getChunkGenerator(World world) {
    return new NewDawnTerrainGenerator(world, world.getSeed(), world.getWorldInfo().isMapFeaturesEnabled());
  }
}
