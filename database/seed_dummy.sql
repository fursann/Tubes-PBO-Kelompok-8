-- Reset & seed data demo CleanHub
-- Jalankan di phpMyAdmin jika ingin reset manual tanpa restart server

USE cleanhub;

SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM order_services;
DELETE FROM orders;
DELETE FROM users;
SET FOREIGN_KEY_CHECKS = 1;

-- 3 Admin, 3 Staff, 3 Customer (login tanpa password)
INSERT INTO users (id, name, role, is_member, password) VALUES
  ('A-001', 'Nadia', 'Admin',    0, ''),
  ('A-002', 'Rizki', 'Admin',    0, ''),
  ('A-003', 'Dina',  'Admin',    0, ''),
  ('S-001', 'Andi',  'Staff',    0, ''),
  ('S-002', 'Rina',  'Staff',    0, ''),
  ('S-003', 'Bayu',  'Staff',    0, ''),
  ('C-001', 'Budi',  'Customer', 1, ''),
  ('C-002', 'Sari',  'Customer', 0, ''),
  ('C-003', 'Doni',  'Customer', 0, '');

-- 6 riwayat pesanan (per kg)
INSERT INTO orders (order_id, customer_id, total_price, status, complaint) VALUES
  ('ORD-001', 'C-001', 21600, 'Menunggu Diproses', '-'),
  ('ORD-002', 'C-002', 24000, 'Sedang Mencuci', '-'),
  ('ORD-003', 'C-003',  8000, 'Sedang Setrika', '-'),
  ('ORD-004', 'C-001', 27000, 'Selesai', '-'),
  ('ORD-005', 'C-002', 40000, 'Menunggu Diproses', 'Pakaian kurang wangi'),
  ('ORD-006', 'C-003', 36000, 'Dibatalkan', '-');

INSERT INTO order_services (order_id, service_index, weight_kg, line_price) VALUES
  ('ORD-001', 3, 3, 24000),
  ('ORD-002', 1, 4, 24000),
  ('ORD-003', 2, 2,  8000),
  ('ORD-004', 4, 2, 30000),
  ('ORD-005', 3, 5, 40000),
  ('ORD-006', 1, 6, 36000);

CREATE TABLE IF NOT EXISTS app_meta (
  meta_key   VARCHAR(50)  NOT NULL PRIMARY KEY,
  meta_value VARCHAR(100) NOT NULL
);
INSERT INTO app_meta (meta_key, meta_value) VALUES ('seed_version', 'demo-v4')
ON DUPLICATE KEY UPDATE meta_value = 'demo-v4';
