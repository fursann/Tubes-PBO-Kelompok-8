/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.tubestest;

/**
 *
 * @author demo
 */
public class Customer extends User {
    private final boolean isMember;

    public Customer(String id, String name, boolean isMember) {
        super(id, name, "Customer");
        this.isMember = isMember;
    }

    public boolean isMember() {
        return isMember;
    }

    @Override
    public void displayDashboard() {
        System.out.println("-- Dashboard Customer --");
        System.out.println("1. Buat Pesanan");
        System.out.println("2. Cek Status Pesanan");
        System.out.println("3. Tambah Keluhan");
    }
}