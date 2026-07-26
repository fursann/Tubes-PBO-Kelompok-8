/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.tubestest;

/**
 *
 * @author demo
 */
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
