package com.mycompany.tubestest;

import com.mycompany.tubestest.db.DatabaseConnection;
import com.mycompany.tubestest.db.DatabaseMigrator;
import com.mycompany.tubestest.db.OrderLineItem;
import com.mycompany.tubestest.db.OrderMeta;
import com.mycompany.tubestest.db.OrderRepository;
import com.mycompany.tubestest.db.ServiceRepository;
import com.mycompany.tubestest.db.UserRepository;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Logika bisnis CleanHub — data dari MySQL (phpMyAdmin).
 */
public final class LaundryService {

    private static final LaundryService INSTANCE = new LaundryService();

    private final UserRepository userRepository = new UserRepository();
    private final ServiceRepository serviceRepository = new ServiceRepository();
    private final OrderRepository orderRepository = new OrderRepository();
    private final NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    private LaundryService() {
        testDatabaseConnection();
    }

    public static LaundryService getInstance() {
        return INSTANCE;
    }

    private void testDatabaseConnection() {
        try {
            DatabaseConnection.getConnection().close();
            System.out.println("[DB] Terhubung ke MySQL — database cleanhub.");
            DatabaseMigrator.migrate();
        } catch (SQLException e) {
            System.err.println("[DB] GAGAL terhubung ke MySQL!");
            System.err.println("     " + e.getMessage());
            System.err.println("     → Jalankan XAMPP MySQL, import database/cleanhub.sql");
            System.err.println("     → Cek TubesTest/src/main/resources/database.properties");
        }
    }

    public User login(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        try {
            return userRepository.findById(rawId);
        } catch (SQLException e) {
            throw dbException(e);
        }
    }

    public String loginWithCredentials(String rawId, String password, String role) {
        return loginWithCredentials(rawId, password, role, null);
    }

    public String loginWithCredentials(String rawId, String password, String role, String accountMode) {
        if (rawId == null || rawId.isBlank()) {
            return "ID / username wajib diisi.";
        }
        if (role == null || role.isBlank()) {
            return "Pilih role login terlebih dahulu.";
        }

        String id = rawId.trim().toUpperCase();
        try {
            if (userRepository.findById(id) == null) {
                return "ID tidak ditemukan.";
            }
            String actualRole = userRepository.getRoleById(id);
            if (actualRole == null || !role.equals(actualRole)) {
                return "Role tidak sesuai. Anda memilih login sebagai " + role + ".";
            }

            boolean dummyAccount = userRepository.isDummyAccount(id);
            if (accountMode != null && !accountMode.isBlank()) {
                String mode = accountMode.trim().toLowerCase();
                if ("live".equals(mode)) {
                    if (dummyAccount) {
                        return "Akun dummy hanya untuk Mode Dummy. Kembali ke halaman awal dan pilih Mode Dummy.";
                    }
                    if (password == null || password.isBlank()) {
                        return "Password wajib diisi.";
                    }
                } else if ("dummy".equals(mode) && !dummyAccount) {
                    return "Akun terdaftar hanya untuk Mode Live. Daftar & login via Mode Live.";
                }
            }

            if (!userRepository.passwordMatches(id, password)) {
                return "Password salah.";
            }
            return null;
        } catch (SQLException e) {
            return dbMessage(e);
        }
    }

    public User authenticate(String rawId, String password, String role) {
        return authenticate(rawId, password, role, null);
    }

    public User authenticate(String rawId, String password, String role, String accountMode) {
        String error = loginWithCredentials(rawId, password, role, accountMode);
        if (error != null) {
            return null;
        }
        return login(rawId);
    }

