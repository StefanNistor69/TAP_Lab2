package main.metrics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public class LatencyMetrics {
	private final int windowSize;
	private final Map<String, Deque<Long>> modelToLatencies = new HashMap<>();

	public LatencyMetrics(int windowSize) {
		this.windowSize = windowSize;
	}

	public synchronized void record(String modelName, long latencyMillis) {
		Deque<Long> queue = modelToLatencies.computeIfAbsent(modelName, k -> new ArrayDeque<>());
		queue.addLast(latencyMillis);
		while (queue.size() > windowSize) {
			queue.removeFirst();
		}
	}

	public synchronized double getP95(String modelName) {
		Deque<Long> queue = modelToLatencies.get(modelName);
		if (queue == null || queue.isEmpty()) {
			return Double.POSITIVE_INFINITY;
		}
		List<Long> copy = new ArrayList<>(queue);
		Collections.sort(copy);
		int index = (int)Math.ceil(0.95 * copy.size()) - 1;
		index = Math.max(0, Math.min(index, copy.size() - 1));
		return copy.get(index);
	}
}
