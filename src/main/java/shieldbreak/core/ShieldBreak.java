package shieldbreak.core;

import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import shieldbreak.core.proxies.CommonProxy;

@Mod(modid = ShieldBreak.MODID, version = ShieldBreak.VERSION, name = ShieldBreak.NAME, acceptableRemoteVersions = "*")
public class ShieldBreak
{
    public static final String MODID = "shieldbreak";
    public static final String VERSION = "1.1.3";
    public static final String NAME = "ShieldBreak";
    public static final String PROXY = "shieldbreak.core.proxies";
    public static final String CHANNEL = "SHIELDBREAK";
	
	@Instance(MODID)
	public static ShieldBreak instance;
	
	@SidedProxy(clientSide = PROXY + ".ClientProxy", serverSide = PROXY + ".CommonProxy")
	public static CommonProxy proxy;
	public static Logger logger;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
    	logger = event.getModLog();
    }
    
    @EventHandler
    public void onInit(FMLInitializationEvent event)
    {
    	proxy.registerHandlers(); 
    }
}
