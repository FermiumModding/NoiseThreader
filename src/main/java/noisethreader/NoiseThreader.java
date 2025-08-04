package noisethreader;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = NoiseThreader.MODID, version = NoiseThreader.VERSION, name = NoiseThreader.NAME, dependencies = "required-after:fermiumbooter; after:openterraingenerator", acceptableRemoteVersions = "*")
public class NoiseThreader {
    public static final String MODID = "noisethreader";
    public static final String VERSION = "1.1.1";
    public static final String NAME = "NoiseThreader";
    public static final Logger LOGGER = LogManager.getLogger();
	
	@Instance(MODID)
	public static NoiseThreader instance;
}