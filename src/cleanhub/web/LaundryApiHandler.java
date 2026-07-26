package cleanhub.web;

import cleanhub.LaundryService;
import cleanhub.LaundryService.CreateOrderResult;
import cleanhub.OrderReport;
import cleanhub.User;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Handler HTTP untuk /api/* — logika sama dengan LaundryApiServlet. */
public class LaundryApiHandler implements HttpHandler {

    private final LaundryService service = LaundryService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path.startsWith("/api")) {
            path = path.substring(4);
        }
        if (path.isEmpty()) {
            path = "/";
        }

        try {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleGet(path, exchange);
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                handlePost(path, body, exchange);
            } else {
                sendJson(exchange, 405, Map.of("success", false, "message", "Method tidak didukung"));
            }
        } catch (Exception ex) {
            sendJson(exchange, 500, Map.of("success", false, "message", ex.getMessage()));
        }
    }

    private void handleGet(String path, HttpExchange exchange) throws IOException {
        switch (path) {
            case "/health" -> sendJson(exchange, 200, Map.of("ok", true, "app", "CleanHub"));
            case "/services" -> sendJson(exchange, 200, Map.of("services", service.getServiceCatalogJson()));
            case "/orders" -> sendJson(exchange, 200, Map.of("reports", service.getReportsJson()));
            case "/admin/users" -> sendJson(exchange, 200, Map.of("users", service.getUsersJson()));
            case "/admin/reports" -> sendJson(exchange, 200, Map.of("reports", service.getReportsJson()));
            default -> sendJson(exchange, 404, Map.of("success", false, "message", "Endpoint tidak ditemukan"));
        }
    }

    private void handlePost(String path, String body, HttpExchange exchange) throws IOException {
        switch (path) {
            case "/login" -> {
                User user = service.login(JsonUtil.parseObject(body).get("id"));
                if (user == null) {
                    sendJson(exchange, 401, Map.of("success", false, "message", "ID tidak ditemukan."));
                } else {
                    sendJson(exchange, 200, Map.of("success", true, "user", service.userToMap(user)));
                }
            }
            case "/register" -> {
                Map<String, String> data = JsonUtil.parseObject(body);
                String error = service.register(
                        data.get("roleChoice"), data.get("id"), data.get("name"),
                        JsonUtil.parseBoolean(body, "isMember"));
                if (error != null) {
                    sendJson(exchange, 400, Map.of("success", false, "message", error));
                } else {
                    User created = service.login(data.get("id"));
                    sendJson(exchange, 200, Map.of(
                            "success", true,
                            "message", "Registrasi berhasil.",
                            "user", created != null ? service.userToMap(created) : Map.of()));
                }
            }
            case "/orders" -> {
                Map<String, String> data = JsonUtil.parseObject(body);
                CreateOrderResult result = service.createOrder(
                        data.get("customerId"), JsonUtil.parseIntArray(body, "serviceIndexes"));
                if (!result.success()) {
                    sendJson(exchange, 400, Map.of("success", false, "message", result.message()));
                } else {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("success", true);
                    payload.put("orderId", result.orderId());
                    payload.put("status", result.status());
                    payload.put("subtotal", result.subtotal());
                    payload.put("totalPrice", result.totalPrice());
                    payload.put("discounted", result.discounted());
                    payload.put("services", result.services());
                    sendJson(exchange, 200, payload);
                }
            }
            case "/orders/check" -> {
                if (service.getReports().isEmpty()) {
                    sendJson(exchange, 400, Map.of("success", false, "message", "Belum ada pesanan."));
                    return;
                }
                OrderReport report = service.findReport(JsonUtil.parseObject(body).get("orderId"));
                if (report == null) {
                    sendJson(exchange, 404, Map.of("success", false, "message", "Pesanan tidak ditemukan."));
                } else {
                    sendJson(exchange, 200, Map.of("success", true, "report", service.reportToMap(report)));
                }
            }
            case "/orders/complaint" -> {
                Map<String, String> data = JsonUtil.parseObject(body);
                String error = service.addComplaint(data.get("orderId"), data.get("complaint"));
                if (error != null) {
                    sendJson(exchange, 400, Map.of("success", false, "message", error));
                } else {
                    sendJson(exchange, 200, Map.of("success", true, "message", "Keluhan berhasil disimpan."));
                }
            }
            case "/orders/status" -> {
                Map<String, String> data = JsonUtil.parseObject(body);
                String error = service.updateOrderStatus(data.get("orderId"), data.get("status"));
                if (error != null) {
                    sendJson(exchange, 400, Map.of("success", false, "message", error));
                } else {
                    OrderReport report = service.findReport(data.get("orderId"));
                    sendJson(exchange, 200, Map.of(
                            "success", true,
                            "message", "Status diperbarui.",
                            "report", report != null ? service.reportToMap(report) : Map.of()));
                }
            }
            default -> sendJson(exchange, 404, Map.of("success", false, "message", "Endpoint tidak ditemukan"));
        }
    }

    private void sendJson(HttpExchange exchange, int code, Map<String, ?> data) throws IOException {
        byte[] bytes = JsonUtil.toJson(data).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }
}
