package main.api;

public class PredictionRequest {
	private final String id;
	private final String payload;
	private final int priority;

	public PredictionRequest(String id, String payload) {
		this(id, payload, 0);
	}

	public PredictionRequest(String id, String payload, int priority) {
		this.id = id;
		this.payload = payload;
		this.priority = priority;
	}

	public String getId() {
		return id;
	}

	public String getPayload() {
		return payload;
	}

	public int getPriority() {
		return priority;
	}
}
