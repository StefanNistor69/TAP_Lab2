package main.api;

public class PredictionResult {
	private final String modelName;
	private final boolean success;
	private final String message;
	private final double value;
	private final long latencyMillis;

	private PredictionResult(Builder builder) {
		this.modelName = builder.modelName;
		this.success = builder.success;
		this.message = builder.message;
		this.value = builder.value;
		this.latencyMillis = builder.latencyMillis;
	}

	public String getModelName() {
		return modelName;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getMessage() {
		return message;
	}

	public double getValue() {
		return value;
	}

	public long getLatencyMillis() {
		return latencyMillis;
	}

	public static class Builder {
		private String modelName;
		private boolean success;
		private String message;
		private double value;
		private long latencyMillis;

		public Builder modelName(String modelName) {
			this.modelName = modelName;
			return this;
		}

		public Builder success(boolean success) {
			this.success = success;
			return this;
		}

		public Builder message(String message) {
			this.message = message;
			return this;
		}

		public Builder value(double value) {
			this.value = value;
			return this;
		}

		public Builder latencyMillis(long latencyMillis) {
			this.latencyMillis = latencyMillis;
			return this;
		}

		public PredictionResult build() {
			return new PredictionResult(this);
		}
	}
}
