package main.models;

import java.util.Random;

import main.annotations.Serve;
import main.api.ModelHandler;
import main.api.PredictionRequest;
import main.api.PredictionResult;

@Serve("ModelB")
public class ModelB implements ModelHandler {
	private final Random random = new Random();

	@Override
	public PredictionResult predict(PredictionRequest request) {
		long start = System.currentTimeMillis();
		boolean success = true;
		String message = "ok";
		double value = 0.0;
		try {
			// Simulate compute time: 80-120ms
			Thread.sleep(80 + random.nextInt(41));
			// 2% failure rate
			if (random.nextDouble() < 0.02) {
				throw new RuntimeException("Transient error in ModelB");
			}
			value = 1.0 + random.nextDouble();
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			success = false;
			message = "interrupted";
		} catch (Exception e) {
			success = false;
			message = e.getMessage();
		}
		long latency = System.currentTimeMillis() - start;
		return new PredictionResult.Builder()
			.modelName("ModelB")
			.success(success)
			.message(message)
			.value(value)
			.latencyMillis(latency)
			.build();
	}
}
