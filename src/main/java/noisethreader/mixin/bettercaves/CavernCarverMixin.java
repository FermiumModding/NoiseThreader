package noisethreader.mixin.bettercaves;

import com.yungnickyoung.minecraft.bettercaves.enums.CavernType;
import com.yungnickyoung.minecraft.bettercaves.noise.NoiseColumn;
import com.yungnickyoung.minecraft.bettercaves.noise.NoiseGen;
import com.yungnickyoung.minecraft.bettercaves.util.BetterCavesUtils;
import com.yungnickyoung.minecraft.bettercaves.world.carver.CarverSettings;
import com.yungnickyoung.minecraft.bettercaves.world.carver.CarverUtils;
import com.yungnickyoung.minecraft.bettercaves.world.carver.cavern.CavernCarver;
import com.yungnickyoung.minecraft.bettercaves.world.carver.cavern.CavernCarverBuilder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import noisethreader.util.bettercaves.ICavernCarver;
import noisethreader.util.bettercaves.NoiseColumnNew;
import noisethreader.util.bettercaves.NoiseGenNew;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Modified from https://github.com/YUNG-GANG/YUNGs-Better-Caves/blob/1.12.2/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/carver/cave/CaveCarver.java licensed LGPLv3
 * Modified to improve performance (Fix unneeded boxing/lists/hashing/etc)
 */
//TODO Also multithread this?
@Mixin(CavernCarver.class)
public abstract class CavernCarverMixin implements ICavernCarver {
	
	@Shadow(remap = false) private CarverSettings settings;
	@Shadow(remap = false) private NoiseGen noiseGen;
	@Shadow(remap = false) private int bottomY;
	@Shadow(remap = false) private CavernType cavernType;
	@Shadow(remap = false) private World world;
	
	@Unique
	private NoiseGenNew noisethreader$noiseGenNew;
	
	@Inject(
			method = "<init>",
			at = @At("TAIL"),
			remap = false
	)
	private void noisethreader$betterCavesCavernCarver_init(CavernCarverBuilder builder, CallbackInfo ci) {
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
	public void carveColumn(ChunkPrimer primer, BlockPos colPos, int topY, float smoothAmp, NoiseColumn noises, IBlockState liquidBlock, boolean flooded) {
		//Should not be called
	}
	
	@Unique
	@Override
	public void noisethreader$carveColumnNew(ChunkPrimer primer, BlockPos colPos, int topY, float smoothAmp, NoiseColumnNew noises, IBlockState liquidBlock, boolean flooded) {
		if(this.bottomY >= 0 && this.bottomY <= 255) {
			int localX = BetterCavesUtils.getLocal(colPos.getX());
			int localZ = BetterCavesUtils.getLocal(colPos.getZ());
			if(localX >= 0 && localX <= 15) {
				if(localZ >= 0 && localZ <= 15) {
					if(topY <= 255) {
						topY -= 2;
						int topTransitionBoundary = Math.max(topY - 6, 1);
						int bottomTransitionBoundary = this.bottomY + 3;
						if(this.cavernType == CavernType.FLOORED) {
							bottomTransitionBoundary = this.bottomY < this.settings.getLiquidAltitude() ? this.settings.getLiquidAltitude() + 8 : this.bottomY + 7;
						}
						bottomTransitionBoundary = Math.min(bottomTransitionBoundary, 255);
						for(int y = topY; y >= this.bottomY && (y > this.settings.getLiquidAltitude() || liquidBlock != null); y--) {
							boolean digBlock = false;
							
							float noise = 1.0F;
							for(double n : noises.get(y).getNoiseValuesArray()) {
								noise *= (float)n;
							}
							
							float noiseThreshold = this.settings.getNoiseThreshold();
							if(y >= topTransitionBoundary) {
								noiseThreshold *= (float)(y - topY) / (float)(topTransitionBoundary - topY);
							}
							
							if(y < bottomTransitionBoundary) {
								noiseThreshold *= (float)(y - this.bottomY) / (float)(bottomTransitionBoundary - this.bottomY);
							}
							
							if(smoothAmp < 1.0F) {
								noiseThreshold *= smoothAmp;
							}
							
							if(noise < noiseThreshold) {
								digBlock = true;
							}
							
							if(this.settings.isEnableDebugVisualizer()) {
								BlockPos blockPos = new BlockPos(localX, y, localZ);
								CarverUtils.debugDigBlock(primer, blockPos, this.settings.getDebugBlock(), digBlock);
							}
							else if(digBlock) {
								BlockPos blockPos = new BlockPos(localX, y, localZ);
								IBlockState airBlockState = flooded && y < this.world.getSeaLevel() ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState();
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