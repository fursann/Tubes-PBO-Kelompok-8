-- Import file ini di phpMyAdmin (tab Import / SQL)
-- Database: cleanhub

CREATE DATABASE IF NOT EXISTS cleanhub
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE cleanhub;

-- User (Customer / Staff / Admin)
CREATE TABLE users (
  id         VARCHAR(20)  NOT NULL PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  role       ENUM('Customer','Staff','Admin') NOT NULL,
  is_member  TINYINT(1)   NOT NULL DEFAULT 0,
  password   VARCHAR(100) NOT NULL DEFAULT '12345'
);

-- Katalog layanan
CREATE TABLE services (
  service_index INT          NOT NULL PRIMARY KEY,
  service_name  VARCHAR(100) NOT NULL,
  price         DECIMAL(12,2) NOT NULL
);

-- Pesanan
CREATE TABLE orders (
  order_id    VARCHAR(20)  NOT NULL PRIMARY KEY,
  customer_id VARCHAR(20)  NOT NULL,
  total_price DECIMAL(12,2) NOT NULL,
  status      VARCHAR(50)  NOT NULL DEFAULT 'Menunggu Diproses',
  complaint   TEXT         NOT NULL DEFAULT '-',
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (customer_id) REFERENCES users(id)
);

-- Detail layanan per pesanan (harga per kg × berat)
CREATE TABLE order_services (
  order_id      VARCHAR(20)   NOT NULL,
  service_index INT           NOT NULL,
  weight_kg     DECIMAL(8,2)  NOT NULL DEFAULT 1,
  line_price    DECIMAL(12,2) NOT NULL,
  PRIMARY KEY (order_id, service_index),
  FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
  FOREIGN KEY (service_index) REFERENCES services(service_index)
);

-- Data demo: 3 Admin, 3 Staff, 3 Customer (login tanpa password)
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

-- price = tarif per kilogram (Rp/kg)
INSERT INTO services (service_index, service_name, price) VALUES
  (1, 'Cuci Kering', 6000),
  (2, 'Setrika Saja', 4000),
  (3, 'Cuci Komplit Reguler (Cuci, Kering, Setrika)', 8000),
  (4, 'Cuci Express (Selesai 1 Hari)', 15000);
