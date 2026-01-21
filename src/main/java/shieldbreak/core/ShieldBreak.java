package shieldbreak.core;

import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.*;
import shieldbreak.handlers.TooltipHandler;

@Mod(modid = ShieldBreak.MODID, version = ShieldBreak.VERSION, name = ShieldBreak.NAME, acceptableRemoteVersions = "*", dependencies = "required:fermiumbooter")
public class ShieldBreak {
	
    public static final String MODID = "shieldbreak";
    public static final String VERSION = "1.2.2";
    public static final String NAME = "ShieldBreak";
	
	@Instance(MODID)
	public static ShieldBreak instance;
	
	public static Logger logger;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    	logger = event.getModLog();
    }
    
    @EventHandler
    public void onInit(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(new TooltipHandler());
    }
}