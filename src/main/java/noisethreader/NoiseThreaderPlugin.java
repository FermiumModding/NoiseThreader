package noisethreader;

import java.util.Map;

import fermiumbooter.FermiumRegistryAPI;
import net.minecraftforge.fml.common.Loader;
import org.spongepowered.asm.launch.MixinBootstrap;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class NoiseThreaderPlugin implements IFMLLoadingPlugin {

	public NoiseThreaderPlugin() {
		MixinBootstrap.init();
		
		FermiumRegistryAPI.enqueueMixin(false, "mixins.noisethreader.vanilla.json");
		FermiumRegistryAPI.enqueueMixin(true, "mixins.noisethreader.otg.json", () -> Loader.isModLoaded("openterraingenerator"));
		FermiumRegistryAPI.enqueueMixin(true, "mixins.noisethreader.bettercaves.json", () -> Loader.isModLoaded("bettercaves"));
	}

	@Override
	public String[] getASMTransformerClass()
	{
		return new String[0];
	}
	
	@Override
	public String getModContainerClass()
	{
		return null;
	}
	
	@Override
	public String getSetupClass()
	{
		return null;
	}
	
	@Override
	public void injectData(Map<String, Object> data) { }
	
	@Override
	public String getAccessTransformerClass()
	{
		return null;
	}
}