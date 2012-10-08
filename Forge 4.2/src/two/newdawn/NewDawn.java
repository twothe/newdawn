/*
 * Copyright (c) by Stefan Feldbinder aka Two
 */
package two.newdawn;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;

/**
 *
 * @author Two
 */
@Mod(modid="newdawn", name="New Dawn", version="121008")
@NetworkMod(clientSideRequired=true, serverSideRequired=false)
public class NewDawn {
  
  @Mod.Instance("NewDawn")
  public static NewDawn instance;
  
  @Mod.Init
  public void init(final FMLInitializationEvent event) {
    NewDawnWorldType.register();
  }
}
