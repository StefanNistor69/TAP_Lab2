package main.server;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

class Bulkhead {
	private static final AtomicLong sequence = new AtomicLong(0);

	private final PriorityThreadPoolExecutor executor;
	private final long timeoutMillis;
	private final Semaphore inFlightPermits;

	Bulkhead(String modelName, int threads, int queueCapacity, long timeoutMillis) {
		this.timeoutMillis = timeoutMillis;
		int maxInFlight = Math.max(1, threads + Math.max(0, queueCapacity));
		this.inFlightPermits = new Semaphore(maxInFlight);
		ThreadFactory factory = r -> {
			Thread t = new Thread(r);
			t.setName("bulkhead-" + modelName + "-" + t.getId());
			t.setDaemon(true);
			return t;
		};
		this.executor = new PriorityThreadPoolExecutor(threads, factory, inFlightPermits);
	}

	<T> T call(Callable<T> task, int priority) throws TimeoutException, RejectedExecutionException, ExecutionException, InterruptedException {
		if (!inFlightPermits.tryAcquire()) {
			throw new RejectedExecutionException("bulkhead saturated");
		}
		long seq = sequence.getAndIncrement();
		PriorityFutureTask<T> futureTask = new PriorityFutureTask<>(task, priority, seq, inFlightPermits);
		try {
			executor.execute(futureTask);
		} catch (RejectedExecutionException ree) {
			// If execute rejected, release permit acquired above
			inFlightPermits.release();
			throw ree;
		}
		try {
			return futureTask.get(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (TimeoutException te) {
			futureTask.cancel(true);
			throw te;
		}
	}

	private static class PriorityThreadPoolExecutor extends ThreadPoolExecutor {
		private final Semaphore inFlightPermits;

		PriorityThreadPoolExecutor(int threads, ThreadFactory factory, Semaphore inFlightPermits) {
			super(
				threads,
				threads,
				0L,
				TimeUnit.MILLISECONDS,
				new PriorityBlockingQueue<>(),
				factory,
				new ThreadPoolExecutor.AbortPolicy()
			);
			this.inFlightPermits = inFlightPermits;
		}

		@Override
		protected void afterExecute(Runnable r, Throwable t) {
			try {
				super.afterExecute(r, t);
			} finally {
				// Release one permit for any task completion (success, failure, or cancellation)
				inFlightPermits.release();
			}
		}
	}

	private static class PriorityFutureTask<T> extends FutureTask<T> implements Comparable<PriorityFutureTask<?>> {
		private final int priority;
		private final long sequenceNumber;
		private final Semaphore permits;

		PriorityFutureTask(Callable<T> callable, int priority, long sequenceNumber, Semaphore permits) {
			super(callable);
			this.priority = priority;
			this.sequenceNumber = sequenceNumber;
			this.permits = permits;
		}

		@Override
		public int compareTo(PriorityFutureTask<?> other) {
			// Higher priority runs first; if equal, lower sequence (earlier) first
			int p = Integer.compare(other.priority, this.priority);
			if (p != 0) return p;
			return Long.compare(this.sequenceNumber, other.sequenceNumber);
		}
	}
}

