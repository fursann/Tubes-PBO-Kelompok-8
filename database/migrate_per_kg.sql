-- Upgrade database lama ke sistem harga per kg
-- Jalankan sekali di phpMyAdmin (tab SQL) pada database cleanhub

USE cleanhub;

-- Lewati jika kolom sudah ada (abaikan error "Duplicate column")
ALTER TABLE order_services ADD COLUMN weight_kg DECIMAL(8,2) NOT NULL DEFAULT 1 AFTER service_index;
ALTER TABLE order_services ADD COLUMN line_price DECIMAL(12,2) NULL AFTER weight_kg;

UPDATE order_services os
JOIN services s ON s.service_index = os.service_index
SET os.line_price = s.price * os.weight_kg
WHERE os.line_price IS NULL;

ALTER TABLE order_services MODIFY line_price DECIMAL(12,2) NOT NULL;

-- Perbarui katalog layanan (harga = Rp per kg)
DELETE FROM services;
INSERT INTO services (service_index, service_name, price) VALUES
  (1, 'Cuci Kering', 6000),
  (2, 'Setrika Saja', 4000),
  (3, 'Cuci Komplit Reguler (Cuci, Kering, Setrika)', 8000),
  (4, 'Cuci Express (Selesai 1 Hari)', 15000);
