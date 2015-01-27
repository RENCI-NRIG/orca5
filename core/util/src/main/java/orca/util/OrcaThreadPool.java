package orca.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The Orca central thread pool.
 * @author aydan
 *
 */
public class OrcaThreadPool {
    public static class CannotExecuteException extends OrcaException {
 		private static final long serialVersionUID = 4715527830364112041L;
		public CannotExecuteException(String message) {
    		super(message);
    	}
    }
    
    public static final int MaxThreads = 50;
    public static final long KeepAlive = 300;
    
    private static Object lock = new Object();
    private static ThreadPoolExecutor tp;
    private static boolean isShutdown;
   
    public static void start() {
    	synchronized(lock){
    		if (tp != null){
    			throw new IllegalStateException("Already started");
    		}
                // We use an unbounded queue. As a result of this, the maximumPoolSize argument to
                // the ThreadPoolExecutor constructor has no effect.
                // Hence, maximumPoolSize is set to the largest valid value.
                // The corePoolSize argument is used as an effective maximum instead, by calling the
                // allowCoreThreadTimeOut() method on the ThreadPoolExecutor.
                // Obviously, having a maximum number of threads means that tasks submitted to this
                // thread pool should keep execution time tightly bounded.
                tp = new ThreadPoolExecutor(MaxThreads, Integer.MAX_VALUE, KeepAlive,
                                            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                                            new OrcaDaemonThreadFactory());
                tp.allowCoreThreadTimeOut(true);
        	isShutdown = false;
    	}
    }
  
    public static void shutdown() {   	
    	synchronized(lock){
    		if (tp == null || isShutdown){
    			throw new IllegalStateException("Already stopped");
    		}
    		tp.shutdown();
    		isShutdown = true;
    	}  	
    }

    public static boolean awaitTermination(long timeout) throws InterruptedException {   	
    	synchronized(lock){
    		if (tp == null){
    			throw new IllegalStateException("Already stopped");
    		}
    		if (!isShutdown){
    			throw new IllegalStateException("Not shutdown");
    		}  		
    		ThreadPoolExecutor e = tp;
    		tp = null;
    		return e.awaitTermination(timeout, TimeUnit.MILLISECONDS);
    	}  	
    }
    
    /**
     * Executes the specified task asynchronously.
     * @param task
     */
    public static void invokeLater(Runnable task) throws CannotExecuteException {
        synchronized(lock){
        	if (tp == null){
        		throw new CannotExecuteException("The thread pool is not running");
        	}
        	tp.execute(task);
        }
    }

    /**
     * Executes the specified task, waits for its completion, and returns its result
     * @param r
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public static <V> V invokeAndWait(FutureTask<V> task) throws InterruptedException, ExecutionException, CannotExecuteException {
     invokeLater(task);
     return task.get();
    }

    private static class OrcaDaemonThreadFactory implements ThreadFactory {
        private static final String threadNamePrefix = "OrcaPoolThread-";
        private static long threadCount = 0;

        public Thread newThread(Runnable r) {
            String name;

            synchronized(this) {
                name = threadNamePrefix + threadCount;
                threadCount++;
            }

            Thread t = new Thread(r, name);
            t.setDaemon(true);
            return t;
	}
    }
}
