package main.server;

import java.util.LinkedHashMap;
import java.util.Map;

import main.annotations.Serve;
import main.api.ModelHandler;
import main.circuit.CircuitBreaker;
import main.metrics.LatencyMetrics;
import main.routing.P95RoutingStrategy;
import main.routing.RoutingStrategy;

public class ModelServerBuilder {
	private ModelServerConfig config = ModelServerConfig.defaults();
	private RoutingStrategy routingStrategy = new P95RoutingStrategy();
	private final Map<String, ModelHandler> models = new LinkedHashMap<>();

	public ModelServerBuilder withConfig(ModelServerConfig config) {
		this.config = config;
		return this;
	}

	public ModelServerBuilder withRouting(RoutingStrategy routingStrategy) {
		this.routingStrategy = routingStrategy;
		return this;
	}

	public ModelServerBuilder addModel(String name, ModelHandler handler) {
		models.put(name, handler);
		return this;
	}

	public ModelServerBuilder addAnnotatedModel(ModelHandler handler) {
		Serve annotation = handler.getClass().getAnnotation(Serve.class);
		if (annotation == null) {
			throw new IllegalArgumentException("Model " + handler.getClass().getName() + " missing @Serve annotation");
		}
		return addModel(annotation.value(), handler);
	}

	public AdaptiveModelServer build() {
		LatencyMetrics metrics = new LatencyMetrics(config.getMetricsWindowSize());
		CircuitBreaker cb = new CircuitBreaker(
			config.getCircuitFailureThreshold(),
			config.getCircuitOpenDurationMillis(),
			config.getCircuitHalfOpenMaxTrialCalls()
		);
		Router router = new Router(metrics, routingStrategy, cb);
		for (Map.Entry<String, ModelHandler> entry : models.entrySet()) {
			String name = entry.getKey();
			ModelHandler handler = entry.getValue();
			Bulkhead bulkhead = new Bulkhead(
				name,
				config.getBulkheadThreadsPerModel(),
				config.getBulkheadQueueCapacityPerModel(),
				config.getBulkheadTimeoutMillis()
			);
			router.register(name, handler, bulkhead);
		}
		return new AdaptiveModelServer(router);
	}
}
