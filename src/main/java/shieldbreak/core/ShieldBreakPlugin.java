package shieldbreak.core;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import fermiumbooter.FermiumRegistryAPI;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;

import java.util.Map;

@IFMLLoadingPlugin.Name("ShieldBreak")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class ShieldBreakPlugin implements IFMLLoadingPlugin {

	public ShieldBreakPlugin() {
		MixinBootstrap.init();
		MixinExtrasBootstrap.init();
		
		FermiumRegistryAPI.enqueueMixin(false, "mixins.shieldbreak.early.vanilla.json");
		FermiumRegistryAPI.enqueueMixin(true, "mixins.shieldbreak.late.ddd.json", () -> FermiumRegistryAPI.isModPresent("distinctdamagedescriptions"));
	}

	@Override
	public String[] getASMTransformerClass() {
		return new String[0];
	}
	
	@Override
	public String getModContainerClass() {
		return null;
	}
	
	@Override
	public String getSetupClass() {
		return null;
	}
	
	@Override
	public void injectData(Map<String, Object> data) { }
	
	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}