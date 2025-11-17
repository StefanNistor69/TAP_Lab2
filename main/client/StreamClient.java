package main.client;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class StreamClient {
	public static void main(String[] args) throws Exception {
		String baseUrl = args.length > 0 ? args[0] : "http://localhost:8080/predict";
		int priority = args.length > 1 ? Integer.parseInt(args[1]) : 0;
		long intervalMs = args.length > 2 ? Long.parseLong(args[2]) : 200;
		System.out.println("Streaming to " + baseUrl + " priority=" + priority + " intervalMs=" + intervalMs);
		int i = 0;
		while (true) {
			i++;
			String id = "cl-" + UUID.randomUUID();
			String urlStr = baseUrl + "?id=" + id + "&priority=" + priority;
			String payload = "stream-payload-" + i;
			String response = post(urlStr, payload);
			System.out.println("#" + i + " " + response);
			// crude check for continue flag
			if (response.contains("\"continue\":false")) {
				System.out.println("Server signaled stop. Exiting client.");
				break;
			}
			Thread.sleep(intervalMs);
		}
	}

	private static String post(String urlStr, String body) throws Exception {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		conn.setFixedLengthStreamingMode(bytes.length);
		conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
		conn.connect();
		try (OutputStream os = conn.getOutputStream()) {
			os.write(bytes);
		}
		byte[] resp = conn.getInputStream().readAllBytes();
		return new String(resp, StandardCharsets.UTF_8);
	}
}

