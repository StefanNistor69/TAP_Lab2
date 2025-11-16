package main.server;

public class ModelServerConfig {
	private final int metricsWindowSize;
	private final int circuitFailureThreshold;
	private final long circuitOpenDurationMillis;
	private final int circuitHalfOpenMaxTrialCalls;

	public ModelServerConfig(
		int metricsWindowSize,
		int circuitFailureThreshold,
		long circuitOpenDurationMillis,
		int circuitHalfOpenMaxTrialCalls
	) {
		this.metricsWindowSize = metricsWindowSize;
		this.circuitFailureThreshold = circuitFailureThreshold;
		this.circuitOpenDurationMillis = circuitOpenDurationMillis;
		this.circuitHalfOpenMaxTrialCalls = circuitHalfOpenMaxTrialCalls;
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
		return new ModelServerConfig(100, 3, 5_000L, 1);
	}
}
