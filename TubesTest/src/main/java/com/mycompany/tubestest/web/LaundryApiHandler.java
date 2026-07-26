package com.mycompany.tubestest.web;

import com.mycompany.tubestest.LaundryService;
import com.mycompany.tubestest.LaundryService.CreateOrderResult;
import com.mycompany.tubestest.LaundryService.DeleteAllOrdersResult;
import com.mycompany.tubestest.LaundryService.UpdateAdminResult;
import com.mycompany.tubestest.OrderReport;
import com.mycompany.tubestest.User;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Handler REST /api/* */
public class LaundryApiHandler implements HttpHandler {

    private final LaundryService service = LaundryService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = normalizePath(exchange);
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            if ("GET".equals(method)) {
                handleGet(path, exchange);
            } else if ("POST".equals(method)) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                handlePost(path, body, exchange);
            } else {
                sendJson(exchange, 405, Map.of("success", false, "message", "Method tidak didukung"));
            }
        } catch (IllegalStateException ex) {
            sendJson(exchange, 503, Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            sendJson(exchange, 500, Map.of("success", false, "message", String.valueOf(ex.getMessage())));
        } catch (Throwable ex) {
            ex.printStackTrace();
            try {
                sendJson(exchange, 500, Map.of(
                        "success", false,
                        "message", "Server error: " + ex.getClass().getSimpleName()
                                + " — restart run.bat / NetBeans Run"));
            } catch (IOException ignored) {
            }
        }
    }

    private void handleGet(String path, HttpExchange exchange) throws IOException {
        switch (path) {
            case "/health" -> sendJson(exchange, 200, Map.of("ok", true, "app", "CleanHub"));
            case "/services" -> sendJson(exchange, 200, Map.of("services", service.getServiceCatalogJson()));
            case "/users" -> sendJson(exchange, 200, Map.of(
                    "users", service.getUsersJson(queryParam(exchange, "kind"))));
            case "/orders" -> sendJson(exchange, 200, Map.of("reports", service.getReportsJson()));
            case "/search" -> handleSearch(exchange);
            case "/admin/users" -> sendJson(exchange, 200, Map.of("users", service.getUsersJson()));
            case "/admin/reports" -> sendJson(exchange, 200, Map.of("reports", service.getReportsJson()));
            default -> notFound(exchange, path);
        }
    }

    private void handlePost(String path, String body, HttpExchange exchange) throws IOException {
        Map<String, String> data = JsonUtil.parseObject(body);

        switch (path) {
            case "/login" -> {
                String id = data.get("id");
                String password = data.get("password");
                String role = data.get("role");
                String accountMode = JsonUtil.getString(data, "accountMode", body);
                String error = service.loginWithCredentials(id, password, role, accountMode);
                if (error != null) {
                    sendJson(exchange, 401, Map.of("success", false, "message", error));
                } else {
                    User user = service.authenticate(id, password, role, accountMode);
                    if (user == null) {
                        sendJson(exchange, 500, Map.of(
                                "success", false,
                                "message", "Login gagal memuat data user. Cek koneksi MySQL."));
                    } else {
                        Map<String, Object> userMap = service.userToMap(user);
                        userMap.put("loginId", id == null ? "" : id.trim().toUpperCase());
                        sendJson(exchange, 200, Map.of("success", true, "user", userMap));
                    }
                }
            }
            case "/register" -> {
                String roleChoice = JsonUtil.getString(data, "roleChoice", body);
                if (roleChoice == null) {
                    roleChoice = JsonUtil.getString(data, "role", body);
                }
                String id = JsonUtil.getString(data, "id", body);
                String name = JsonUtil.getString(data, "name", body);
                String password = JsonUtil.getString(data, "password", body);
                String error = service.register(roleChoice, id, name, JsonUtil.parseBoolean(body, "isMember"), password);
                if (error != null) {
                    sendJson(exchange, 400, Map.of("success", false, "message", error));
                } else {
                    User created = service.login(id);
                    sendJson(exchange, 200, Map.of(
                            "success", true,
                            "message", "Registrasi berhasil dan tersimpan di MySQL.",
                            "user", created != null ? service.userToMap(created) : Map.of()));
                }
            }
            case "/orders" -> handleCreateOrder(body, data, exchange);
            case "/orders/check" -> handleCheckOrder(data, exchange);
            case "/orders/complaint" -> handleComplaint(data, exchange);
            case "/orders/status" -> handleUpdateStatus(data, exchange);
            case "/delete-order", "/admin/orders/delete" -> handleDeleteOrder(data, exchange);
            case "/delete-all-orders", "/admin/orders/clear" -> handleDeleteAllOrders(exchange);
            case "/delete-user", "/admin/users/delete" -> handleDeleteUser(data, exchange);
            case "/edit-user", "/admin/users/edit" -> handleEditUser(data, exchange);
            default -> notFound(exchange, path);
        }
    }

    private void handleSearch(HttpExchange exchange) throws IOException {
        String q = queryParam(exchange, "q");
        sendJson(exchange, 200, Map.of(
                "success", true,
                "query", q == null ? "" : q,
                "reports", service.searchOrdersJson(q)));
    }

    private String queryParam(HttpExchange exchange, String name) {
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        for (String part : raw.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && name.equals(kv[0])) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private void handleCreateOrder(String body, Map<String, String> data, HttpExchange exchange) throws IOException {
        double weightKg = JsonUtil.parseDouble(body, "weightKg");
        if (weightKg <= 0 && data.get("weightKg") != null) {
            try {
                weightKg = Double.parseDouble(data.get("weightKg"));
            } catch (NumberFormatException ignored) {
            }
        }
        CreateOrderResult result = service.createOrder(
                data.get("customerId"), JsonUtil.parseIntArray(body, "serviceIndexes"), weightKg);
        if (!result.success()) {
            sendJson(exchange, 400, Map.of("success", false, "message", result.message()));
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("orderId", result.orderId());
        payload.put("status", result.status());
        payload.put("subtotal", result.subtotal());
        payload.put("totalPrice", result.totalPrice());
        payload.put("discounted", result.discounted());
        payload.put("services", result.services());
        payload.put("weightKg", result.weightKg());
        sendJson(exchange, 200, payload);
    }

    private void handleCheckOrder(Map<String, String> data, HttpExchange exchange) throws IOException {
        if (service.getReports().isEmpty()) {
            sendJson(exchange, 400, Map.of("success", false, "message", "Belum ada pesanan."));
            return;
        }
        OrderReport report = service.findReport(data.get("orderId"));
        if (report == null) {
            sendJson(exchange, 404, Map.of("success", false, "message", "Pesanan tidak ditemukan."));
        } else {
            sendJson(exchange, 200, Map.of("success", true, "report", service.reportToMap(report)));
        }
    }

    private void handleComplaint(Map<String, String> data, HttpExchange exchange) throws IOException {
        String error = service.addComplaint(data.get("orderId"), data.get("complaint"));
        if (error != null) {
            sendJson(exchange, 400, Map.of("success", false, "message", error));
        } else {
            sendJson(exchange, 200, Map.of("success", true, "message", "Keluhan berhasil disimpan."));
        }
    }

    private void handleUpdateStatus(Map<String, String> data, HttpExchange exchange) throws IOException {
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

    private void handleDeleteOrder(Map<String, String> data, HttpExchange exchange) throws IOException {
        String error = service.deleteOrder(data.get("orderId"));
        if (error != null) {
            sendJson(exchange, 400, Map.of("success", false, "message", error));
        } else {
            sendJson(exchange, 200, Map.of("success", true, "message", "Pesanan berhasil dihapus."));
        }
    }

    private void handleDeleteUser(Map<String, String> data, HttpExchange exchange) throws IOException {
        String error = service.deleteUser(data.get("id"), data.get("currentAdminId"));
        if (error != null) {
            sendJson(exchange, 400, Map.of("success", false, "message", error));
        } else {
            sendJson(exchange, 200, Map.of("success", true, "message", "User berhasil dihapus."));
        }
    }

    private void handleEditUser(Map<String, String> data, HttpExchange exchange) throws IOException {
        UpdateAdminResult result = service.updateAdmin(
                data.get("oldId"), data.get("newId"), data.get("name"));
        if (!result.success()) {
            sendJson(exchange, 400, Map.of("success", false, "message", result.message()));
            return;
        }
        sendJson(exchange, 200, Map.of(
                "success", true,
                "message", "Data admin berhasil diperbarui.",
                "user", result.user()));
    }

    private void handleDeleteAllOrders(HttpExchange exchange) throws IOException {
        DeleteAllOrdersResult result = service.deleteAllOrders();
        if (!result.success()) {
            sendJson(exchange, 400, Map.of("success", false, "message", result.message()));
            return;
        }
        sendJson(exchange, 200, Map.of(
                "success", true,
                "deleted", result.deleted(),
                "message", result.deleted() > 0
                        ? result.deleted() + " riwayat transaksi dihapus."
                        : "Tidak ada riwayat transaksi untuk dihapus."));
    }

    private void notFound(HttpExchange exchange, String path) throws IOException {
        System.out.println("[API] 404 " + exchange.getRequestMethod() + " " + path);
        sendJson(exchange, 404, Map.of(
                "success", false,
                "message", "Endpoint tidak ditemukan: " + path + " — restart run.bat"));
    }

    private String normalizePath(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        if (path == null || path.isBlank()) {
            return "/";
        }
        if (path.startsWith("/api/")) {
            path = path.substring(4);
        } else if ("/api".equals(path)) {
            return "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
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
