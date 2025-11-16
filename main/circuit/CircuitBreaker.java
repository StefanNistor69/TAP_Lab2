package main.circuit;

import java.util.HashMap;
import java.util.Map;

public class CircuitBreaker {
	public enum State { CLOSED, OPEN, HALF_OPEN }

	private static class StateData {
		State state = State.CLOSED;
		int consecutiveFailures = 0;
		long lastStateChangedAtMillis = System.currentTimeMillis();
		int halfOpenTrialCalls = 0;
	}

	private final int failureThreshold;
	private final long openStateDurationMillis;
	private final int halfOpenMaxTrialCalls;
	private final Map<String, StateData> modelStates = new HashMap<>();

	public CircuitBreaker(int failureThreshold, long openStateDurationMillis, int halfOpenMaxTrialCalls) {
		this.failureThreshold = failureThreshold;
		this.openStateDurationMillis = openStateDurationMillis;
		this.halfOpenMaxTrialCalls = halfOpenMaxTrialCalls;
	}

	public synchronized boolean allowRequest(String modelName) {
		StateData data = modelStates.computeIfAbsent(modelName, k -> new StateData());
		long now = System.currentTimeMillis();

		switch (data.state) {
			case CLOSED:
				return true;
			case OPEN:
				if ((now - data.lastStateChangedAtMillis) >= openStateDurationMillis) {
					data.state = State.HALF_OPEN;
					data.halfOpenTrialCalls = 0;
					data.lastStateChangedAtMillis = now;
					return true;
				}
				return false;
			case HALF_OPEN:
				if (data.halfOpenTrialCalls < halfOpenMaxTrialCalls) {
					data.halfOpenTrialCalls++;
					return true;
				}
				return false;
			default:
				return true;
		}
	}

	public synchronized void onSuccess(String modelName) {
		StateData data = modelStates.computeIfAbsent(modelName, k -> new StateData());
		data.consecutiveFailures = 0;
		if (data.state == State.HALF_OPEN) {
			data.state = State.CLOSED;
			data.lastStateChangedAtMillis = System.currentTimeMillis();
		}
	}

	public synchronized void onFailure(String modelName) {
		StateData data = modelStates.computeIfAbsent(modelName, k -> new StateData());
		data.consecutiveFailures++;
		if (data.state == State.CLOSED && data.consecutiveFailures >= failureThreshold) {
			data.state = State.OPEN;
			data.lastStateChangedAtMillis = System.currentTimeMillis();
		} else if (data.state == State.HALF_OPEN) {
			data.state = State.OPEN;
			data.lastStateChangedAtMillis = System.currentTimeMillis();
		}
	}

	public synchronized State getState(String modelName) {
		return modelStates.computeIfAbsent(modelName, k -> new StateData()).state;
	}
}
