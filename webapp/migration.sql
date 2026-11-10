-- =============================================================
-- SmartPark — Migration: Add Customer Portal Support
-- Run this AFTER your original schema.sql
-- =============================================================

USE smart_parking;

-- Table: users (customer accounts)
CREATE TABLE IF NOT EXISTS users (
  user_id    INT AUTO_INCREMENT PRIMARY KEY,
  username   VARCHAR(100) NOT NULL UNIQUE,
  password   VARCHAR(255) NOT NULL,
  full_name  VARCHAR(100) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: reservation_requests (customer → admin approval flow)
CREATE TABLE IF NOT EXISTS reservation_requests (
  request_id    INT AUTO_INCREMENT PRIMARY KEY,
  user_id       INT NOT NULL,
  customer_name VARCHAR(100) NOT NULL,
  slot_id       INT NOT NULL,
  requested_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  admin_note    VARCHAR(255) DEFAULT NULL,
  reservation_id INT DEFAULT NULL,
  CONSTRAINT fk_rreq_user FOREIGN KEY (user_id)  REFERENCES users(user_id)          ON DELETE CASCADE,
  CONSTRAINT fk_rreq_slot FOREIGN KEY (slot_id)  REFERENCES parking_slots(slot_id)  ON DELETE CASCADE
);

-- Seed demo customer accounts (plain-text passwords for demo)
INSERT IGNORE INTO users (username, password, full_name) VALUES
('ravi',    'pass123', 'Ravi Kumar'),
('priya',   'pass123', 'Priya Sharma'),
('mehul',   'pass123', 'Mehul Patel');
