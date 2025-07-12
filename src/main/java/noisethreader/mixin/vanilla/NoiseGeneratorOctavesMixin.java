package noisethreader.mixin.vanilla;

import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.NoiseGeneratorImproved;
import net.minecraft.world.gen.NoiseGeneratorOctaves;
import noisethreader.handlers.ForgeConfigHandler;
import noisethreader.handlers.ThreadHandler;
import noisethreader.handlers.VanillaNoiseHandler;
import noisethreader.util.INoiseGeneratorOctaves;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.Random;

@Mixin(value = NoiseGeneratorOctaves.class)
public abstract class NoiseGeneratorOctavesMixin implements INoiseGeneratorOctaves {
	
	@Shadow @Final private int octaves;
	@Shadow @Final private NoiseGeneratorImproved[] generatorCollection;
	
	@Unique
	private final VanillaNoiseHandler noisethreader$vanillaNoiseHandler = new VanillaNoiseHandler();
	
	@Unique
	private int noisethreader$octaveSplitAmount = 0;
	
	@Inject(
			method = "<init>",
			at = @At("RETURN")
	)
	private void noisethreader_vanillaNoiseGeneratorOctaves_init(Random seed, int octavesIn, CallbackInfo ci) {
		if(ThreadHandler.getPoolSize() >= ForgeConfigHandler.server.threadPoolMinimumSize) {
			this.noisethreader$octaveSplitAmount = (int)((double)(this.octaves + 1) / 2.0D);
		}
	}
	
	/**
	 * @author fonnymunkey
	 * @reason Handle multithreaded noise generation for better performance
	 */
	@Overwrite
	public double[] generateNoiseOctaves(double[] noiseArray, int xOffset, int yOffset, int zOffset, int xSize, int ySize, int zSize, double xScale, double yScale, double zScale) {
		if(noiseArray == null) {
			noiseArray = new double[xSize * ySize * zSize];
		}
		else {
			//TODO: Maybe just better to always create a new empty array regardless?
			Arrays.fill(noiseArray, 0.0D);
		}
		
		//Values below these generally do not benefit from multithreading and can give worse performance
		if(this.noisethreader$octaveSplitAmount <= 0 || (this.octaves * xSize * ySize * zSize) / this.noisethreader$octaveSplitAmount < 550) {
			return this.noisethreader$generateNoiseOctavesVanilla(noiseArray, xOffset, yOffset, zOffset, xSize, ySize, zSize, xScale, yScale, zScale);
		}
		else return this.noisethreader$vanillaNoiseHandler.generateVanillaNoiseOctaves((NoiseGeneratorOctaves)(Object)this, noiseArray, xOffset, yOffset, zOffset, xSize, ySize, zSize, xScale, yScale, zScale, this.octaves, 2, this.noisethreader$octaveSplitAmount);
	}
	
	/**
	 * Default method of generating noise octaves when multithreading won't improve performance
	 */
	@Unique
	@Override
	public double[] noisethreader$generateNoiseOctavesVanilla(double[] noiseArray, int xOffset, int yOffset, int zOffset, int xSize, int ySize, int zSize, double xScale, double yScale, double zScale) {
		double d3 = 1.0D;
		
		for(int j = 0; j < this.octaves; ++j) {
			double d0 = (double)xOffset * d3 * xScale;
			double d1 = (double)yOffset * d3 * yScale;
			double d2 = (double)zOffset * d3 * zScale;
			long k = MathHelper.lfloor(d0);
			long l = MathHelper.lfloor(d2);
			d0 = d0 - (double)k;
			d2 = d2 - (double)l;
			k = k % 16777216L;
			l = l % 16777216L;
			d0 = d0 + (double)k;
			d2 = d2 + (double)l;
			this.generatorCollection[j].populateNoiseArray(noiseArray, d0, d1, d2, xSize, ySize, zSize, xScale * d3, yScale * d3, zScale * d3, d3);
			d3 /= 2.0D;
		}
		
		return noiseArray;
	}
	
	/**
	 * Multithreaded handling of generating noise by octave sections
	 */
	@Unique
	@Override
	public double[] noisethreader$generateNoiseOctavesThreaded(double[] noiseArray, int xOffset, int yOffset, int zOffset, int xSize, int ySize, int zSize, double xScale, double yScale, double zScale, int octaveStart, int octaveEnd) {
		if(noiseArray == null) {
			noiseArray = new double[xSize * ySize * zSize];
		}
		
		double d3 = 1.0D;
		for(int i = 0; i < octaveStart; i++) {
			d3 /= 2.0D;
		}
		
		for(int j = octaveStart; j < octaveEnd; ++j) {
			double d0 = (double)xOffset * d3 * xScale;
			double d1 = (double)yOffset * d3 * yScale;
			double d2 = (double)zOffset * d3 * zScale;
			long k = MathHelper.lfloor(d0);
			long l = MathHelper.lfloor(d2);
			d0 = d0 - (double)k;
			d2 = d2 - (double)l;
			k = k % 16777216L;
			l = l % 16777216L;
			d0 = d0 + (double)k;
			d2 = d2 + (double)l;
			this.generatorCollection[j].populateNoiseArray(noiseArray, d0, d1, d2, xSize, ySize, zSize, xScale * d3, yScale * d3, zScale * d3, d3);
			d3 /= 2.0D;
		}
		
		return noiseArray;
	}
}