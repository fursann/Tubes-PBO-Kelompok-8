package com.mycompany.tubestest.db;

import com.mycompany.tubestest.Order;
import com.mycompany.tubestest.OrderReport;
import com.mycompany.tubestest.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderRepository {

    public boolean isEmpty() throws SQLException {
        String sql = "SELECT COUNT(*) FROM orders";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1) == 0;
        }
    }

    public List<OrderReport> findAll() throws SQLException {
        String sql = "SELECT order_id FROM orders ORDER BY created_at, order_id";
        List<OrderReport> reports = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                OrderReport report = findByOrderId(rs.getString("order_id"));
                if (report != null) {
                    reports.add(report);
                }
            }
        }
        return reports;
    }

    public OrderReport findByOrderId(String orderId) throws SQLException {
        if (orderId == null || orderId.isBlank()) {
            return null;
        }

        String sql = """
                SELECT order_id, total_price, status, complaint
                FROM orders
                WHERE UPPER(order_id) = UPPER(?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String id = rs.getString("order_id");
                Order order = new Order(id);
                order.setTotalPrice(rs.getDouble("total_price"));
                loadOrderServices(conn, id, order);
                return new OrderReport(order, rs.getString("status"), rs.getString("complaint"));
            }
        }
    }

    public String nextOrderId() throws SQLException {
        String sql = """
                SELECT COALESCE(MAX(CAST(SUBSTRING(order_id, 5) AS UNSIGNED)), 990) + 1 AS next_num
                FROM orders
                WHERE order_id LIKE 'ORD-%'
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return "ORD-" + rs.getInt("next_num");
        }
    }

    public void insertOrder(String orderId, String customerId, double totalPrice,
                            String status, List<OrderLineItem> lines) throws SQLException {
        String insertOrder = """
                INSERT INTO orders (order_id, customer_id, total_price, status, complaint)
                VALUES (?, ?, ?, ?, '-')
                """;
        String insertLine = """
                INSERT INTO order_services (order_id, service_index, weight_kg, line_price)
                VALUES (?, ?, ?, ?)
                """;

        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(insertOrder)) {
                ps.setString(1, orderId);
                ps.setString(2, customerId);
                ps.setDouble(3, totalPrice);
                ps.setString(4, status);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(insertLine)) {
                for (OrderLineItem line : lines) {
                    ps.setString(1, orderId);
                    ps.setInt(2, line.serviceIndex());
                    ps.setDouble(3, line.weightKg());
                    ps.setDouble(4, line.linePrice());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    public void updateStatus(String orderId, String status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE UPPER(order_id) = UPPER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, orderId.trim());
            ps.executeUpdate();
        }
    }

    public void updateComplaint(String orderId, String complaint) throws SQLException {
        String sql = "UPDATE orders SET complaint = ? WHERE UPPER(order_id) = UPPER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, complaint);
            ps.setString(2, orderId.trim());
            ps.executeUpdate();
        }
    }

    public boolean deleteByOrderId(String orderId) throws SQLException {
        String sql = "DELETE FROM orders WHERE UPPER(order_id) = UPPER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId.trim());
            return ps.executeUpdate() > 0;
        }
    }

    public int deleteAll() throws SQLException {
        String sql = "DELETE FROM orders";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    /**
     * Cari pesanan by ID order, nama/id customer, atau status (case-insensitive).
     */
    public List<String> searchOrderIds(String term) throws SQLException {
        if (term == null || term.isBlank()) {
            List<String> all = new ArrayList<>();
            for (OrderReport r : findAll()) {
                all.add(r.getOrder().getOrderId());
            }
            return all;
        }
        String like = "%" + term.trim().toUpperCase() + "%";
        String sql = """
                SELECT o.order_id
                FROM orders o
                LEFT JOIN users u ON u.id = o.customer_id
                WHERE UPPER(o.order_id) LIKE ?
                   OR UPPER(o.status) LIKE ?
                   OR UPPER(COALESCE(u.name, '')) LIKE ?
                   OR UPPER(COALESCE(u.id, '')) LIKE ?
                ORDER BY o.created_at DESC, o.order_id DESC
                """;
        List<String> ids = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString("order_id"));
                }
            }
        }
        return ids;
    }

    private void loadOrderServices(Connection conn, String orderId, Order order) throws SQLException {
        String sql = """
                SELECT s.service_name, os.weight_kg, os.line_price, s.price AS catalog_price
                FROM order_services os
                JOIN services s ON s.service_index = os.service_index
                WHERE os.order_id = ?
                ORDER BY os.service_index
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double weightKg = rs.getDouble("weight_kg");
                    double linePrice = rs.getDouble("line_price");
                    if (rs.wasNull() || linePrice <= 0) {
                        linePrice = rs.getDouble("catalog_price") * (weightKg > 0 ? weightKg : 1);
                    }
                    String name = rs.getString("service_name");
                    if (weightKg > 0) {
                        name = name + " (" + formatWeight(weightKg) + " kg)";
                    }
                    order.addService(new Service(name, linePrice));
                }
            }
        }
    }

    private static String formatWeight(double weightKg) {
        if (Math.abs(weightKg - Math.rint(weightKg)) < 0.001) {
            return String.valueOf((long) Math.rint(weightKg));
        }
        return String.format(Locale.US, "%.2f", weightKg).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
