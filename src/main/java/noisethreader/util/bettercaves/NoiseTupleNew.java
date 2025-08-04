package noisethreader.util.bettercaves;

/**
 * Modified from https://github.com/YUNG-GANG/YUNGs-Better-Caves/blob/1.12.2/src/main/java/com/yungnickyoung/minecraft/bettercaves/noise/NoiseTuple.java licensed LGPLv3
 * Modified to improve performance (Fix unneeded boxing/lists/hashing/etc)
 */
public class NoiseTupleNew {
	
	private final double[] noiseValuesArray;
	
	public NoiseTupleNew(int size) {
		super();
		this.noiseValuesArray = new double[size];
	}
	
	public double get(int index) {
		return this.noiseValuesArray[index];
	}
	
	public void set(int index, double newValue) {
		this.noiseValuesArray[index] = newValue;
	}
	
	//All uses expect to return new NoiseTuple
	public NoiseTupleNew times(float magnitude) {
		NoiseTupleNew result = new NoiseTupleNew(this.noiseValuesArray.length);
		for(int i = 0; i < this.noiseValuesArray.length; i++) {
			result.set(i, this.noiseValuesArray[i] * (double)magnitude);
		}
		return result;
	}
	
	//No uses require new NoiseTuple
	public NoiseTupleNew plus(NoiseTupleNew other) {
		for(int i = 0; i < this.noiseValuesArray.length; i++) {
			this.noiseValuesArray[i] += other.get(i);
		}
		return this;
	}
	
	public double[] getNoiseValuesArray() {
		return this.noiseValuesArray;
	}
}