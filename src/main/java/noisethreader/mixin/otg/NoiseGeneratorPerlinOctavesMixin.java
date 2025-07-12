package noisethreader.mixin.otg;

import com.pg85.otg.generator.noise.NoiseGeneratorPerlin;
import com.pg85.otg.generator.noise.NoiseGeneratorPerlinOctaves;
import net.minecraft.util.math.MathHelper;
import noisethreader.handlers.ForgeConfigHandler;
import noisethreader.handlers.OTGNoiseHandler;
import noisethreader.handlers.ThreadHandler;
import noisethreader.util.INoiseGeneratorPerlinOctaves;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.Random;

@Mixin(NoiseGeneratorPerlinOctaves.class)
public abstract class NoiseGeneratorPerlinOctavesMixin implements INoiseGeneratorPerlinOctaves {
	
	@Shadow(remap = false) private int numOctaves;
	
	@Shadow(remap = false) private NoiseGeneratorPerlin[] noiseArray;
	
	@Unique
	private final OTGNoiseHandler noisethreader$otgNoiseHandler = new OTGNoiseHandler();
	
	@Unique
	private int noisethreader$octaveSplitAmount = 0;
	
	@Inject(
			method = "<init>",
			at = @At("RETURN"),
			remap = false
	)
	private void noisethreader_otgNoiseGeneratorPerlinOctaves_init(Random seed, int octavesIn, CallbackInfo ci) {
		if(ThreadHandler.getPoolSize() >= ForgeConfigHandler.server.threadPoolMinimumSize) {
			this.noisethreader$octaveSplitAmount = (int)((double)(this.numOctaves + 1) / 2.0D);
		}
	}
	
	/**
	 * @author fonnymunkey
	 * @reason Handle multithreaded noise generation for better performance
	 */
	@Overwrite(remap = false)
	public double[] Noise3D(double[] doubleArray, int xOffset, int yOffset, int zOffset, int xSize, int ySize, int zSize, double xScale, double yScale, double zScale) {
		if(doubleArray == null) {
			doubleArray = new double[xSize * ySize * zSize];
		}
		else {
			//TODO: Maybe just better to always create a new empty array regardless?
			Arrays.fill(doubleArray, 0.0D);
		}
		
		//Values below these generally do not benefit from multithreading and can give worse performance
		if(this.noisethreader$octaveSplitAmount <= 0 || (this.numOctaves * xSize * ySize * zSize) / this.noisethreader$octaveSplitAmount < 550) {
			return this.noisethreader$generateNoiseOctavesOTG(doubleArray, xOffset, yOffset, zOffset, xSize, ySize, zSize, xScale, yScale, zScale);
		}
		else return this.noisethreader$otgNoiseHandler.generateOTGNoiseOctaves((NoiseGeneratorPerlinOctaves)(Object)this, doubleArray, xOffset, yOffset, zOffset, xSize, ySize, zSize, xScale, yScale, zScale, this.numOctaves, 2, this.noisethreader$octaveSplitAmount);
	}
	
	/**
	 * Default method of generating noise octaves when multithreading won't improve performance
	 */
	@Unique
	@Override
	public double[] noisethreader$generateNoiseOctavesOTG(double[] doubleArray, int xOffset, int yOffset, int zOffset, int xSize, int ySize, int zSize, double xScale, double yScale, double zScale) {
		double d3 = 1.0D;
		
		for(int j = 0; j < this.numOctaves; ++j) {
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
			((INoiseGeneratorPerlinMixin)this.noiseArray[j]).invokePopulateNoiseArray3D(doubleArray, d0, d1, d2, xSize, ySize, zSize, xScale * d3, yScale * d3, zScale * d3, d3);
			d3 /= 2.0D;
		}
		
		return doubleArray;
	}
	
	/**
	 * Multithreaded handling of generating noise by octave sections
	 */
	@Unique
	@Override
	public double[] noisethreader$generateNoiseOctavesThreaded(double[] doubleArray, int xOffset, int yOffset, int zOffset, int xSize, int ySize, int zSize, double xScale, double yScale, double zScale, int octaveStart, int octaveEnd) {
		if(doubleArray == null) {
			doubleArray = new double[xSize * ySize * zSize];
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
			((INoiseGeneratorPerlinMixin)this.noiseArray[j]).invokePopulateNoiseArray3D(doubleArray, d0, d1, d2, xSize, ySize, zSize, xScale * d3, yScale * d3, zScale * d3, d3);
			d3 /= 2.0D;
		}
		
		return doubleArray;
	}
}