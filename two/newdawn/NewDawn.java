/*
 * Copyright (c) by Stefan Feldbinder aka Two
 */
package two.newdawn;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;

/**
 *
 * @author Two
 */
@Mod(modid = "newdawn", name = "New Dawn", version = "131212")
@NetworkMod(clientSideRequired = false, serverSideRequired = false)
public class NewDawn {

  @Mod.Instance("NewDawn")
  public static NewDawn instance;

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
  }

  @Mod.EventHandler
  public void load(FMLInitializationEvent event) {
    NewDawnWorldType.register();
  }

  @Mod.EventHandler
  public void postInit(FMLPostInitializationEvent event) {
  }
}
