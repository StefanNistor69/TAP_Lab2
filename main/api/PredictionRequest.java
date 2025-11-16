package main.api;

public class PredictionRequest {
	private final String id;
	private final String payload;

	public PredictionRequest(String id, String payload) {
		this.id = id;
		this.payload = payload;
	}

	public String getId() {
		return id;
	}

	public String getPayload() {
		return payload;
	}
}
