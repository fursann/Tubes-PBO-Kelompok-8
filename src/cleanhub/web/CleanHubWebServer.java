package cleanhub.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Server web JDK (tanpa Maven) — static web/ + API /api/*.
 * Untuk JSP penuh gunakan TubesTest + Jetty.
 */
public final class CleanHubWebServer {

    private static HttpServer server;

    private CleanHubWebServer() {
    }

    public static void start(int port) throws IOException {
        if (server != null) {
            return;
        }

        Path webRoot = resolveWebRoot();
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", new LaundryApiHandler());
        server.createContext("/", exchange -> serveStatic(webRoot, exchange));
        server.setExecutor(null);
        server.start();

        System.out.println("CleanHub Web: http://localhost:" + port + "/");
        System.out.println("  API: http://localhost:" + port + "/api/health");
        System.out.println("  (JSP /status.jsp — jalankan via TubesTest + Maven)");
        System.out.println("  Web: " + webRoot.toAbsolutePath());
    }

    public static void join() throws InterruptedException {
        if (server != null) {
            while (server != null) {
                Thread.sleep(60_000);
            }
        }
    }

    private static void serveStatic(Path webRoot, HttpExchange exchange) throws IOException {
        String uri = exchange.getRequestURI().getPath();
        if ("/".equals(uri)) {
            uri = "/index.html";
        }
        Path file = webRoot.resolve(uri.replaceFirst("^/", "")).normalize();
        if (!file.startsWith(webRoot) || !Files.isRegularFile(file)) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        String contentType = contentType(file.getFileName().toString());
        byte[] bytes = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String contentType(String name) {
        if (name.endsWith(".html")) {
            return "text/html;charset=UTF-8";
        }
        if (name.endsWith(".css")) {
            return "text/css;charset=UTF-8";
        }
        if (name.endsWith(".js")) {
            return "application/javascript;charset=UTF-8";
        }
        return "application/octet-stream";
    }

    private static Path resolveWebRoot() {
        Path[] candidates = {
                Paths.get("web"),
                Paths.get("../web"),
                Paths.get("../../web")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate.resolve("index.html"))) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException("Folder web/ tidak ditemukan. Jalankan dari root proyek.");
    }
}
