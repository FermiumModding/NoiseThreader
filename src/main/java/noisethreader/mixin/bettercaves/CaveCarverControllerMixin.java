package noisethreader.mixin.bettercaves;

import com.yungnickyoung.minecraft.bettercaves.noise.FastNoise;
import com.yungnickyoung.minecraft.bettercaves.noise.NoiseColumn;
import com.yungnickyoung.minecraft.bettercaves.noise.NoiseCube;
import com.yungnickyoung.minecraft.bettercaves.world.CaveCarverController;
import com.yungnickyoung.minecraft.bettercaves.world.carver.CarverNoiseRange;
import com.yungnickyoung.minecraft.bettercaves.world.carver.cave.CaveCarver;
import com.yungnickyoung.minecraft.bettercaves.world.carver.vanilla.VanillaCaveCarver;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraftforge.common.BiomeDictionary;
import noisethreader.NoiseThreader;
import noisethreader.util.ColumnCarverHolder;
import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.stream.IntStream;

@Mixin(CaveCarverController.class)
public abstract class CaveCarverControllerMixin {
	
	@Shadow(remap = false) private List<CarverNoiseRange> noiseRanges;
	@Shadow(remap = false) private boolean isSurfaceCavesEnabled;
	@Shadow(remap = false) private boolean isOverrideSurfaceDetectionEnabled;
	@Shadow(remap = false) private boolean isFloodedUndergroundEnabled;
	@Shadow(remap = false) private boolean isDebugViewEnabled;
	@Shadow(remap = false) private FastNoise caveRegionController;
	@Shadow(remap = false) private VanillaCaveCarver surfaceCaveCarver;
	@Shadow(remap = false) private World world;

	@Unique
	private boolean noisethreader$shouldCarveVanillaCaves = false;
	
	@Unique
	private boolean[][] noisethreader$vanillaCarvingMask = null;
	
	@Unique
	private ColumnCarverHolder[][][][] noisethreader$columnCarverHolders = null;

	//This can probably be optimized further but as-is already 60% reduction
	/**
	 * @author fonnymunkey
	 * @reason multithreading
	 */
	@Overwrite(remap = false)
	public void carveChunk(ChunkPrimer primer, int chunkX, int chunkZ, int[][] surfaceAltitudes, IBlockState[][] liquidBlocks) {
		if(noiseRanges.isEmpty() && !isSurfaceCavesEnabled) return;
		
		this.noisethreader$shouldCarveVanillaCaves = false;
		this.noisethreader$vanillaCarvingMask = new boolean[16][16];
		this.noisethreader$columnCarverHolders = new ColumnCarverHolder[4][4][4][4];
		
		try {
			//Last 16 pos iterations rely on the same noisecube based on order, so group them to be handled in the same thread each
			IntStream.range(0, 16).parallel().forEach(subIndex -> this.noisethreader$genChunkCarverSection(chunkX, chunkZ, surfaceAltitudes, liquidBlocks, subIndex/4, subIndex%4));
		}
		catch(Exception ex) {
			NoiseThreader.LOGGER.log(Level.ERROR, "NoiseThreader BetterCaves Multithreaded Noise encountered an error: " + ex.getMessage(), ex);
			//Just run the original carving instead, since nothing would have actually been carved yet here
			this.noisethreader$carveChunkOriginal(primer, chunkX, chunkZ, surfaceAltitudes, liquidBlocks);
			this.noisethreader$vanillaCarvingMask = null;
			this.noisethreader$columnCarverHolders = null;
			return;
		}
		
		//Gross and can probably be better but im tired
		for(int subX = 0; subX < 4; subX++) {
			for(int subZ = 0; subZ < 4; subZ++) {
				for(int offsetX = 0; offsetX < 4; offsetX++) {
					for(int offsetZ = 0; offsetZ < 4; offsetZ++) {
						//Don't need to iterate noiseRanges as only one is ever selected due to break
						ColumnCarverHolder columnCarverHolder = this.noisethreader$columnCarverHolders[subX][subZ][offsetX][offsetZ];
						if(columnCarverHolder == null) continue;
						//Reconvene and carve in correct order just incase
						((CaveCarver)this.noiseRanges.get(columnCarverHolder.carverIndex).getCarver())
								.carveColumn(primer,
											 columnCarverHolder.colPos,
											 columnCarverHolder.topY,
											 columnCarverHolder.noiseColumn,
											 columnCarverHolder.liquidBlock,
											 columnCarverHolder.flooded);
					}
				}
			}
		}
		
		//Vanilla carving should be effectively the same
		if(this.noisethreader$shouldCarveVanillaCaves) {
			VanillaCaveCarver carver = null;
			for(CarverNoiseRange range : this.noiseRanges) {
				if(range.getCarver() instanceof VanillaCaveCarver) {
					carver = (VanillaCaveCarver)range.getCarver();
					break;
				}
			}
			if(carver != null) {
				carver.generate(this.world, chunkX, chunkZ, primer, true, liquidBlocks, this.noisethreader$vanillaCarvingMask);
			}
		}
		if(this.isSurfaceCavesEnabled) {
			this.surfaceCaveCarver.generate(this.world, chunkX, chunkZ, primer, false, liquidBlocks);
		}
		
		this.noisethreader$vanillaCarvingMask = null;
		this.noisethreader$columnCarverHolders = null;
	}
	
