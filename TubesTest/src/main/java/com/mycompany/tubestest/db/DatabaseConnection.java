package com.mycompany.tubestest.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Koneksi JDBC ke MySQL (phpMyAdmin).
 */
public final class DatabaseConnection {

    private static final Properties PROPS = new Properties();

    static {
        try {
            loadProperties();
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DatabaseConnection() {
    }

    private static void loadProperties() throws IOException {
        String[] resourceNames = {"database.properties", "database.properties.example"};
        for (String name : resourceNames) {
            try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream(name)) {
                if (in != null) {
                    PROPS.load(in);
                    if (name.endsWith(".example")) {
                        System.out.println("[DB] Pakai database.properties.example (copy ke database.properties jika perlu).");
                    }
                    return;
                }
            }
        }

        Path[] candidates = {
                Paths.get("TubesTest/src/main/resources/database.properties"),
                Paths.get("TubesTest/src/main/resources/database.properties.example"),
                Paths.get("src/main/resources/database.properties"),
                Paths.get("src/main/resources/database.properties.example"),
                Paths.get("../TubesTest/src/main/resources/database.properties"),
                Paths.get("../TubesTest/src/main/resources/database.properties.example")
        };
        for (Path path : candidates) {
            if (Files.isRegularFile(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    PROPS.load(in);
                    System.out.println("[DB] Config: " + path.toAbsolutePath());
                    return;
                }
            }
        }
        throw new IllegalStateException(
                "database.properties tidak ditemukan. Copy database.properties.example → database.properties");
    }

    public static Connection getConnection() throws SQLException {
        String url = withTimeouts(PROPS.getProperty("db.url"));
        Connection conn = DriverManager.getConnection(
                url,
                PROPS.getProperty("db.user"),
                PROPS.getProperty("db.password"));
        conn.setAutoCommit(true);
        return conn;
    }

    private static String withTimeouts(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);
        if (!url.contains("connectTimeout")) {
            sb.append(url.contains("?") ? "&" : "?").append("connectTimeout=5000");
        }
        if (!url.contains("socketTimeout")) {
            sb.append("&socketTimeout=10000");
        }
        return sb.toString();
    }
}
