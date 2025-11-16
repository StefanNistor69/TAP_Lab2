package main.routing;

import main.metrics.LatencyMetrics;

public interface RoutingStrategy {
	String selectModel(Iterable<String> modelNames, LatencyMetrics metrics);
}
