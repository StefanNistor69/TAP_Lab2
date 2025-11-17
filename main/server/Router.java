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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

public class Router {
	private final Map<String, ModelHandler> nameToModel = new HashMap<>();
	private final Map<String, Bulkhead> nameToBulkhead = new HashMap<>();
	private final LatencyMetrics metrics;
	private final RoutingStrategy routingStrategy;
	private final CircuitBreaker circuitBreaker;

	public Router(LatencyMetrics metrics, RoutingStrategy routingStrategy, CircuitBreaker circuitBreaker) {
        this.metrics = metrics;
        this.routingStrategy = routingStrategy;
        this.circuitBreaker = circuitBreaker;
	}

	public void register(String modelName, ModelHandler handler, Bulkhead bulkhead) {
		nameToModel.put(modelName, handler);
		nameToBulkhead.put(modelName, bulkhead);
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
		Bulkhead bulkhead = nameToBulkhead.get(target);

		long start = System.currentTimeMillis();
		PredictionResult result;
		try {
			result = bulkhead.call(() -> handler.predict(request), request.getPriority());
		} catch (TimeoutException te) {
			long latency = System.currentTimeMillis() - start;
			metrics.record(target, latency);
			circuitBreaker.onFailure(target);
			return new PredictionResult.Builder()
				.modelName(target)
				.success(false)
				.message("bulkhead timeout")
				.value(Double.NaN)
				.latencyMillis(latency)
				.build();
		} catch (RejectedExecutionException ree) {
			long latency = System.currentTimeMillis() - start;
			metrics.record(target, latency);
			circuitBreaker.onFailure(target);
			return new PredictionResult.Builder()
				.modelName(target)
				.success(false)
				.message("bulkhead saturated")
				.value(Double.NaN)
				.latencyMillis(latency)
				.build();
		} catch (ExecutionException ee) {
			long latency = System.currentTimeMillis() - start;
			metrics.record(target, latency);
			circuitBreaker.onFailure(target);
			return new PredictionResult.Builder()
				.modelName(target)
				.success(false)
				.message("execution error: " + ee.getCause().getMessage())
				.value(Double.NaN)
				.latencyMillis(latency)
				.build();
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			long latency = System.currentTimeMillis() - start;
			metrics.record(target, latency);
			circuitBreaker.onFailure(target);
			return new PredictionResult.Builder()
				.modelName(target)
				.success(false)
				.message("interrupted")
				.value(Double.NaN)
				.latencyMillis(latency)
				.build();
		}
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
