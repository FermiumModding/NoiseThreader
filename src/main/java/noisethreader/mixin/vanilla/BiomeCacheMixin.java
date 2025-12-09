package noisethreader.mixin.vanilla;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.biome.BiomeCache;
import noisethreader.handlers.ForgeConfigHandler;
import org.spongepowered.asm.mixin.*;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(BiomeCache.class)
public abstract class BiomeCacheMixin {
	
	//Quick and dirty thread safety, slight overhead but much less cost than the rest of the savings
	@Unique
	private final ReentrantLock noisethreader$cacheLock = new ReentrantLock();
	
	@WrapMethod(
			method = "getEntry"
	)
	private BiomeCache.Block noisethreader_vanillaBiomeCache_getEntry(int x, int z, Operation<BiomeCache.Block> original) {
		if(!ForgeConfigHandler.server.multithreadBetterCavesNoise) {
			return original.call(x, z);
		}
		
		this.noisethreader$cacheLock.lock();
		try {
			return original.call(x, z);
		}
		finally {
			this.noisethreader$cacheLock.unlock();
		}
	}
	
	@WrapMethod(
			method = "cleanupCache"
	)
	private void noisethreader_vanillaBiomeCache_cleanupCache(Operation<Void> original) {
		if(!ForgeConfigHandler.server.multithreadBetterCavesNoise) {
			original.call();
			return;
		}
		
		this.noisethreader$cacheLock.lock();
		try {
			original.call();
		}
		finally {
			this.noisethreader$cacheLock.unlock();
		}
	}
}