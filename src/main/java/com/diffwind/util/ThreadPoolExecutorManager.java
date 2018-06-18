package com.diffwind.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolExecutorManager {

	public static int poolSize = 4;
	public final static BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(100);
	public final static ThreadPoolExecutor executor = new ThreadPoolExecutor(poolSize, poolSize, 10, TimeUnit.SECONDS, workQueue);
	
}
