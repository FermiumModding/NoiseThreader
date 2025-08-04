package noisethreader.util.bettercaves;

/**
 * Modified from https://github.com/YUNG-GANG/YUNGs-Better-Caves/blob/1.12.2/src/main/java/com/yungnickyoung/minecraft/bettercaves/noise/NoiseCube.java licensed LGPLv3
 * Modified to improve performance (Fix unneeded boxing/lists/hashing/etc)
 */
public class NoiseCubeNew {
	
	private final NoiseColumnNew[][] cubeValuesArrays;
	
	public NoiseCubeNew(int edgeLength) {
		this.cubeValuesArrays = new NoiseColumnNew[edgeLength][edgeLength];
	}
	
	public NoiseColumnNew[] getArray(int index) {
		return this.cubeValuesArrays[index];
	}
}