package main.routing;

import main.metrics.LatencyMetrics;

public class P95RoutingStrategy implements RoutingStrategy {
	@Override
	public String selectModel(Iterable<String> modelNames, LatencyMetrics metrics) {
		String bestModel = null;
		double bestP95 = Double.POSITIVE_INFINITY;
		for (String name : modelNames) {
			double p95 = metrics.getP95(name);
			if (p95 < bestP95) {
				bestP95 = p95;
				bestModel = name;
			}
		}
		return bestModel;
	}
}
