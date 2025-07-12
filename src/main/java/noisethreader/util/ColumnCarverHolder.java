package noisethreader.util;

import com.yungnickyoung.minecraft.bettercaves.noise.NoiseColumn;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

public class ColumnCarverHolder {
	
	public final int carverIndex;
	public final BlockPos colPos;
	public final int topY;
	public final NoiseColumn noiseColumn;
	public final IBlockState liquidBlock;
	public final boolean flooded;
	
	public ColumnCarverHolder(int carverIndex, BlockPos colPos, int topY, NoiseColumn noiseColumn, IBlockState liquidBlock, boolean flooded) {
		this.carverIndex = carverIndex;
		this.colPos = colPos;
		this.topY = topY;
		this.noiseColumn = noiseColumn;
		this.liquidBlock = liquidBlock;
		this.flooded = flooded;
	}
}