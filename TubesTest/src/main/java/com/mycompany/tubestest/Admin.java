/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.tubestest;

/**
 *
 * @author demo
 */
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