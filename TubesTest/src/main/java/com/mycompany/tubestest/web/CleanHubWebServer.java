package com.mycompany.tubestest.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

/**
 * Server web — satu router untuk /api/* dan file statis web/.
 */
public final class CleanHubWebServer {

    private static HttpServer server;
    private static LaundryApiHandler apiHandler;

    private CleanHubWebServer() {
    }

    private static LaundryApiHandler handler() {
        if (apiHandler == null) {
            apiHandler = new LaundryApiHandler();
        }
        return apiHandler;
    }

    public static void start(int port) throws IOException {
        stop();

        Path webRoot = resolveWebRoot();
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("bind")) {
                throw new IOException(
                        "Port " + port + " sudah dipakai. Tutup server lama (run.bat / Maven) atau jalankan dengan --port 9090",
                        e);
            }
            throw e;
        }

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path != null && ("/api".equals(path) || path.startsWith("/api/"))) {
                handler().handle(exchange);
            } else {
                serveStatic(webRoot, exchange);
            }
        });

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("CleanHub Web: http://localhost:" + port + "/");
        System.out.println("  API: http://localhost:" + port + "/api/health");
        System.out.println("  Web: " + webRoot.toAbsolutePath());
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public static void join() throws InterruptedException {
        if (server != null) {
            while (server != null) {
                Thread.sleep(60_000);
            }
        }
    }

    private static void serveStatic(Path webRoot, HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

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
        if (name.endsWith(".html") || name.endsWith(".jsp")) {
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
                Paths.get("../../web"),
                Paths.get("TubesTest/../web").normalize()
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate.resolve("index.html"))) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException(
                "Folder web/ tidak ditemukan. Set working directory ke folder 'tubes pbo'.");
    }
}
