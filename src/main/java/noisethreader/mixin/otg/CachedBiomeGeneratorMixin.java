package noisethreader.mixin.otg;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.pg85.otg.util.ChunkCoordinate;
import noisethreader.handlers.ForgeConfigHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(targets = "com.pg85.otg.generator.biome.CachedBiomeGenerator")
public abstract class CachedBiomeGeneratorMixin {
	
	@Unique
	private final ReentrantLock noisethreader$cacheLock = new ReentrantLock();
	
	@Coerce//Me when everything private
	@WrapMethod(
			method = "getBiomeCacheChunk",
			remap = false
	)
	private Object noisethreader_otgCachedBiomeGenerator_getBiomeClassChunk(ChunkCoordinate chunkCoord, Operation<Object> original) {
		if(!ForgeConfigHandler.server.multithreadBetterCavesNoise) {
			return original.call(chunkCoord);
		}
		
		this.noisethreader$cacheLock.lock();
		try {
			return original.call(chunkCoord);
		}
		finally {
			this.noisethreader$cacheLock.unlock();
		}
	}
}