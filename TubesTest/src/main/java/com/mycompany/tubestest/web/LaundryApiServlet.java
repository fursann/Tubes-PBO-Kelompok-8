package com.mycompany.tubestest.web;

import com.mycompany.tubestest.LaundryService;
import com.mycompany.tubestest.LaundryService.CreateOrderResult;
import com.mycompany.tubestest.OrderReport;
import com.mycompany.tubestest.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet REST API — menghubungkan web/index.html dengan LaundryService.
 */
public class LaundryApiServlet extends HttpServlet {

    private final LaundryService service = LaundryService.getInstance();

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        addCors(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        addCors(resp);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json;charset=UTF-8");

        String path = pathInfo(req);
        try {
            switch (path) {
                case "/health" -> writeJson(resp, Map.of("ok", true, "app", "CleanHub"));
                case "/services" -> writeJson(resp, Map.of("services", service.getServiceCatalogJson()));
                case "/orders" -> writeJson(resp, Map.of("reports", service.getReportsJson()));
                case "/search" -> {
                    String q = req.getParameter("q");
                    writeJson(resp, Map.of("success", true, "query", q != null ? q : "", "reports", service.searchOrdersJson(q)));
                }
                case "/admin/users" -> writeJson(resp, Map.of("users", service.getUsersJson()));
                case "/admin/reports" -> writeJson(resp, Map.of("reports", service.getReportsJson()));
                default -> sendError(resp, 404, "Endpoint tidak ditemukan.");
            }
        } catch (Exception ex) {
            sendError(resp, 500, ex.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        addCors(resp);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json;charset=UTF-8");

        String path = pathInfo(req);
        String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        try {
            switch (path) {
                case "/login" -> handleLogin(body, resp);
                case "/register" -> handleRegister(body, resp);
                case "/orders" -> handleCreateOrder(body, resp);
                case "/orders/check" -> handleCheckOrder(body, resp);
                case "/orders/complaint" -> handleComplaint(body, resp);
                case "/orders/status" -> handleUpdateStatus(body, resp);
                case "/admin/orders/delete" -> handleDeleteOrder(body, resp);
                case "/admin/users/delete" -> handleDeleteUser(body, resp);
                default -> sendError(resp, 404, "Endpoint tidak ditemukan: " + path);
            }
        } catch (Exception ex) {
            sendError(resp, 500, ex.getMessage());
        }
    }

    private void handleLogin(String body, HttpServletResponse resp) throws IOException {
        Map<String, String> data = JsonUtil.parseObject(body);
        String id = data.get("id");
        String password = data.get("password");
        String role = data.get("role");
        String error = service.loginWithCredentials(id, password, role);
        if (error != null) {
            sendError(resp, 401, error);
            return;
        }
        User user = service.authenticate(id, password, role);
        Map<String, Object> userMap = service.userToMap(user);
        userMap.put("loginId", id == null ? "" : id.trim().toUpperCase());
        writeJson(resp, Map.of("success", true, "user", userMap));
    }

    private void handleRegister(String body, HttpServletResponse resp) throws IOException {
        Map<String, String> data = JsonUtil.parseObject(body);
        String roleChoice = data.get("roleChoice");
        String id = data.get("id");
        String name = data.get("name");
        String password = data.get("password");
        boolean isMember = JsonUtil.parseBoolean(body, "isMember");

        String error = service.register(roleChoice, id, name, isMember, password);
        if (error != null) {
            sendError(resp, 400, error);
            return;
        }
        User created = service.login(id);
        writeJson(resp, Map.of(
                "success", true,
                "message", "Registrasi berhasil.",
                "user", created != null ? service.userToMap(created) : Map.of()
        ));
    }

    private void handleCreateOrder(String body, HttpServletResponse resp) throws IOException {
        Map<String, String> data = JsonUtil.parseObject(body);
        String customerId = data.get("customerId");
        List<Integer> indexes = JsonUtil.parseIntArray(body, "serviceIndexes");
        double weightKg = JsonUtil.parseDouble(body, "weightKg");
        if (weightKg <= 0 && data.get("weightKg") != null) {
            try {
                weightKg = Double.parseDouble(data.get("weightKg"));
            } catch (NumberFormatException ignored) {
            }
        }

        CreateOrderResult result = service.createOrder(customerId, indexes, weightKg);
        if (!result.success()) {
            sendError(resp, 400, result.message());
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
        writeJson(resp, payload);
    }

    private void handleCheckOrder(String body, HttpServletResponse resp) throws IOException {
        if (service.getReports().isEmpty()) {
            sendError(resp, 400, "Belum ada pesanan.");
            return;
        }
        Map<String, String> data = JsonUtil.parseObject(body);
        OrderReport report = service.findReport(data.get("orderId"));
        if (report == null) {
            sendError(resp, 404, "Pesanan tidak ditemukan.");
            return;
        }
        writeJson(resp, Map.of("success", true, "report", service.reportToMap(report)));
    }

    private void handleComplaint(String body, HttpServletResponse resp) throws IOException {
        Map<String, String> data = JsonUtil.parseObject(body);
        String error = service.addComplaint(data.get("orderId"), data.get("complaint"));
        if (error != null) {
            sendError(resp, 400, error);
            return;
        }
        writeJson(resp, Map.of("success", true, "message", "Keluhan berhasil disimpan."));
    }

    private void handleUpdateStatus(String body, HttpServletResponse resp) throws IOException {
        Map<String, String> data = JsonUtil.parseObject(body);
        String error = service.updateOrderStatus(data.get("orderId"), data.get("status"));
        if (error != null) {
            sendError(resp, 400, error);
            return;
        }
        OrderReport report = service.findReport(data.get("orderId"));
        writeJson(resp, Map.of(
                "success", true,
                "message", "Status diperbarui.",
                "report", report != null ? service.reportToMap(report) : Map.of()
        ));
    }

    private void handleDeleteOrder(String body, HttpServletResponse resp) throws IOException {
        Map<String, String> data = JsonUtil.parseObject(body);
        String error = service.deleteOrder(data.get("orderId"));
        if (error != null) {
            sendError(resp, 400, error);
            return;
        }
        writeJson(resp, Map.of("success", true, "message", "Pesanan berhasil dihapus."));
    }

    private void handleDeleteUser(String body, HttpServletResponse resp) throws IOException {
        Map<String, String> data = JsonUtil.parseObject(body);
        String error = service.deleteUser(data.get("id"), data.get("currentAdminId"));
        if (error != null) {
            sendError(resp, 400, error);
            return;
        }
        writeJson(resp, Map.of("success", true, "message", "User berhasil dihapus."));
    }

    private String pathInfo(HttpServletRequest req) {
        String path = req.getPathInfo();
        return path == null ? "/" : path;
    }

    private void writeJson(HttpServletResponse resp, Map<String, ?> data) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(JsonUtil.toJson(data));
    }

    private void sendError(HttpServletResponse resp, int code, String message) throws IOException {
        resp.setStatus(code);
        resp.getWriter().write(JsonUtil.toJson(Map.of("success", false, "message", message)));
    }

    private void addCors(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }
}
