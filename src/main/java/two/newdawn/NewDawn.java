/*
 * Copyright (c) by Stefan Feldbinder aka Two
 */
package two.newdawn;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;
import two.newdawn.API.NewDawnRegistry;
import two.newdawn.commands.CommandGenerateWorld;
import two.newdawn.worldgen.thaumcraft.ThaumcraftBiomeProvider;
import two.newdawn.worldgen.vanilla.VanillaBiomeProvider;

/**
 *
 * @author Two
 */
@Mod(modid = NewDawn.MOD_ID, name = NewDawn.MOD_NAME, version = NewDawn.MOD_VERSION)
public class NewDawn {

  public static final String MOD_NAME = "New Dawn";
  public static final String MOD_ID = "newdawn";
  public static final String MOD_VERSION = "1710.1.0";
  /* Global logger that uses string format type logging */
  public static final Logger log = LogManager.getLogger(NewDawn.class.getSimpleName(), new StringFormatterMessageFactory());

  @Mod.Instance("NewDawn")
  public static NewDawn instance;
  public static final Config config = new Config();
  protected NewDawnWorldType worldType;
  protected boolean thaumcraftSupportEnabled;

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    config.initialize(event.getSuggestedConfigurationFile());
  }

  @Mod.EventHandler
  public void load(FMLInitializationEvent event) {
    config.load();
    this.worldType = new NewDawnWorldType();
    this.thaumcraftSupportEnabled = config.getMiscBoolean("Enable internal Thaumcraft support", true);
    if (thaumcraftSupportEnabled) {
      ThaumcraftBiomeProvider.prepareThaumcraftSupport(config);
    }
    config.save();
  }

  @Mod.EventHandler
  public void postInit(FMLPostInitializationEvent event) {
    NewDawnRegistry.registerProvider(new VanillaBiomeProvider());
    if (this.thaumcraftSupportEnabled && Loader.isModLoaded("Thaumcraft")) {
      NewDawnRegistry.registerProvider(new ThaumcraftBiomeProvider());
      log.info("Internal Thaumcraft support enabled.");
    }
  }

  @Mod.EventHandler
  public void serverStart(FMLServerStartingEvent event) {
    final MinecraftServer server = event.getServer();
    final ICommandManager commandManager = server.getCommandManager();
    if (commandManager instanceof ServerCommandManager) {
      final ServerCommandManager serverCommandManager = (ServerCommandManager) commandManager;
      serverCommandManager.registerCommand(new CommandGenerateWorld());
    } else {
      FMLLog.warning("Unable to get server command manager. No slash-commands will be available.");
    }
  }

}
