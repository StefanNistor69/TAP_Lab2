package main.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.api.ModelHandler;
import main.api.PredictionRequest;
import main.api.PredictionResult;
import main.circuit.CircuitBreaker;
import main.metrics.LatencyMetrics;
import main.routing.RoutingStrategy;

public class Router {
	private final Map<String, ModelHandler> nameToModel = new HashMap<>();
	private final LatencyMetrics metrics;
	private final RoutingStrategy routingStrategy;
	private final CircuitBreaker circuitBreaker;

	public Router(LatencyMetrics metrics, RoutingStrategy routingStrategy, CircuitBreaker circuitBreaker) {
        this.metrics = metrics;
        this.routingStrategy = routingStrategy;
        this.circuitBreaker = circuitBreaker;
	}

	public void register(String modelName, ModelHandler handler) {
		nameToModel.put(modelName, handler);
	}

	public Map<String, ModelHandler> getRegistrySnapshot() {
		return Collections.unmodifiableMap(new HashMap<>(nameToModel));
	}

	public PredictionResult route(PredictionRequest request) {
		List<String> candidates = new ArrayList<>();
		for (String model : nameToModel.keySet()) {
			if (circuitBreaker.allowRequest(model)) {
				candidates.add(model);
			}
		}
		if (candidates.isEmpty()) {
			return new PredictionResult.Builder()
				.modelName("none")
				.success(false)
				.message("All circuits open - no available models")
				.value(Double.NaN)
				.latencyMillis(0)
				.build();
		}

		String target = routingStrategy.selectModel(candidates, metrics);
		if (target == null) {
			target = candidates.get(0);
		}
		ModelHandler handler = nameToModel.get(target);

		long start = System.currentTimeMillis();
		PredictionResult result = handler.predict(request);
		long latency = System.currentTimeMillis() - start;
		metrics.record(target, latency);

		if (result.isSuccess()) {
			circuitBreaker.onSuccess(target);
		} else {
			circuitBreaker.onFailure(target);
		}
		return result;
	}
}
