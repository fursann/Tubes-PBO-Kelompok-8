package cleanhub;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class LaundryService {

    private static final LaundryService INSTANCE = new LaundryService();

    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<Integer, Service> serviceCatalog = new LinkedHashMap<>();
    private final List<OrderReport> reports = new ArrayList<>();
    private final AtomicInteger orderCounter = new AtomicInteger(991);
    private final NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    private LaundryService() {
        seedData();
    }

    public static LaundryService getInstance() {
        return INSTANCE;
    }

    public User login(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        return users.get(rawId.trim().toUpperCase());
    }

    public String register(String roleChoice, String idRaw, String name, Boolean isMember) {
        if (idRaw == null || idRaw.isBlank()) {
            return "ID tidak boleh kosong.";
        }
        String id = idRaw.trim().toUpperCase();
        if (users.containsKey(id)) {
            return "ID sudah terdaftar.";
        }
        if (name == null || name.isBlank()) {
            return "Nama tidak boleh kosong.";
        }

        switch (roleChoice == null ? "" : roleChoice.trim()) {
            case "1" -> {
                boolean member = Boolean.TRUE.equals(isMember);
                users.put(id, new Customer(id, name.trim(), member));
            }
            case "2" -> users.put(id, new Staff(id, name.trim()));
            case "3" -> users.put(id, new Admin(id, name.trim()));
            default -> {
                return "Pilihan role tidak valid.";
            }
        }
        return null;
    }

    public List<Map<String, Object>> getServiceCatalogJson() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Integer, Service> entry : serviceCatalog.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("index", entry.getKey());
            row.put("name", entry.getValue().getServiceName());
            row.put("price", entry.getValue().getPrice());
            list.add(row);
        }
        return list;
    }

    public CreateOrderResult createOrder(String customerId, List<Integer> serviceIndexes) {
        User user = users.get(customerId == null ? "" : customerId.trim().toUpperCase());
        if (!(user instanceof Customer customer)) {
            return CreateOrderResult.fail("Customer tidak ditemukan.");
        }

        Order order = new Order("ORD-" + orderCounter.getAndIncrement());
        if (serviceIndexes != null) {
            for (Integer index : serviceIndexes) {
                if (index == null) {
                    continue;
                }
                Service selected = serviceCatalog.get(index);
                if (selected != null) {
                    order.addService(selected);
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

        OrderReport report = new OrderReport(order, "Menunggu Diproses");
        reports.add(report);

        return CreateOrderResult.ok(
                order.getOrderId(),
                report.getStatus(),
                subtotal,
                finalPrice,
                payment.validateStatus(),
                serviceNames(order));
    }

    public OrderReport findReport(String orderId) {
        if (orderId == null) {
            return null;
        }
        String target = orderId.trim();
        for (OrderReport report : reports) {
            if (report.getOrder().getOrderId().equalsIgnoreCase(target)) {
                return report;
            }
        }
        return null;
    }

    public String addComplaint(String orderId, String complaint) {
        if (reports.isEmpty()) {
            return "Belum ada pesanan.";
        }
        OrderReport report = findReport(orderId);
        if (report == null) {
            return "Pesanan tidak ditemukan.";
        }
        if (complaint == null || complaint.isBlank()) {
            return "Keluhan tidak boleh kosong";
        }
        report.setComplaint(complaint.trim());
        return null;
    }

    public String updateOrderStatus(String orderId, String newStatus) {
        if (reports.isEmpty()) {
            return "Belum ada pesanan.";
        }
        OrderReport report = findReport(orderId);
        if (report == null) {
            return "Pesanan tidak ditemukan.";
        }
        if (newStatus == null || newStatus.isBlank()) {
            return "Status tidak boleh kosong.";
        }
        report.setStatus(newStatus.trim());
        return null;
    }

    public List<Map<String, Object>> getReportsJson() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (OrderReport report : reports) {
            list.add(reportToMap(report));
        }
        return list;
    }

    public List<Map<String, Object>> getUsersJson() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (User user : users.values()) {
            list.add(userToMap(user));
        }
        return list;
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
        return map;
    }

    public String formatRupiah(double amount) {
        return rupiahFormat.format(amount);
    }

    public List<OrderReport> getReports() {
        return Collections.unmodifiableList(reports);
    }

    public Map<String, User> getUsers() {
        return Collections.unmodifiableMap(users);
    }

    public Map<Integer, Service> getServiceCatalog() {
        return Collections.unmodifiableMap(serviceCatalog);
    }

    private List<String> serviceNames(Order order) {
        List<String> names = new ArrayList<>();
        for (Service service : order.getServices()) {
            names.add(service.getServiceName());
        }
        return names;
    }

    private void seedData() {
        users.put("C-001", new Customer("C-001", "Budi", true));
        users.put("C-002", new Customer("C-002", "Sari", false));
        users.put("S-001", new Staff("S-001", "Andi"));
        users.put("A-001", new Admin("A-001", "Nadia"));

        serviceCatalog.put(1, new Service("Cuci Kering", 30000));
        serviceCatalog.put(2, new Service("Setrika", 20000));
        serviceCatalog.put(3, new Service("Cuci Express", 45000));
    }

    public record CreateOrderResult(
            boolean success,
            String message,
            String orderId,
            String status,
            double subtotal,
            double totalPrice,
            boolean discounted,
            List<String> services) {

        static CreateOrderResult ok(String orderId, String status, double subtotal,
                                    double totalPrice, boolean discounted, List<String> services) {
            return new CreateOrderResult(true, null, orderId, status, subtotal, totalPrice, discounted, services);
        }

        static CreateOrderResult fail(String message) {
            return new CreateOrderResult(false, message, null, null, 0, 0, false, List.of());
        }
    }
}