    public String register(String roleChoice, String idRaw, String name, Boolean isMember, String password) {
        if (idRaw == null || idRaw.isBlank()) {
            return "ID tidak boleh kosong.";
        }
        String id = idRaw.trim().toUpperCase();
        if (name == null || name.isBlank()) {
            return "Nama tidak boleh kosong.";
        }
        if (password == null || password.isBlank()) {
            return "Password wajib diisi.";
        }
        if (password.length() < 4) {
            return "Password minimal 4 karakter.";
        }

        try {
            if (userRepository.exists(id)) {
                return "ID sudah terdaftar.";
            }

            String role = normalizeRoleChoice(roleChoice);
            switch (role) {
                case "1" -> userRepository.insertCustomer(id, name.trim(), Boolean.TRUE.equals(isMember), password);
                case "2" -> userRepository.insertStaff(id, name.trim(), password);
                case "3" -> userRepository.insertAdmin(id, name.trim(), password);
                default -> {
                    return "Pilihan role tidak valid.";
                }
            }

            User saved = userRepository.findById(id);
            if (saved == null) {
                return "Gagal menyimpan user ke database. Cek koneksi MySQL.";
            }
            System.out.printf("[DB] User baru tersimpan: %s | %s | %s%n", saved.getId(), saved.getName(), saved.getRole());
            return null;
        } catch (SQLException e) {
            return dbMessage(e);
        }
    }

    public String register(String roleChoice, String idRaw, String name, Boolean isMember) {
        return register(roleChoice, idRaw, name, isMember, UserRepository.DEFAULT_PASSWORD);
    }

