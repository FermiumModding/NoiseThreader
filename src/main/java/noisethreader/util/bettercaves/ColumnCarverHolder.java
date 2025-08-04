package noisethreader.util.bettercaves;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

public class ColumnCarverHolder {
	
	public final int carverIndex;
	public final BlockPos colPos;
	public final int topY;
	public final NoiseColumnNew noiseColumn;
	public final IBlockState liquidBlock;
	public final boolean flooded;
	
	public ColumnCarverHolder(int carverIndex, BlockPos colPos, int topY, NoiseColumnNew noiseColumn, IBlockState liquidBlock, boolean flooded) {
		this.carverIndex = carverIndex;
		this.colPos = colPos;
		this.topY = topY;
		this.noiseColumn = noiseColumn;
		this.liquidBlock = liquidBlock;
		this.flooded = flooded;
	}
}