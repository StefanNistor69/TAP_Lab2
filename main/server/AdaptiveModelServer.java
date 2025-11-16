package main.server;

import main.api.PredictionRequest;
import main.api.PredictionResult;

public class AdaptiveModelServer {
	private final Router router;

	public AdaptiveModelServer(Router router) {
		this.router = router;
	}

	public PredictionResult predict(PredictionRequest request) {
		return router.route(request);
	}
}
