package main.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import main.api.PredictionRequest;
import main.api.PredictionResult;

public class HttpApiServer {
	private final AdaptiveModelServer modelServer;
	private HttpServer http;
	private final AtomicBoolean streamingAllowed = new AtomicBoolean(true);

	public HttpApiServer(AdaptiveModelServer modelServer) {
		this.modelServer = modelServer;
	}

	public void start(int port) throws IOException {
		http = HttpServer.create(new InetSocketAddress(port), 0);
		http.createContext("/predict", new PredictHandler());
		http.createContext("/control/start-stream", new ControlHandler(true));
		http.createContext("/control/stop-stream", new ControlHandler(false));
		http.createContext("/control/shutdown", new ShutdownHandler());
		http.setExecutor(null); // default executor
		http.start();
		System.out.println("HTTP server started on http://localhost:" + port + " (POST /predict)");
		System.out.println("Control endpoints: POST /control/start-stream, /control/stop-stream, /control/shutdown");
	}

	public void stop() {
		if (http != null) {
			http.stop(0);
		}
	}

	private class PredictHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange ex) throws IOException {
			try {
				if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
					sendText(ex, 405, "Method Not Allowed");
					return;
				}
				Map<String, String> query = parseQuery(ex.getRequestURI());
				String id = query.getOrDefault("id", UUID.randomUUID().toString());
				int priority = 0;
				try {
					priority = Integer.parseInt(query.getOrDefault("priority", "0"));
				} catch (NumberFormatException ignored) {}

				String body = readBody(ex.getRequestBody());
				if (body.isEmpty()) {
					body = query.getOrDefault("payload", "");
				}

				PredictionRequest req = new PredictionRequest(id, body, priority);
				PredictionResult res = modelServer.predict(req);

				String json = "{"
					+ "\"requestId\":\"" + escape(id) + "\","
					+ "\"model\":\"" + escape(res.getModelName()) + "\","
					+ "\"success\":" + res.isSuccess() + ","
					+ "\"latencyMs\":" + res.getLatencyMillis() + ","
					+ "\"value\":" + res.getValue() + ","
					+ "\"msg\":\"" + escape(res.getMessage()) + "\","
					+ "\"continue\":" + streamingAllowed.get()
					+ "}";

				sendJson(ex, 200, json);
			} catch (Exception e) {
				sendText(ex, 500, "Internal Server Error: " + e.getMessage());
			}
		}
	}

	private class ControlHandler implements HttpHandler {
		private final boolean allow;
		ControlHandler(boolean allow) { this.allow = allow; }
		@Override
		public void handle(HttpExchange ex) throws IOException {
			if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
				sendText(ex, 405, "Method Not Allowed");
				return;
			}
			streamingAllowed.set(allow);
			sendJson(ex, 200, "{\"ok\":true,\"streamingAllowed\":" + streamingAllowed.get() + "}");
		}
	}

	private class ShutdownHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange ex) throws IOException {
			if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
				sendText(ex, 405, "Method Not Allowed");
				return;
			}
			sendJson(ex, 200, "{\"ok\":true,\"message\":\"server shutting down\"}");
			new Thread(() -> {
				try { Thread.sleep(100); } catch (InterruptedException ignored) {}
				stop();
				System.exit(0);
			}).start();
		}
	}

	private static String readBody(InputStream is) throws IOException {
		byte[] buf = is.readAllBytes();
		return new String(buf, StandardCharsets.UTF_8).trim();
	}

	private static Map<String, String> parseQuery(URI uri) {
		Map<String, String> params = new HashMap<>();
		String raw = uri.getRawQuery();
		if (raw == null || raw.isEmpty()) return params;
		for (String pair : raw.split("&")) {
			if (pair.isEmpty()) continue;
			int idx = pair.indexOf('=');
			String k = idx >= 0 ? pair.substring(0, idx) : pair;
			String v = idx >= 0 ? pair.substring(idx + 1) : "";
			k = urlDecode(k);
			v = urlDecode(v);
			params.put(k, v);
		}
		return params;
	}

	private static String urlDecode(String s) {
		return URLDecoder.decode(s, StandardCharsets.UTF_8);
	}

	private static String escape(String s) {
		if (s == null) return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
	}

	private static void sendJson(HttpExchange ex, int code, String json) throws IOException {
		Headers headers = ex.getResponseHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		headers.add("Access-Control-Allow-Origin", "*");
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		ex.sendResponseHeaders(code, bytes.length);
		try (OutputStream os = ex.getResponseBody()) {
			os.write(bytes);
		}
	}

	private static void sendText(HttpExchange ex, int code, String text) throws IOException {
		Headers headers = ex.getResponseHeaders();
		headers.add("Content-Type", "text/plain; charset=utf-8");
		headers.add("Access-Control-Allow-Origin", "*");
		byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
		ex.sendResponseHeaders(code, bytes.length);
		try (OutputStream os = ex.getResponseBody()) {
			os.write(bytes);
		}
	}
}

