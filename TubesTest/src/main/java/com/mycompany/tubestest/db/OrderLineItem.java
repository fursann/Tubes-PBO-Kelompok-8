package com.mycompany.tubestest.db;

/** Satu baris layanan dalam pesanan (per kg). */
public record OrderLineItem(int serviceIndex, double weightKg, double linePrice) {
}
