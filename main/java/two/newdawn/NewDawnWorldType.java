/*
 * Copyright (c) by Stefan Feldbinder aka Two
 */
package two.newdawn;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import two.newdawn.worldgen.NewDawnTerrainGenerator;

/**
 *
 * @author Two
 */
public class NewDawnWorldType extends WorldType {
  public NewDawnWorldType() {
    super(NewDawn.MOD_NAME);
  }


  @Override
  public String getTranslateName() {
    return NewDawn.MOD_NAME;
  }

  @Override
  public IChunkProvider getChunkGenerator(World world, String generatorOptions) {
    return new NewDawnTerrainGenerator(world, world.getSeed(), world.getWorldInfo().isMapFeaturesEnabled());
  }
}
