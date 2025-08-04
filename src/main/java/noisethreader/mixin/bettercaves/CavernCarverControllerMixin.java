package noisethreader.mixin.bettercaves;

import com.yungnickyoung.minecraft.bettercaves.noise.FastNoise;
import com.yungnickyoung.minecraft.bettercaves.util.BetterCavesUtils;
import com.yungnickyoung.minecraft.bettercaves.world.CavernCarverController;
import com.yungnickyoung.minecraft.bettercaves.world.carver.CarverNoiseRange;
import com.yungnickyoung.minecraft.bettercaves.world.carver.cavern.CavernCarver;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraftforge.common.BiomeDictionary;
import noisethreader.util.bettercaves.ICavernCarver;
import noisethreader.util.bettercaves.NoiseColumnNew;
import noisethreader.util.bettercaves.NoiseCubeNew;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.function.Predicate;

/**
 * Modified from https://github.com/YUNG-GANG/YUNGs-Better-Caves/blob/1.12.2/src/main/java/com/yungnickyoung/minecraft/bettercaves/world/CavernCarverController.java licensed LGPLv3
 * Modified to improve performance (Fix unneeded boxing/lists/hashing/etc)
 */
@Mixin(CavernCarverController.class)
public abstract class CavernCarverControllerMixin {
	
	@Shadow(remap = false) private List<CarverNoiseRange> noiseRanges;
	@Shadow(remap = false) private boolean isOverrideSurfaceDetectionEnabled;
	@Shadow(remap = false) private boolean isFloodedUndergroundEnabled;
	@Shadow(remap = false) private boolean isDebugViewEnabled;
	@Shadow(remap = false) private World world;
	@Shadow(remap = false) private static Predicate<Biome> isNotOcean;
	@Shadow(remap = false) private static Predicate<Biome> isOcean;
	@Shadow(remap = false) private FastNoise cavernRegionController;
	
	/**
	 * @author fonnymunkey
	 * @reason performance
	 */
	@Overwrite(remap = false)
	public void carveChunk(ChunkPrimer primer, int chunkX, int chunkZ, int[][] surfaceAltitudes, IBlockState[][] liquidBlocks) {
		if(this.noiseRanges.isEmpty()) return;
		for(int subX = 0; subX < 4; subX++) {
			for(int subZ = 0; subZ < 4; subZ++) {
				int startX = subX * 4;
				int startZ = subZ * 4;
				int endX = startX + 4 - 1;
				int endZ = startZ + 4 - 1;
				int startPosX = chunkX * 16 + startX;
				int startPosZ = chunkZ * 16 + startZ;
				int endPosX = chunkX * 16 + endX;
				int endPosZ = chunkZ * 16 + endZ;
				int maxHeight = 0;
				if(!this.isOverrideSurfaceDetectionEnabled) {
					for(int x = startX; x < endX; x++) {
						for(int z = startZ; z < endZ; z++) {
							maxHeight = Math.max(maxHeight, surfaceAltitudes[x][z]);
						}
					}
					for(CarverNoiseRange range : this.noiseRanges) {
						maxHeight = Math.max(maxHeight, range.getCarver().getTopY());
					}
				}
				//NoiseCube isn't actually used outside of this section of iteration, so does not need to be stored in noiseRanges
				NoiseCubeNew[] noiseCubes = new NoiseCubeNew[this.noiseRanges.size()];
				for(int offsetX = 0; offsetX < 4; offsetX++) {
					for(int offsetZ = 0; offsetZ < 4; offsetZ++) {
						int localX = startX + offsetX;
						int localZ = startZ + offsetZ;
						BlockPos colPos = new BlockPos(chunkX * 16 + localX, 1, chunkZ * 16 + localZ);
						boolean flooded = false;
						float smoothAmpFactor = 1;
						if(this.isFloodedUndergroundEnabled && !this.isDebugViewEnabled) {
							flooded = BiomeDictionary.hasType(this.world.getBiome(colPos), BiomeDictionary.Type.OCEAN);
							smoothAmpFactor = BetterCavesUtils.biomeDistanceFactor(this.world, colPos, 2, flooded ? isNotOcean : isOcean);
							if(smoothAmpFactor <= 0) continue;
						}
						int surfaceAltitude = surfaceAltitudes[localX][localZ];
						IBlockState liquidBlock = liquidBlocks[localX][localZ];
						float cavernRegionNoise = this.cavernRegionController.GetNoise(colPos.getX(), colPos.getZ());
						for(int rangeIndex = 0; rangeIndex < this.noiseRanges.size(); rangeIndex++) {
							CarverNoiseRange range = this.noiseRanges.get(rangeIndex);
							if(range.contains(cavernRegionNoise)) {
								CavernCarver carver = (CavernCarver)range.getCarver();
								int bottomY = carver.getBottomY();
								int topY = this.isDebugViewEnabled ? carver.getTopY() : Math.min(surfaceAltitude, carver.getTopY());
								if(this.isOverrideSurfaceDetectionEnabled) {
									topY = carver.getTopY();
									maxHeight = carver.getTopY();
								}
								float smoothAmp = range.getSmoothAmp(cavernRegionNoise) * smoothAmpFactor;
								if(noiseCubes[rangeIndex] == null) {
									noiseCubes[rangeIndex] = ((ICavernCarver)carver).noisethreader$getNoiseGenNew().interpolateNoiseCube(startPosX, startPosZ, endPosX, endPosZ, bottomY, maxHeight);
								}
								NoiseColumnNew noiseColumn = noiseCubes[rangeIndex].getArray(offsetX)[offsetZ];
								((ICavernCarver)carver).noisethreader$carveColumnNew(primer, colPos, topY, smoothAmp, noiseColumn, liquidBlock, flooded);
								break;
							}
						}
					}
				}
			}
		}
	}
}