	@Unique
	private void noisethreader$genChunkCarverSection(int chunkX, int chunkZ, int[][] surfaceAltitudes, IBlockState[][] liquidBlocks, int subX, int subZ) {
		int startX = subX * 4;
		int startZ = subZ * 4;
		int endX = startX + 4 - 1;
		int endZ = startZ + 4 - 1;
		BlockPos startPos = new BlockPos(chunkX * 16 + startX, 1, chunkZ * 16 + startZ);
		BlockPos endPos = new BlockPos(chunkX * 16 + endX, 1, chunkZ * 16 + endZ);
		
		int maxHeight = 0;
		if(!isOverrideSurfaceDetectionEnabled) {
			for(int x = startX; x < endX; x++) {
				for(int z = startZ; z < endZ; z++) {
					maxHeight = Math.max(maxHeight, surfaceAltitudes[x][z]);
				}
			}
			for(CarverNoiseRange range : noiseRanges) {
				maxHeight = Math.max(maxHeight, range.getCarver().getTopY());
			}
		}
		//NoiseCube isn't actually used outside of this section of iteration, so does not need to be stored in noiseRanges
		NoiseCube[] noiseCubes = new NoiseCube[this.noiseRanges.size()];
		for(int offsetX = 0; offsetX < 4; offsetX++) {
			for(int offsetZ = 0; offsetZ < 4; offsetZ++) {
				int localX = startX + offsetX;
				int localZ = startZ + offsetZ;
				BlockPos colPos = new BlockPos(chunkX * 16 + localX, 1, chunkZ * 16 + localZ);
				boolean flooded = isFloodedUndergroundEnabled && !isDebugViewEnabled && BiomeDictionary.hasType(world.getBiome(colPos), BiomeDictionary.Type.OCEAN);
				if(flooded) {
					if(!BiomeDictionary.hasType(world.getBiome(colPos.east()), BiomeDictionary.Type.OCEAN) ||
							!BiomeDictionary.hasType(world.getBiome(colPos.north()), BiomeDictionary.Type.OCEAN) ||
							!BiomeDictionary.hasType(world.getBiome(colPos.west()), BiomeDictionary.Type.OCEAN) ||
							!BiomeDictionary.hasType(world.getBiome(colPos.south()), BiomeDictionary.Type.OCEAN)
					) continue;
				}
				
				int surfaceAltitude = surfaceAltitudes[localX][localZ];
				IBlockState liquidBlock = liquidBlocks[localX][localZ];
				
				float caveRegionNoise = caveRegionController.GetNoise(colPos.getX(), colPos.getZ());
				for(int rangeIndex = 0; rangeIndex < this.noiseRanges.size(); rangeIndex++) {
					CarverNoiseRange range = this.noiseRanges.get(rangeIndex);
					if(!range.contains(caveRegionNoise)) continue;
					if(range.getCarver() instanceof CaveCarver) {
						CaveCarver carver = (CaveCarver)range.getCarver();
						int bottomY = carver.getBottomY();
						int topY = Math.min(surfaceAltitude, carver.getTopY());
						if(this.isOverrideSurfaceDetectionEnabled) {
							topY = carver.getTopY();
							maxHeight = carver.getTopY();
						}
						if(this.isDebugViewEnabled) {
							topY = 128;
							maxHeight = 128;
						}
						if(noiseCubes[rangeIndex] == null) {
							noiseCubes[rangeIndex] = carver.getNoiseGen().interpolateNoiseCube(startPos, endPos, bottomY, maxHeight);
						}
						NoiseColumn noiseColumn = noiseCubes[rangeIndex].get(offsetX).get(offsetZ);
						//Store the needed data for carving, only carve after multithreading finishes
						this.noisethreader$columnCarverHolders[subX][subZ][offsetX][offsetZ] = new ColumnCarverHolder(rangeIndex, colPos, topY, noiseColumn, liquidBlock, flooded);
						break;
					}
					else if(range.getCarver() instanceof VanillaCaveCarver) {
						this.noisethreader$shouldCarveVanillaCaves = true;
						this.noisethreader$vanillaCarvingMask[localX][localZ] = true;
					}
				}
			}
		}
	}
	
