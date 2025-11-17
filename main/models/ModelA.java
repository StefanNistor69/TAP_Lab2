package main.models;

import java.util.Random;

import main.annotations.Serve;
import main.api.ModelHandler;
import main.api.PredictionRequest;
import main.api.PredictionResult;

@Serve("ModelA")
public class ModelA implements ModelHandler {
	private final Random random = new Random();

	@Override
	public PredictionResult predict(PredictionRequest request) {
		long start = System.currentTimeMillis();
		boolean success = true;
		String message = "ok";
		double value = 0.0;
		try {
			// Simulate compute time: 40-80ms
			Thread.sleep(40 + random.nextInt(41));
			// 10% failure rate
			if (random.nextDouble() < 0.50) {
				throw new RuntimeException("Transient error in ModelA");
			}
			value = random.nextDouble();
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
			.modelName("ModelA")
			.success(success)
			.message(message)
			.value(value)
			.latencyMillis(latency)
			.build();
	}
}
