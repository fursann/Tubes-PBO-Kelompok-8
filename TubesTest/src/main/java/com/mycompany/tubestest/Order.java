/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.tubestest;

/**
 *
 * @author demo
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order {
    private final String orderId;
    private double totalPrice;
    private final List<Service> services;

    public Order(String orderId) {
        this.orderId = orderId;
        this.services = new ArrayList<>();
    }

    public void addService(Service service) {
        services.add(service);
        totalPrice += service.getPrice();
    }

    public String getOrderId() {
        return orderId;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public List<Service> getServices() {
        return Collections.unmodifiableList(services);
    }
}