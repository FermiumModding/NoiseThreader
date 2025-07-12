package noisethreader.handlers;

import com.pg85.otg.generator.noise.NoiseGeneratorPerlinOctaves;
import noisethreader.NoiseThreader;
import noisethreader.util.INoiseGeneratorPerlinOctaves;
import org.apache.logging.log4j.Level;

import java.util.stream.IntStream;

public class OTGNoiseHandler {
	
	private double[] finalNoiseArray;
	
	public double[] generateOTGNoiseOctaves(NoiseGeneratorPerlinOctaves noiseGeneratorPerlinOctaves, double[] noiseArray, int xOffset, int yOffset, int zOffset, int xSize, int ySize, int zSize, double xScale, double yScale, double zScale, int octaves, int octaveSplitSize, int octaveSplitAmount) {
		this.finalNoiseArray = noiseArray;
		
		try {
			IntStream.range(0, octaveSplitAmount).parallel().forEach(split -> this.combineArrays(((INoiseGeneratorPerlinOctaves)noiseGeneratorPerlinOctaves).noisethreader$generateNoiseOctavesThreaded(null, xOffset, yOffset, zOffset, xSize, ySize, zSize, xScale, yScale, zScale, split * octaveSplitSize, Math.min(octaves, (split + 1) * octaveSplitSize)), xSize, ySize, zSize));
		}
		catch(Exception ex) {
			NoiseThreader.LOGGER.log(Level.ERROR, "NoiseThreader OTG Multithreaded Noise encountered an error: " + ex.getMessage(), ex);
			return ((INoiseGeneratorPerlinOctaves)noiseGeneratorPerlinOctaves).noisethreader$generateNoiseOctavesOTG(noiseArray, xOffset, yOffset, zOffset, xSize, ySize, zSize, xScale, yScale, zScale);
		}
		
		return this.finalNoiseArray;
	}
	
	private synchronized void combineArrays(double[] segmentArray, int xSize, int ySize, int zSize) {
		int i = 0;
		for(int x = 0; x < xSize; ++x) {
			for(int z = 0; z < zSize; ++z) {
				for(int y = 0; y < ySize; ++y) {
					this.finalNoiseArray[i] += segmentArray[i];
					i++;
				}
			}
		}
	}
}