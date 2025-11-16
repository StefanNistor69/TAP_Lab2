package main;

import main.api.PredictionRequest;
import main.api.PredictionResult;
import main.models.ModelA;
import main.models.ModelB;
import main.server.AdaptiveModelServer;
import main.server.ModelServerBuilder;

public class main {
	public static void main(String[] args) throws Exception {
		AdaptiveModelServer server = new ModelServerBuilder()
			.addAnnotatedModel(new ModelA())
			.addAnnotatedModel(new ModelB())
			.build();

		for (int i = 1; i <= 50; i++) {
			PredictionRequest req = new PredictionRequest("req-" + i, "payload-" + i);
			PredictionResult res = server.predict(req);
			System.out.println(
				"#" + i + " -> model=" + res.getModelName()
				+ " success=" + res.isSuccess()
				+ " latencyMs=" + res.getLatencyMillis()
				+ " value=" + res.getValue()
				+ " msg=" + res.getMessage()
			);
			Thread.sleep(50);
		}
	}
}
