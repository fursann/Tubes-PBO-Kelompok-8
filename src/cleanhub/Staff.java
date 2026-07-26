package cleanhub;

public class Staff extends User {
    public Staff(String id, String name) {
        super(id, name, "Staff");
    }

    @Override
    public void displayDashboard() {
        System.out.println("-- Dashboard Staff --");
        System.out.println("1. Lihat Pesanan Masuk");
        System.out.println("2. Update Status Pesanan");
    }
}
