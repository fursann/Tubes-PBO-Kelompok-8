/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.tubestest;

/**
 *
 * @author demo
 */
public class OrderReport {
    private final Order order;
    private String status;
    private String complaint;

    public OrderReport(Order order, String status) {
        this(order, status, "-");
    }

    public OrderReport(Order order, String status, String complaint) {
        this.order = order;
        this.status = status;
        this.complaint = complaint != null && !complaint.isBlank() ? complaint : "-";
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