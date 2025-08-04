package noisethreader.util.bettercaves;

/**
 * Modified from https://github.com/YUNG-GANG/YUNGs-Better-Caves/blob/1.12.2/src/main/java/com/yungnickyoung/minecraft/bettercaves/noise/NoiseColumn.java licensed LGPLv3
 * Modified to improve performance (Fix unneeded boxing/lists/hashing/etc)
 */
public class NoiseColumnNew {
	
	private final NoiseTupleNew[] columnValuesArray;
	
	public NoiseColumnNew(int height) {
		this.columnValuesArray = new NoiseTupleNew[height];
	}
	
	public void put(int y, NoiseTupleNew noiseTuple) {
		this.columnValuesArray[y] = noiseTuple;
	}
	
	public NoiseTupleNew get(int y) {
		return this.columnValuesArray[y];
	}
}