    public List<Map<String, Object>> getServiceCatalogJson() {
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Map.Entry<Integer, Service> entry : serviceRepository.findAll().entrySet()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("index", entry.getKey());
                row.put("name", entry.getValue().getServiceName());
                row.put("price", entry.getValue().getPrice());
                row.put("unit", "kg");
                row.put("priceLabel", formatRupiah(entry.getValue().getPrice()) + " / kg");
                list.add(row);
            }
            return list;
        } catch (SQLException e) {
            throw dbException(e);
        }
    }

    public CreateOrderResult createOrder(String customerId, List<Integer> serviceIndexes, double weightKg) {
        try {
            User user = userRepository.findById(customerId == null ? "" : customerId);
            if (!(user instanceof Customer customer)) {
                return CreateOrderResult.fail("Customer tidak ditemukan.");
            }

            if (weightKg <= 0) {
                return CreateOrderResult.fail("Berat laundry harus lebih dari 0 kg.");
            }
            if (weightKg > 500) {
                return CreateOrderResult.fail("Berat maksimal 500 kg per pesanan.");
            }

            Map<Integer, Service> catalog = serviceRepository.findAll();
            Order order = new Order(orderRepository.nextOrderId());
            List<OrderLineItem> lines = new ArrayList<>();

            if (serviceIndexes != null) {
                for (Integer index : serviceIndexes) {
                    if (index == null) {
                        continue;
                    }
                    Service selected = catalog.get(index);
                    if (selected != null) {
                        double linePrice = selected.getPrice() * weightKg;
                        String displayName = selected.getServiceName() + " (" + formatWeight(weightKg) + " kg)";
                        order.addService(new Service(displayName, linePrice));
                        lines.add(new OrderLineItem(index, weightKg, linePrice));
                    }
                }
            }

            if (order.getServices().isEmpty()) {
                return CreateOrderResult.fail("Tidak ada layanan valid yang dipilih. Pesanan dibatalkan.");
            }

            double subtotal = order.getTotalPrice();
            MembershipPayment payment = new MembershipPayment(customer);
            double finalPrice = payment.applyDiscount(subtotal);
            order.setTotalPrice(finalPrice);

            String status = "Menunggu Diproses";
            orderRepository.insertOrder(
                    order.getOrderId(),
                    customer.getId(),
                    finalPrice,
                    status,
                    lines);

            return CreateOrderResult.ok(
                    order.getOrderId(),
                    status,
                    subtotal,
                    finalPrice,
                    payment.validateStatus(),
                    serviceNames(order),
                    weightKg);
        } catch (SQLException e) {
            return CreateOrderResult.fail(dbMessage(e));
        }
    }

    public OrderReport findReport(String orderId) {
        try {
            return orderRepository.findByOrderId(orderId);
        } catch (SQLException e) {
            throw dbException(e);
        }
    }

    public String addComplaint(String orderId, String complaint) {
        try {
            if (orderRepository.isEmpty()) {
                return "Belum ada pesanan.";
            }
            if (findReport(orderId) == null) {
                return "Pesanan tidak ditemukan.";
            }
            if (complaint == null || complaint.isBlank()) {
                return "Keluhan tidak boleh kosong.";
            }
            orderRepository.updateComplaint(orderId, complaint.trim());
            return null;
        } catch (SQLException e) {
            return dbMessage(e);
        }
    }

    public String updateOrderStatus(String orderId, String newStatus) {
        try {
            if (orderRepository.isEmpty()) {
                return "Belum ada pesanan.";
            }
            if (findReport(orderId) == null) {
                return "Pesanan tidak ditemukan.";
            }
            if (newStatus == null || newStatus.isBlank()) {
                return "Status tidak boleh kosong.";
            }
            orderRepository.updateStatus(orderId, newStatus.trim());
            return null;
        } catch (SQLException e) {
            return dbMessage(e);
        }
    }

    public String deleteOrder(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return "ID pesanan tidak boleh kosong.";
        }
        try {
            if (!orderRepository.deleteByOrderId(orderId)) {
                return "Pesanan tidak ditemukan.";
            }
            return null;
        } catch (SQLException e) {
            return dbMessage(e);
        }
    }

    public String deleteUser(String userId, String currentAdminId) {
        if (userId == null || userId.isBlank()) {
            return "ID user tidak boleh kosong.";
        }
        String id = userId.trim().toUpperCase();
        try {
            if (userRepository.findById(id) == null) {
                return "User tidak ditemukan.";
            }
            if (currentAdminId != null && id.equalsIgnoreCase(currentAdminId.trim())) {
                return "Tidak dapat menghapus akun yang sedang login.";
            }
            if (userRepository.countOrdersByCustomer(id) > 0) {
                return "User masih memiliki pesanan. Hapus pesanan terlebih dahulu.";
            }
            if (!userRepository.deleteById(id)) {
                return "User tidak ditemukan.";
            }
            return null;
        } catch (SQLException e) {
            return dbMessage(e);
        }
    }

    public UpdateAdminResult updateAdmin(String oldId, String newId, String name) {
        if (oldId == null || oldId.isBlank()) {
            return UpdateAdminResult.fail("ID admin lama tidak boleh kosong.");
        }
        if (newId == null || newId.isBlank()) {
            return UpdateAdminResult.fail("ID admin baru tidak boleh kosong.");
        }
        if (name == null || name.isBlank()) {
            return UpdateAdminResult.fail("Nama admin tidak boleh kosong.");
        }

        String oldKey = oldId.trim().toUpperCase();
        String newKey = newId.trim().toUpperCase();
        String trimmedName = name.trim();

        try {
            User existing = userRepository.findById(oldKey);
            if (existing == null) {
                return UpdateAdminResult.fail("Admin tidak ditemukan.");
            }
            if (!"Admin".equals(existing.getRole())) {
                return UpdateAdminResult.fail("Hanya akun Admin yang dapat diedit dari fitur ini.");
            }
            if (!oldKey.equals(newKey) && userRepository.exists(newKey)) {
                return UpdateAdminResult.fail("ID admin baru sudah digunakan.");
            }

            userRepository.updateAdmin(oldKey, newKey, trimmedName);
            User updated = userRepository.findById(newKey);
            if (updated == null) {
                return UpdateAdminResult.fail("Gagal memuat admin setelah diperbarui.");
            }
            return UpdateAdminResult.ok(userToMap(updated));
        } catch (SQLException e) {
            return UpdateAdminResult.fail(dbMessage(e));
        }
    }

    public DeleteAllOrdersResult deleteAllOrders() {
        try {
            int deleted = orderRepository.deleteAll();
            return DeleteAllOrdersResult.ok(deleted);
        } catch (SQLException e) {
            return DeleteAllOrdersResult.fail(dbMessage(e));
        }
    }

    public List<Map<String, Object>> getReportsJson() {
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            for (OrderReport report : orderRepository.findAll()) {
                list.add(reportToMap(report));
            }
            return list;
        } catch (SQLException e) {
            throw dbException(e);
        }
    }

    public List<Map<String, Object>> getUsersJson() {
        return getUsersJson(null);
    }

    public List<Map<String, Object>> getUsersJson(String accountKind) {
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            for (User user : userRepository.findAllByAccountKind(accountKind)) {
                list.add(userToMap(user));
            }
            return list;
        } catch (SQLException e) {
            throw dbException(e);
        }
    }

    public Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("role", user.getRole());
        if (user instanceof Customer customer) {
            map.put("isMember", customer.isMember());
        }
        return map;
    }

    public Map<String, Object> reportToMap(OrderReport report) {
        Order order = report.getOrder();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderId", order.getOrderId());
        map.put("status", report.getStatus());
        map.put("complaint", report.getComplaint());
        map.put("totalPrice", order.getTotalPrice());
        map.put("services", serviceNames(order));
        try {
            Map<String, String> meta = OrderMeta.load(order.getOrderId());
            map.put("customerId", meta.getOrDefault("customerId", "-"));
            map.put("customerName", meta.getOrDefault("customerName", "-"));
        } catch (SQLException e) {
            map.put("customerId", "-");
            map.put("customerName", "-");
        }
        return map;
    }

    public List<Map<String, Object>> searchOrdersJson(String query) {
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            for (String orderId : orderRepository.searchOrderIds(query)) {
                OrderReport report = orderRepository.findByOrderId(orderId);
                if (report != null) {
                    list.add(reportToMap(report));
                }
            }
            return list;
        } catch (SQLException e) {
            throw dbException(e);
        }
    }

    public String formatRupiah(double amount) {
        return rupiahFormat.format(amount);
    }

    public List<OrderReport> getReports() {
        try {
            return orderRepository.findAll();
        } catch (SQLException e) {
            throw dbException(e);
        }
    }

    public Map<String, User> getUsers() {
        try {
            Map<String, User> map = new LinkedHashMap<>();
            for (User user : userRepository.findAll()) {
                map.put(user.getId(), user);
            }
            return map;
        } catch (SQLException e) {
            throw dbException(e);
        }
    }

    public Map<Integer, Service> getServiceCatalog() {
        try {
            return serviceRepository.findAll();
        } catch (SQLException e) {
            throw dbException(e);
        }
    }

    private List<String> serviceNames(Order order) {
        List<String> names = new ArrayList<>();
        for (Service service : order.getServices()) {
            names.add(service.getServiceName());
        }
        return names;
    }

    private IllegalStateException dbException(SQLException e) {
        return new IllegalStateException(dbMessage(e), e);
    }

    private String normalizeRoleChoice(String roleChoice) {
        if (roleChoice == null) {
            return "";
        }
        String r = roleChoice.trim();
        return switch (r) {
            case "Customer", "customer" -> "1";
            case "Staff", "staff" -> "2";
            case "Admin", "admin" -> "3";
            default -> r;
        };
    }

    private String dbMessage(SQLException e) {
        return "Koneksi database gagal. Pastikan MySQL XAMPP aktif dan database cleanhub sudah di-import. (" + e.getMessage() + ")";
    }

    private static String formatWeight(double weightKg) {
        if (Math.abs(weightKg - Math.rint(weightKg)) < 0.001) {
            return String.valueOf((long) Math.rint(weightKg));
        }
        return String.format(Locale.US, "%.2f", weightKg).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    public record CreateOrderResult(
            boolean success,
            String message,
            String orderId,
            String status,
            double subtotal,
            double totalPrice,
            boolean discounted,
            List<String> services,
            double weightKg) {

        static CreateOrderResult ok(String orderId, String status, double subtotal,
                                    double totalPrice, boolean discounted, List<String> services,
                                    double weightKg) {
            return new CreateOrderResult(true, null, orderId, status, subtotal, totalPrice, discounted, services, weightKg);
        }

        static CreateOrderResult fail(String message) {
            return new CreateOrderResult(false, message, null, null, 0, 0, false, List.of(), 0);
        }
    }

    public record UpdateAdminResult(
            boolean success,
            String message,
            Map<String, Object> user) {

        static UpdateAdminResult ok(Map<String, Object> user) {
            return new UpdateAdminResult(true, null, user);
        }

        static UpdateAdminResult fail(String message) {
            return new UpdateAdminResult(false, message, null);
        }
    }

    public record DeleteAllOrdersResult(
            boolean success,
            String message,
            int deleted) {

        static DeleteAllOrdersResult ok(int deleted) {
            return new DeleteAllOrdersResult(true, null, deleted);
        }

        static DeleteAllOrdersResult fail(String message) {
            return new DeleteAllOrdersResult(false, message, 0);
        }
    }
}
