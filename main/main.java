package main;

import main.api.PredictionRequest;
import main.api.PredictionResult;
import main.models.ModelA;
import main.models.ModelB;
import main.server.AdaptiveModelServer;
import main.server.ModelServerBuilder;
import main.server.HttpApiServer;

public class main {
	public static void main(String[] args) throws Exception {
		AdaptiveModelServer server = new ModelServerBuilder()
			.addAnnotatedModel(new ModelA())
			.addAnnotatedModel(new ModelB())
			.build();

		HttpApiServer http = new HttpApiServer(server);
		http.start(8080);
		System.out.println("POST /predict?priority=0 with raw body as payload, or use ?payload=...&id=...");
		// Keep process alive
		Thread.currentThread().join();
	}
}
