-- =======================================================
-- Smart Parking Management System - Database Schema Setup
-- Database: smart_parking
-- =======================================================

CREATE DATABASE IF NOT EXISTS smart_parking;
USE smart_parking;

-- Table: parking_slots
CREATE TABLE IF NOT EXISTS parking_slots (
    slot_id INT AUTO_INCREMENT PRIMARY KEY,
    slot_name VARCHAR(50) NOT NULL UNIQUE,
    is_available BOOLEAN NOT NULL DEFAULT TRUE
);

-- Table: reservations
CREATE TABLE IF NOT EXISTS reservations (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    slot_id INT NOT NULL,
    reservation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_slot FOREIGN KEY (slot_id) REFERENCES parking_slots(slot_id) ON DELETE CASCADE
);

-- Seed initial slots if the table is empty
INSERT IGNORE INTO parking_slots (slot_id, slot_name, is_available) VALUES
(1, 'A-101', TRUE),
(2, 'A-102', TRUE),
(3, 'A-103', TRUE),
(4, 'B-201', TRUE),
(5, 'B-202', TRUE),
(6, 'B-203', TRUE),
(7, 'C-301', TRUE),
(8, 'C-302', TRUE),
(9, 'D-401', TRUE),
(10, 'D-402', TRUE);
