package com.mycompany.tubestest.db;

import com.mycompany.tubestest.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServiceRepository {

    public Map<Integer, Service> findAll() throws SQLException {
        String sql = "SELECT service_index, service_name, price FROM services ORDER BY service_index";
        Map<Integer, Service> catalog = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int index = rs.getInt("service_index");
                catalog.put(index, new Service(
                        rs.getString("service_name"),
                        rs.getDouble("price")));
            }
        }
        return catalog;
    }

    public Service findByIndex(int index) throws SQLException {
        String sql = "SELECT service_name, price FROM services WHERE service_index = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, index);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Service(rs.getString("service_name"), rs.getDouble("price"));
            }
        }
    }
}
