package cleanhub;

public class Admin extends User {
    public Admin(String id, String name) {
        super(id, name, "Admin");
    }

    @Override
    public void displayDashboard() {
        System.out.println("-- Dashboard Admin --");
        System.out.println("1. Lihat Rekap Laporan Transaksi");
        System.out.println("2. Lihat Data Semua User");
    }
}
