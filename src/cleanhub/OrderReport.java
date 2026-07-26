package cleanhub;

public class OrderReport {
    private final Order order;
    private String status;
    private String complaint;

    public OrderReport(Order order, String status) {
        this.order = order;
        this.status = status;
        this.complaint = "-";
    }

    public Order getOrder() {
        return order;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getComplaint() {
        return complaint;
    }

    public void setComplaint(String complaint) {
        this.complaint = complaint;
    }
}
