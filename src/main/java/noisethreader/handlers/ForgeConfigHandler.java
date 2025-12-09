package noisethreader.handlers;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noisethreader.NoiseThreader;

@Config(modid = NoiseThreader.MODID)
public class ForgeConfigHandler {
	
	@Config.Comment("Server-Side Options")
	@Config.Name("Server Options")
	public static final ServerConfig server = new ServerConfig();

	public static class ServerConfig {

		@Config.Comment("Minimum size of the available common thread pool to run multithreading")
		@Config.Name("Thread Pool Minimum Size")
		@Config.RangeInt(min = 1)
		public int threadPoolMinimumSize = 4;
		
		@Config.Comment("If NoiseThreader should multithread BetterCaves noise generation" + "\n" +
				"If false, NoiseThreader will still optimize BetterCaves generation in general")
		@Config.Name("Multithread BetterCaves Noise Generation")
		public boolean multithreadBetterCavesNoise = true;
	}

	@Mod.EventBusSubscriber(modid = NoiseThreader.MODID)
	private static class EventHandler{

		@SubscribeEvent
		public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
			if(event.getModID().equals(NoiseThreader.MODID)) {
				ConfigManager.sync(NoiseThreader.MODID, Config.Type.INSTANCE);
			}
		}
	}
}