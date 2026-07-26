package com.mycompany.tubestest.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Metadata pesanan (customer) untuk API / search. */
public final class OrderMeta {

    private OrderMeta() {
    }

    public static Map<String, String> load(Connection conn, String orderId) throws SQLException {
        String sql = """
                SELECT o.customer_id, u.name AS customer_name
                FROM orders o
                LEFT JOIN users u ON u.id = o.customer_id
                WHERE o.order_id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, String> map = new LinkedHashMap<>();
                if (rs.next()) {
                    map.put("customerId", rs.getString("customer_id"));
                    map.put("customerName", rs.getString("customer_name"));
                }
                return map;
            }
        }
    }

    public static Map<String, String> load(String orderId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return load(conn, orderId);
        }
    }
}
