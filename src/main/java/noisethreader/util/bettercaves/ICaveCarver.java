package noisethreader.util.bettercaves;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkPrimer;

public interface ICaveCarver {
	void noisethreader$carveColumnNew(ChunkPrimer primer, BlockPos colPos, int topY, NoiseColumnNew noises, IBlockState liquidBlock, boolean flooded);
	NoiseGenNew noisethreader$getNoiseGenNew();
}