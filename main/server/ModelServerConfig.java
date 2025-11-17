package main.server;

public class ModelServerConfig {
	private final int metricsWindowSize;
	private final int circuitFailureThreshold;
	private final long circuitOpenDurationMillis;
	private final int circuitHalfOpenMaxTrialCalls;
	private final int bulkheadThreadsPerModel;
	private final int bulkheadQueueCapacityPerModel;
	private final long bulkheadTimeoutMillis;

	public ModelServerConfig(
		int metricsWindowSize,
		int circuitFailureThreshold,
		long circuitOpenDurationMillis,
		int circuitHalfOpenMaxTrialCalls,
		int bulkheadThreadsPerModel,
		int bulkheadQueueCapacityPerModel,
		long bulkheadTimeoutMillis
	) {
		this.metricsWindowSize = metricsWindowSize;
		this.circuitFailureThreshold = circuitFailureThreshold;
		this.circuitOpenDurationMillis = circuitOpenDurationMillis;
		this.circuitHalfOpenMaxTrialCalls = circuitHalfOpenMaxTrialCalls;
		this.bulkheadThreadsPerModel = bulkheadThreadsPerModel;
		this.bulkheadQueueCapacityPerModel = bulkheadQueueCapacityPerModel;
		this.bulkheadTimeoutMillis = bulkheadTimeoutMillis;
	}

	public int getMetricsWindowSize() {
		return metricsWindowSize;
	}

	public int getCircuitFailureThreshold() {
		return circuitFailureThreshold;
	}

	public long getCircuitOpenDurationMillis() {
		return circuitOpenDurationMillis;
	}

	public int getCircuitHalfOpenMaxTrialCalls() {
		return circuitHalfOpenMaxTrialCalls;
	}

	public static ModelServerConfig defaults() {
		return new ModelServerConfig(100, 3, 5_000L, 1, 4, 32, 500);
	}

	public int getBulkheadThreadsPerModel() {
		return bulkheadThreadsPerModel;
	}

	public int getBulkheadQueueCapacityPerModel() {
		return bulkheadQueueCapacityPerModel;
	}

	public long getBulkheadTimeoutMillis() {
		return bulkheadTimeoutMillis;
	}
}
