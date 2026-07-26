package com.mycompany.tubestest.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Migrasi ringan + seed data demo CleanHub. */
public final class DatabaseMigrator {

    private static final String SEED_VERSION = "demo-v4";

    private DatabaseMigrator() {
    }

    public static void migrate() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            ensureOrderServiceColumns(conn);
            ensurePasswordColumn(conn);
            seedPerKgCatalog(conn);
            seedDemoDataIfNeeded(conn);
        } catch (SQLException e) {
            System.err.println("[DB] Migrasi gagal: " + e.getMessage());
            System.err.println("     → Import database/cleanhub.sql atau database/seed_dummy.sql di phpMyAdmin");
        }
    }

    private static void ensureOrderServiceColumns(Connection conn) throws SQLException {
        tryAddColumn(conn, "ALTER TABLE order_services ADD COLUMN weight_kg DECIMAL(8,2) NOT NULL DEFAULT 1 AFTER service_index");
        tryAddColumn(conn, "ALTER TABLE order_services ADD COLUMN line_price DECIMAL(12,2) NULL AFTER weight_kg");
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                    UPDATE order_services os
                    JOIN services s ON s.service_index = os.service_index
                    SET os.line_price = s.price * os.weight_kg
                    WHERE os.line_price IS NULL
                    """);
        }
    }

    private static void tryAddColumn(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (!msg.contains("duplicate") && !msg.contains("already exists")) {
                throw e;
            }
        }
    }

    private static void seedPerKgCatalog(Connection conn) throws SQLException {
        String[][] catalog = {
                {"1", "Cuci Kering", "6000"},
                {"2", "Setrika Saja", "4000"},
                {"3", "Cuci Komplit Reguler (Cuci, Kering, Setrika)", "8000"},
                {"4", "Cuci Express (Selesai 1 Hari)", "15000"}
        };
        String sql = """
                INSERT INTO services (service_index, service_name, price) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE service_name = VALUES(service_name), price = VALUES(price)
                """;
        try (var ps = conn.prepareStatement(sql)) {
            for (String[] row : catalog) {
                ps.setInt(1, Integer.parseInt(row[0]));
                ps.setString(2, row[1]);
                ps.setDouble(3, Double.parseDouble(row[2]));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void ensurePasswordColumn(Connection conn) throws SQLException {
        tryAddColumn(conn, "ALTER TABLE users ADD COLUMN password VARCHAR(100) NOT NULL DEFAULT '12345' AFTER is_member");
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                    UPDATE users SET password = '12345'
                    WHERE password IS NULL
                    """);
        }
    }

    private static void ensureAppMetaTable(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS app_meta (
                      meta_key   VARCHAR(50)  NOT NULL PRIMARY KEY,
                      meta_value VARCHAR(100) NOT NULL
                    )
                    """);
        }
    }

    private static String getMeta(Connection conn, String key) throws SQLException {
        String sql = "SELECT meta_value FROM app_meta WHERE meta_key = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("meta_value") : null;
            }
        }
    }

    private static void setMeta(Connection conn, String key, String value) throws SQLException {
        String sql = """
                INSERT INTO app_meta (meta_key, meta_value) VALUES (?, ?)
                ON DUPLICATE KEY UPDATE meta_value = VALUES(meta_value)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    private static void seedDemoDataIfNeeded(Connection conn) throws SQLException {
        ensureAppMetaTable(conn);
        if (SEED_VERSION.equals(getMeta(conn, "seed_version"))) {
            return;
        }

        try (Statement st = conn.createStatement()) {
            st.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
            st.executeUpdate("DELETE FROM order_services");
            st.executeUpdate("DELETE FROM orders");
            st.executeUpdate("DELETE FROM users");
            st.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
        }

        insertDemoUsers(conn);
        insertDemoOrders(conn);
        setMeta(conn, "seed_version", SEED_VERSION);

        System.out.println("[DB] Data demo di-reset: 9 user (3/role) + 6 pesanan. Login tanpa password.");
    }

    private static void insertDemoUsers(Connection conn) throws SQLException {
        String sql = """
                INSERT INTO users (id, name, role, is_member, password) VALUES (?, ?, ?, ?, '')
                """;
        Object[][] users = {
                {"A-001", "Nadia", "Admin", 0},
                {"A-002", "Rizki", "Admin", 0},
                {"A-003", "Dina", "Admin", 0},
                {"S-001", "Andi", "Staff", 0},
                {"S-002", "Rina", "Staff", 0},
                {"S-003", "Bayu", "Staff", 0},
                {"C-001", "Budi", "Customer", 1},
                {"C-002", "Sari", "Customer", 0},
                {"C-003", "Doni", "Customer", 0}
        };
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] row : users) {
                ps.setString(1, (String) row[0]);
                ps.setString(2, (String) row[1]);
                ps.setString(3, (String) row[2]);
                ps.setInt(4, (Integer) row[3]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void insertDemoOrders(Connection conn) throws SQLException {
        // orderId, customerId, total, status, complaint, serviceIndex, weightKg, linePrice
        Object[][] orders = {
                {"ORD-001", "C-001", 21600.0, "Menunggu Diproses", "-", 3, 3.0, 24000.0},
                {"ORD-002", "C-002", 24000.0, "Sedang Mencuci", "-", 1, 4.0, 24000.0},
                {"ORD-003", "C-003", 8000.0, "Sedang Setrika", "-", 2, 2.0, 8000.0},
                {"ORD-004", "C-001", 27000.0, "Selesai", "-", 4, 2.0, 30000.0},
                {"ORD-005", "C-002", 40000.0, "Menunggu Diproses", "Pakaian kurang wangi", 3, 5.0, 40000.0},
                {"ORD-006", "C-003", 36000.0, "Dibatalkan", "-", 1, 6.0, 36000.0}
        };

        String insertOrder = """
                INSERT INTO orders (order_id, customer_id, total_price, status, complaint)
                VALUES (?, ?, ?, ?, ?)
                """;
        String insertLine = """
                INSERT INTO order_services (order_id, service_index, weight_kg, line_price)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement orderPs = conn.prepareStatement(insertOrder);
             PreparedStatement linePs = conn.prepareStatement(insertLine)) {
            for (Object[] row : orders) {
                orderPs.setString(1, (String) row[0]);
                orderPs.setString(2, (String) row[1]);
                orderPs.setDouble(3, (Double) row[2]);
                orderPs.setString(4, (String) row[3]);
                orderPs.setString(5, (String) row[4]);
                orderPs.addBatch();

                linePs.setString(1, (String) row[0]);
                linePs.setInt(2, (Integer) row[5]);
                linePs.setDouble(3, (Double) row[6]);
                linePs.setDouble(4, (Double) row[7]);
                linePs.addBatch();
            }
            orderPs.executeBatch();
            linePs.executeBatch();
        }
    }
}