	@Unique
	private void noisethreader$carveChunkOriginal(ChunkPrimer primer, int chunkX, int chunkZ, int[][] surfaceAltitudes, IBlockState[][] liquidBlocks) {
		if(noiseRanges.isEmpty() && !isSurfaceCavesEnabled) return;
		
		boolean shouldCarveVanillaCaves = false;
		boolean[][] vanillaCarvingMask = new boolean[16][16];
		
		for(int subX = 0; subX < 4; ++subX) {
			for(int subZ = 0; subZ < 4; ++subZ) {
				int startX = subX * 4;
				int startZ = subZ * 4;
				int endX = startX + 4 - 1;
				int endZ = startZ + 4 - 1;
				BlockPos startPos = new BlockPos(chunkX * 16 + startX, 1, chunkZ * 16 + startZ);
				BlockPos endPos = new BlockPos(chunkX * 16 + endX, 1, chunkZ * 16 + endZ);
				this.noiseRanges.forEach((rangex) -> rangex.setNoiseCube(null));
				int maxHeight = 0;
				if(!isOverrideSurfaceDetectionEnabled) {
					for(int x = startX; x < endX; x++) {
						for(int z = startZ; z < endZ; z++) {
							maxHeight = Math.max(maxHeight, surfaceAltitudes[x][z]);
						}
					}
					for(CarverNoiseRange range : noiseRanges) {
						maxHeight = Math.max(maxHeight, range.getCarver().getTopY());
					}
				}
				for(int offsetX = 0; offsetX < 4; ++offsetX) {
					for(int offsetZ = 0; offsetZ < 4; ++offsetZ) {
						int localX = startX + offsetX;
						int localZ = startZ + offsetZ;
						BlockPos colPos = new BlockPos(chunkX * 16 + localX, 1, chunkZ * 16 + localZ);
						boolean flooded = this.isFloodedUndergroundEnabled && !this.isDebugViewEnabled && BiomeDictionary.hasType(this.world.getBiome(colPos), BiomeDictionary.Type.OCEAN);
						if(!flooded || BiomeDictionary.hasType(this.world.getBiome(colPos.east()), BiomeDictionary.Type.OCEAN) && BiomeDictionary.hasType(this.world.getBiome(colPos.north()), BiomeDictionary.Type.OCEAN) && BiomeDictionary.hasType(this.world.getBiome(colPos.west()), BiomeDictionary.Type.OCEAN) && BiomeDictionary.hasType(this.world.getBiome(colPos.south()), BiomeDictionary.Type.OCEAN)) {
							int surfaceAltitude = surfaceAltitudes[localX][localZ];
							IBlockState liquidBlock = liquidBlocks[localX][localZ];
							float caveRegionNoise = this.caveRegionController.GetNoise((float)colPos.getX(), (float)colPos.getZ());
							for(CarverNoiseRange range : this.noiseRanges) {
								if(range.contains(caveRegionNoise)) {
									if(range.getCarver() instanceof CaveCarver) {
										CaveCarver carver = (CaveCarver)range.getCarver();
										int bottomY = carver.getBottomY();
										int topY = Math.min(surfaceAltitude, carver.getTopY());
										if(this.isOverrideSurfaceDetectionEnabled) {
											topY = carver.getTopY();
											maxHeight = carver.getTopY();
										}
										if(this.isDebugViewEnabled) {
											topY = 128;
											maxHeight = 128;
										}
										if(range.getNoiseCube() == null) {
											range.setNoiseCube(carver.getNoiseGen().interpolateNoiseCube(startPos, endPos, bottomY, maxHeight));
										}
										NoiseColumn noiseColumn = range.getNoiseCube().get(offsetX).get(offsetZ);
										carver.carveColumn(primer, colPos, topY, noiseColumn, liquidBlock, flooded);
										break;
									}
									if(range.getCarver() instanceof VanillaCaveCarver) {
										vanillaCarvingMask[localX][localZ] = true;
										shouldCarveVanillaCaves = true;
									}
								}
							}
						}
					}
				}
			}
		}
		
		if(shouldCarveVanillaCaves) {
			VanillaCaveCarver carver = null;
			for(CarverNoiseRange range : this.noiseRanges) {
				if(range.getCarver() instanceof VanillaCaveCarver) {
					carver = (VanillaCaveCarver)range.getCarver();
					break;
				}
			}
			if(carver != null) {
				carver.generate(this.world, chunkX, chunkZ, primer, true, liquidBlocks, vanillaCarvingMask);
			}
		}
		if(this.isSurfaceCavesEnabled) {
			this.surfaceCaveCarver.generate(this.world, chunkX, chunkZ, primer, false, liquidBlocks);
		}
	}
}