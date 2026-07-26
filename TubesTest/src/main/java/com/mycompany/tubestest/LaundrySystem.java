package com.mycompany.tubestest;

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
        System.out.println("1. Customer  2. Staff  3. Admin");
        System.out.print("Pilih role (1-3): ");
        String roleChoice = scanner.nextLine().trim();
        String role = switch (roleChoice) {
            case "1" -> "Customer";
            case "2" -> "Staff";
            case "3" -> "Admin";
            default -> null;
        };
        if (role == null) {
            System.out.println("Role tidak valid.\n");
            return login();
        }

        System.out.print("ID / Username: ");
        String input = scanner.nextLine().trim();
        if ("EXIT".equalsIgnoreCase(input)) {
            return null;
        }
        if ("REG".equalsIgnoreCase(input)) {
            registerUser();
            return login();
        }

        System.out.print("Password: ");
        String password = scanner.nextLine();

        String error;
        try {
            error = service.loginWithCredentials(input, password, role);
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage() + "\n");
            return login();
        }
        if (error != null) {
            System.out.println(error + "\n");
            return login();
        }

        User user = service.authenticate(input, password, role);
        System.out.printf("Selamat datang, %s!%n", input.trim().toUpperCase());
        System.out.printf("(%s — %s)%n", user.getName(), user.getRole());
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

        System.out.print("Password (min 4 karakter): ");
        String password = scanner.nextLine().trim();

        Boolean isMember = null;
        if ("1".equals(roleChoice)) {
            System.out.print("Member laundry? (y/n): ");
            String memberAns = scanner.nextLine().trim();
            isMember = memberAns.equalsIgnoreCase("y")
                    || memberAns.equalsIgnoreCase("yes")
                    || memberAns.equalsIgnoreCase("ya");
        }

        String error = service.register(roleChoice, idRaw, name, isMember, password);
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
        Map<Integer, Service> catalog;
        try {
            catalog = service.getServiceCatalog();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
            return;
        }
        System.out.println("\nDaftar Layanan (harga per kg):");
        for (Map.Entry<Integer, Service> entry : catalog.entrySet()) {
            Service svc = entry.getValue();
            System.out.printf("%d. %s (%s / kg)%n", entry.getKey(), svc.getServiceName(),
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

        System.out.print("Berat laundry (kg): ");
        double weightKg;
        try {
            weightKg = Double.parseDouble(scanner.nextLine().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            System.out.println("Berat tidak valid.");
            return;
        }

        LaundryService.CreateOrderResult result = service.createOrder(customer.getId(), indexes, weightKg);
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
        List<OrderReport> reports;
        try {
            reports = service.getReports();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
            return;
        }
        if (reports.isEmpty()) {
            System.out.println("Belum ada pesanan.");
            return;
        }
        System.out.print("Masukkan ID Pesanan: ");
        String orderId = scanner.nextLine().trim();
        OrderReport report;
        try {
            report = service.findReport(orderId);
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
            return;
        }
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
        List<OrderReport> reports;
        try {
            reports = service.getReports();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage() + "\n");
            return;
        }
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
        OrderReport report;
        try {
            report = service.findReport(orderId);
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage() + "\n");
            return;
        }
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
        Map<String, User> users;
        List<OrderReport> reports;
        try {
            users = service.getUsers();
            reports = service.getReports();
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage() + "\n");
            return;
        }
        System.out.println("Rekap User:");
        for (User user : users.values()) {
            System.out.printf("- %s | %s | %s%n", user.getId(), user.getName(), user.getRole());
        }

        System.out.println("\nRekap Laporan Transaksi:");
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
