package noisethreader.util.bettercaves;

import com.yungnickyoung.minecraft.bettercaves.config.BCSettings;
import com.yungnickyoung.minecraft.bettercaves.noise.*;
import net.minecraft.world.World;

/**
 * Modified from https://github.com/YUNG-GANG/YUNGs-Better-Caves/blob/1.12.2/src/main/java/com/yungnickyoung/minecraft/bettercaves/noise/NoiseGen.java licensed LGPLv3
 * Modified to improve performance (Fix unneeded boxing/lists/hashing/etc)
 */
public class NoiseGenNew {
	
	private final long seed;
	private final NoiseSettings noiseSettings;
	private final float yCompression;
	private final float xzCompression;
	private INoiseLibrary[] listNoiseGens;
	
	public NoiseGenNew(World world, boolean isFastNoise, NoiseSettings noiseSettings, int numGenerators, float yComp, float xzComp) {
		this.seed = world.getSeed();
		this.noiseSettings = noiseSettings;
		this.yCompression = yComp;
		this.xzCompression = xzComp;
		this.initializeNoiseGens(isFastNoise, numGenerators);
	}
	
	private NoiseColumnNew generateNoiseColumn(int xPos, int zPos, int minHeight, int maxHeight) {
		float x = (float)xPos * this.xzCompression;
		float z = (float)zPos * this.xzCompression;
		NoiseColumnNew noiseColumn = new NoiseColumnNew(maxHeight + 1);
		for(int i = minHeight; i <= maxHeight; i++) {
			float y = (float)i * this.yCompression;
			NoiseTupleNew tuple = new NoiseTupleNew(this.listNoiseGens.length);
			for(int j = 0; j < this.listNoiseGens.length; j++) {
				tuple.set(j, this.listNoiseGens[j].GetNoise(x, y, z));
			}
			noiseColumn.put(i, tuple);
		}
		return noiseColumn;
	}
	
	public NoiseCubeNew interpolateNoiseCube(int startPosX, int startPosZ, int endPosX, int endPosZ, int minHeight, int maxHeight) {
		NoiseColumnNew noisesX0Z0 = this.generateNoiseColumn(startPosX, startPosZ, minHeight, maxHeight);
		NoiseColumnNew noisesX0Z1 = this.generateNoiseColumn(startPosX, endPosZ, minHeight, maxHeight);
		NoiseColumnNew noisesX1Z0 = this.generateNoiseColumn(endPosX, startPosZ, minHeight, maxHeight);
		NoiseColumnNew noisesX1Z1 = this.generateNoiseColumn(endPosX, endPosZ, minHeight, maxHeight);
		int subChunkSize = endPosX - startPosX + 1;
		NoiseCubeNew cube = new NoiseCubeNew(subChunkSize);
		cube.getArray(0)[0] = noisesX0Z0;
		cube.getArray(0)[subChunkSize - 1] = noisesX0Z1;
		cube.getArray(subChunkSize - 1)[0] = noisesX1Z0;
		cube.getArray(subChunkSize - 1)[subChunkSize - 1] = noisesX1Z1;
		
		for(int x = 1; x < subChunkSize - 1; x++) {
			float startCoeff = BCSettings.START_COEFFS[x];
			float endCoeff = BCSettings.END_COEFFS[x];
			NoiseColumnNew xz0 = cube.getArray(x)[0];
			if(xz0 == null) {
				xz0 = new NoiseColumnNew(maxHeight + 1);
				cube.getArray(x)[0] = xz0;
			}
			NoiseColumnNew xz = cube.getArray(x)[subChunkSize - 1];
			if(xz == null) {
				xz = new NoiseColumnNew(maxHeight + 1);
				cube.getArray(x)[subChunkSize - 1] = xz;
			}
			NoiseTupleNew startTuple;
			NoiseTupleNew endTuple;
			for(int y = minHeight; y <= maxHeight; y++) {
				startTuple = cube.getArray(0)[0].get(y);
				endTuple = cube.getArray(subChunkSize - 1)[0].get(y);
				xz0.put(y, startTuple.times(startCoeff).plus(endTuple.times(endCoeff)));
				
				startTuple = cube.getArray(0)[subChunkSize - 1].get(y);
				endTuple = cube.getArray(subChunkSize - 1)[subChunkSize - 1].get(y);
				xz.put(y, startTuple.times(startCoeff).plus(endTuple.times(endCoeff)));
			}
		}
		for(int x = 0; x < subChunkSize; x++) {
			for(int z = 1; z < subChunkSize - 1; z++) {
				float startCoeff = BCSettings.START_COEFFS[z];
				float endCoeff = BCSettings.END_COEFFS[z];
				NoiseColumnNew xz = cube.getArray(x)[z];
				if(xz == null) {
					xz = new NoiseColumnNew(maxHeight + 1);
					cube.getArray(x)[z] = xz;
				}
				for(int y = minHeight; y <= maxHeight; y++) {
					NoiseTupleNew startTuple = cube.getArray(x)[0].get(y);
					NoiseTupleNew endTuple = cube.getArray(x)[subChunkSize - 1].get(y);
					xz.put(y, startTuple.times(startCoeff).plus(endTuple.times(endCoeff)));
				}
			}
		}
		return cube;
	}
	
	private void initializeNoiseGens(boolean isFastNoise, int numGenerators) {
		this.listNoiseGens = new INoiseLibrary[numGenerators];
		if(isFastNoise) {
			for(int i = 0; i < numGenerators; ++i) {
				FastNoise noiseGen = new FastNoise();
				noiseGen.SetSeed((int)this.seed + 1111 * (i + 1));
				noiseGen.SetFractalType(this.noiseSettings.getFractalType());
				noiseGen.SetNoiseType(this.noiseSettings.getNoiseType());
				noiseGen.SetFractalOctaves(this.noiseSettings.getOctaves());
				noiseGen.SetFractalGain(this.noiseSettings.getGain());
				noiseGen.SetFrequency(this.noiseSettings.getFrequency());
				this.listNoiseGens[i] = noiseGen;
			}
		}
		else {
			for(int i = 0; i < numGenerators; ++i) {
				OpenSimplex2S noiseGen = new OpenSimplex2S(this.seed + (long)(1111 * (i + 1)));
				noiseGen.setGain(this.noiseSettings.getGain());
				noiseGen.setOctaves(this.noiseSettings.getOctaves());
				noiseGen.setFrequency(this.noiseSettings.getFrequency());
				noiseGen.setLacunarity(2.0);
				this.listNoiseGens[i] = noiseGen;
			}
		}
	}
}