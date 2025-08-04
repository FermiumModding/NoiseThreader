package noisethreader.mixin.bettercaves;

import com.yungnickyoung.minecraft.bettercaves.noise.NoiseColumn;
import com.yungnickyoung.minecraft.bettercaves.noise.NoiseGen;
import com.yungnickyoung.minecraft.bettercaves.util.BetterCavesUtils;
import com.yungnickyoung.minecraft.bettercaves.world.carver.CarverSettings;
import com.yungnickyoung.minecraft.bettercaves.world.carver.CarverUtils;
import com.yungnickyoung.minecraft.bettercaves.world.carver.cave.CaveCarver;
import com.yungnickyoung.minecraft.bettercaves.world.carver.cave.CaveCarverBuilder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import noisethreader.util.bettercaves.ICaveCarver;
import noisethreader.util.bettercaves.NoiseColumnNew;
import noisethreader.util.bettercaves.NoiseGenNew;
import noisethreader.util.bettercaves.NoiseTupleNew;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Modified from https://github.com/YUNG-GANG/YUNGs-Better-Caves/blob/1.12.2/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/cave/CaveCarver.java licensed LGPLv3
 * Modified to multithread and improve performance (Fix unneeded boxing/lists/hashing/etc)
 */
@Mixin(CaveCarver.class)
public abstract class CaveCarverMixin implements ICaveCarver {
	
	@Shadow(remap = false) private CarverSettings settings;
	@Shadow(remap = false) private int bottomY;
	@Shadow(remap = false) private int surfaceCutoff;
	@Shadow(remap = false) private boolean enableYAdjust;
	@Shadow(remap = false) private World world;
	@Shadow(remap = false) private float yAdjustF1;
	@Shadow(remap = false) private float yAdjustF2;
	@Shadow(remap = false) private NoiseGen noiseGen;
	
	@Unique
	private NoiseGenNew noisethreader$noiseGenNew;
	
	@Inject(
			method = "<init>",
			at = @At("TAIL"),
			remap = false
	)
	private void noisethreader$betterCavesCaveCarver_init(CaveCarverBuilder builder, CallbackInfo ci) {
		this.noisethreader$noiseGenNew = new NoiseGenNew(
				this.settings.getWorld(),
				this.settings.isFastNoise(),
				this.settings.getNoiseSettings(),
				this.settings.getNumGens(),
				this.settings.getyCompression(),
				this.settings.getXzCompression());
		this.noiseGen = null;
	}
	
	/**
	 * @author fonnymunkey
	 * @reason rewrite for performance
	 */
	@Overwrite(remap = false)
	public void carveColumn(ChunkPrimer primer, BlockPos colPos, int topY, NoiseColumn noises, IBlockState liquidBlock, boolean flooded) {
		//Should not be called
	}
	
	@Unique
	@Override
	public void noisethreader$carveColumnNew(ChunkPrimer primer, BlockPos colPos, int topY, NoiseColumnNew noises, IBlockState liquidBlock, boolean flooded) {
		if(this.bottomY >= 0 && this.bottomY <= 255) {
			if(topY >= 0 && topY <= 255) {
				int localX = BetterCavesUtils.getLocal(colPos.getX());
				int localZ = BetterCavesUtils.getLocal(colPos.getZ());
				if(localX >= 0 && localX <= 15) {
					if(localZ >= 0 && localZ <= 15) {
						int transitionBoundary = topY - this.surfaceCutoff;
						if(transitionBoundary < 1) transitionBoundary = 1;
						
						float[] thresholds = this.noisethreader$generateThresholdsArray(topY, this.bottomY, transitionBoundary);
						if(this.enableYAdjust) {
							this.noisethreader$preprocessCaveNoiseColArray(noises, topY, this.bottomY, thresholds, this.settings.getNumGens());
						}
						
						for(int y = topY; y >= this.bottomY && (y > this.settings.getLiquidAltitude() || liquidBlock != null); y--) {
							boolean digBlock = true;
							for(double noise : noises.get(y).getNoiseValuesArray()) {
								if(noise < (double)thresholds[y]) {
									digBlock = false;
									break;
								}
							}
							if(this.settings.isEnableDebugVisualizer()) {
								BlockPos blockPos = new BlockPos(localX, y, localZ);
								CarverUtils.debugDigBlock(primer, blockPos, this.settings.getDebugBlock(), digBlock);
							}
							else if(digBlock) {
								IBlockState airBlockState = flooded && y < this.world.getSeaLevel() ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState();
								BlockPos blockPos = new BlockPos(localX, y, localZ);
								CarverUtils.digBlock(this.settings.getWorld(), primer, blockPos, airBlockState, liquidBlock, this.settings.getLiquidAltitude(), this.settings.isReplaceFloatingGravel());
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * @author fonnymunkey
	 * @reason rewrite for performance
	 */
	@Overwrite(remap = false)
	private void preprocessCaveNoiseCol(NoiseColumn noises, int topY, int bottomY, Map<Integer, Float> thresholds, int numGens) {
		//Should not be called
	}
	
	@Unique
	private void noisethreader$preprocessCaveNoiseColArray(NoiseColumnNew noises, int topY, int bottomY, float[] thresholds, int numGens) {
		for(int realY = topY; realY >= bottomY; realY--) {
			NoiseTupleNew noiseBlock = noises.get(realY);
			boolean valid = true;
			for(double noise : noiseBlock.getNoiseValuesArray()) {
				if(noise < (double)thresholds[realY]) {
					valid = false;
					break;
				}
			}
			
			if(valid) {
				float f1 = this.yAdjustF1;
				float f2 = this.yAdjustF2;
				if(realY < topY) {
					NoiseTupleNew tupleAbove = noises.get(realY + 1);
					for(int i = 0; i < numGens; ++i) {
						tupleAbove.set(i, (double)(1.0F - f1) * tupleAbove.get(i) + (double)f1 * noiseBlock.get(i));
					}
				}
				if(realY < topY - 1) {
					NoiseTupleNew tupleTwoAbove = noises.get(realY + 2);
					for(int i = 0; i < numGens; ++i) {
						tupleTwoAbove.set(i, (double)(1.0F - f2) * tupleTwoAbove.get(i) + (double)f2 * noiseBlock.get(i));
					}
				}
			}
		}
	}
	
	/**
	 * @author fonnymunkey
	 * @reason rewrite for performance
	 */
	@Overwrite(remap = false)
	private Map<Integer, Float> generateThresholds(int topY, int bottomY, int transitionBoundary) {
		//Should not be called
		return null;
	}
	
	@Unique
	private float[] noisethreader$generateThresholdsArray(int topY, int bottomY, int transitionBoundary) {
		float[] thresholds = new float[topY + 1];
		for(int realY = bottomY; realY <= topY; realY++) {
			float noiseThreshold = this.settings.getNoiseThreshold();
			if(realY >= transitionBoundary) {
				noiseThreshold *= 1.0F + 0.3F * ((float)(realY - transitionBoundary) / (float)(topY - transitionBoundary));
			}
			thresholds[realY] = noiseThreshold;
		}
		return thresholds;
	}
	
	/**
	 * @author fonnymunkey
	 * @reason rewrite for performance
	 */
	@Overwrite(remap = false)
	public NoiseGen getNoiseGen() {
		//Should not be called
		return null;
	}
	
	@Unique
	@Override
	public NoiseGenNew noisethreader$getNoiseGenNew() {
		return this.noisethreader$noiseGenNew;
	}
}