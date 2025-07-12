package noisethreader.handlers;

import java.util.concurrent.ForkJoinPool;

public class ThreadHandler {
	
	private static int threadSize = -1;

	public static int getPoolSize() {
		if(threadSize == -1) {
			threadSize = ForkJoinPool.commonPool().getParallelism();
		}
		return threadSize;
	}
}