package cleanhub;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class LaundrySystem {
    private final LaundryService service = LaundryService.getInstance();
    private final Scanner scanner = new Scanner(System.in);

    public void start() {
        boolean running = true;
        while (running) {
            User user = login();
            if (user == null) {
                running = false;
                continue;
            }

            if (user instanceof Customer customer) {
                handleCustomer(customer);
            } else if (user instanceof Staff) {
                handleStaff();
            } else if (user instanceof Admin) {
                handleAdmin();
            }
        }
        System.out.println("Terima kasih telah menggunakan CleanHub.");
    }

    private User login() {
        System.out.println("=== LOGIN CLEANHUB ===");
        System.out.println("Ketik ID untuk login | REG = daftar user baru | EXIT = keluar");
        System.out.print("Input: ");
        String input = scanner.nextLine().trim();
        if ("EXIT".equalsIgnoreCase(input)) {
            return null;
        }
        if ("REG".equalsIgnoreCase(input)) {
            registerUser();
            return login();
        }

        User user = service.login(input);
        if (user == null) {
            System.out.println("ID tidak ditemukan.\n");
            return login();
        }

        System.out.printf("Halo, %s! (Role: %s)%n", user.getName(), user.getRole());
        System.out.println("======================");
        return user;
    }

    private void registerUser() {
        System.out.println("\n--- DAFTAR USER BARU ---");
        System.out.println("Pilih role:");
        System.out.println("1. Customer");
        System.out.println("2. Staff");
        System.out.println("3. Admin");
        System.out.print("Pilih (1-3): ");
        String roleChoice = scanner.nextLine().trim();

        System.out.print("ID user (contoh C-003, S-002): ");
        String idRaw = scanner.nextLine().trim();

        System.out.print("Nama: ");
        String name = scanner.nextLine().trim();

        Boolean isMember = null;
        if ("1".equals(roleChoice)) {
            System.out.print("Member laundry? (y/n): ");
            String memberAns = scanner.nextLine().trim();
            isMember = memberAns.equalsIgnoreCase("y")
                    || memberAns.equalsIgnoreCase("yes")
                    || memberAns.equalsIgnoreCase("ya");
        }

        String error = service.register(roleChoice, idRaw, name, isMember);
        if (error != null) {
            System.out.println("[System] " + error + "\n");
            return;
        }

        String id = idRaw.trim().toUpperCase();
        User created = service.login(id);
        if (created != null) {
            System.out.printf("[System] %s %s (%s) berhasil didaftarkan.%n%n",
                    created.getRole(), created.getName(), created.getId());
        }
    }

    private void handleCustomer(Customer customer) {
        customer.displayDashboard();
        System.out.print("Pilih: ");
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> createOrder(customer);
            case "2" -> checkOrderStatus();
            case "3" -> addComplaint();
            default -> System.out.println("Pilihan tidak valid.");
        }
        System.out.println();
    }

    private void createOrder(Customer customer) {
        System.out.println("\nDaftar Layanan:");
        for (Map.Entry<Integer, Service> entry : service.getServiceCatalog().entrySet()) {
            Service svc = entry.getValue();
            System.out.printf("%d. %s (%s)%n", entry.getKey(), svc.getServiceName(),
                    service.formatRupiah(svc.getPrice()));
        }

        System.out.print("Pilih layanan (pisahkan dengan koma): ");
        String[] selectedIndexes = scanner.nextLine().split(",");
        List<Integer> indexes = new java.util.ArrayList<>();
        for (String indexRaw : selectedIndexes) {
            try {
                indexes.add(Integer.parseInt(indexRaw.trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        LaundryService.CreateOrderResult result = service.createOrder(customer.getId(), indexes);
        if (!result.success()) {
            System.out.println(result.message());
            return;
        }

        System.out.println("Sub-total: " + service.formatRupiah(result.subtotal()));
        System.out.println("\nMengecek status membership...");
        System.out.println("Total Bayar: " + service.formatRupiah(result.totalPrice()));
        System.out.printf("%nPesanan berhasil dibuat dengan ID: %s. Status: %s.%n",
                result.orderId(), result.status());
    }

    private void checkOrderStatus() {
        if (service.getReports().isEmpty()) {
            System.out.println("Belum ada pesanan.");
            return;
        }
        System.out.print("Masukkan ID Pesanan: ");
        String orderId = scanner.nextLine().trim();
        OrderReport report = service.findReport(orderId);
        if (report == null) {
            System.out.println("Pesanan tidak ditemukan.");
            return;
        }
        System.out.printf("Status %s saat ini: %s%n",
                report.getOrder().getOrderId(), report.getStatus());
        if (!"-".equals(report.getComplaint())) {
            System.out.println("Keluhan tercatat: " + report.getComplaint());
        }
    }

    private void addComplaint() {
        System.out.print("Masukkan ID Pesanan: ");
        String orderId = scanner.nextLine().trim();
        System.out.print("Tuliskan keluhan: ");
        String complaint = scanner.nextLine().trim();
        String error = service.addComplaint(orderId, complaint);
        if (error != null) {
            System.out.println(error);
            return;
        }
        System.out.println("[System] Keluhan berhasil disimpan.");
    }

    private void handleStaff() {
        System.out.println("-- Dashboard Staff --");
        List<OrderReport> reports = service.getReports();
        if (reports.isEmpty()) {
            System.out.println("Daftar Pesanan Masuk: (Kosong)\n");
            return;
        }
        System.out.println("Daftar Pesanan Masuk:");
        for (int i = 0; i < reports.size(); i++) {
            OrderReport report = reports.get(i);
            System.out.printf("%d. %s (Status: %s)%n", i + 1,
                    report.getOrder().getOrderId(), report.getStatus());
        }

        System.out.print("\nPilih ID Pesanan untuk diupdate: ");
        String orderId = scanner.nextLine().trim();
        OrderReport report = service.findReport(orderId);
        if (report == null) {
            System.out.println("Pesanan tidak ditemukan.\n");
            return;
        }

        System.out.print("Update status baru: ");
        String newStatus = scanner.nextLine().trim();
        String error = service.updateOrderStatus(orderId, newStatus);
        if (error != null) {
            System.out.println(error + "\n");
            return;
        }
        System.out.printf("[System] Status %s berhasil diubah menjadi: %s.%n%n",
                report.getOrder().getOrderId(), report.getStatus());
    }

    private void handleAdmin() {
        System.out.println("-- Dashboard Admin --");
        System.out.println("Rekap User:");
        for (User user : service.getUsers().values()) {
            System.out.printf("- %s | %s | %s%n", user.getId(), user.getName(), user.getRole());
        }

        System.out.println("\nRekap Laporan Transaksi:");
        List<OrderReport> reports = service.getReports();
        if (reports.isEmpty()) {
            System.out.println("(Belum ada transaksi)");
            System.out.println();
            return;
        }
        for (OrderReport report : reports) {
            System.out.printf("- %s | %s | Total: %s | Keluhan: %s%n",
                    report.getOrder().getOrderId(),
                    report.getStatus(),
                    service.formatRupiah(report.getOrder().getTotalPrice()),
                    report.getComplaint());
        }
        System.out.println();
    }
}
