 -- Create database
 drop database rental_db;
CREATE DATABASE IF NOT EXISTS rental_db;
USE rental_db;

-- ----------------- Owners -----------------
CREATE TABLE IF NOT EXISTS owners (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(50)
);

-- Sample data
INSERT INTO owners (name, phone) VALUES
('Alice Johnson', '+1234567890'),
('Bob Smith', '+1987654321');

-- ----------------- Properties -----------------
CREATE TABLE IF NOT EXISTS properties (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(100),
    rent DOUBLE,
    owner_id INT,
    FOREIGN KEY (owner_id) REFERENCES owners(id) ON DELETE SET NULL
);

-- Sample data
INSERT INTO properties (name, location, rent, owner_id) VALUES
('Sunny Apartments', 'New York', 1500, 1),
('Green Villa', 'Los Angeles', 2500, 2),
('Lakeview Condo', 'Chicago', 1800, 1);

-- ----------------- Tenants -----------------
CREATE TABLE IF NOT EXISTS tenants (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(50)
);

-- Sample data
INSERT INTO tenants (name, phone) VALUES
('Charlie Brown', '+1122334455'),
('Daisy Miller', '+1222333444');

-- ----------------- Leases -----------------
CREATE TABLE IF NOT EXISTS leases (
    id INT AUTO_INCREMENT PRIMARY KEY,
    property_id INT,
    tenant_id INT,
    start_date DATE,
    end_date DATE,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

-- Sample data
INSERT INTO leases (property_id, tenant_id, start_date, end_date) VALUES
(1, 1, '2025-01-01', '2025-12-31'),
(2, 2, '2025-03-01', '2026-02-28');

-- ----------------- Payments -----------------
CREATE TABLE IF NOT EXISTS payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    lease_id INT,
    amount DOUBLE,
    date DATE,
    FOREIGN KEY (lease_id) REFERENCES leases(id) ON DELETE CASCADE
);

-- Sample data
INSERT INTO payments (lease_id, amount, date) VALUES
(1, 1500, '2025-01-05'),
(1, 1500, '2025-02-05'),
(2, 2500, '2025-03-05');

-- ----------------- Users -----------------
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    role VARCHAR(20) NOT NULL,
    ref_id INT
);

-- Sample data (passwords hashed using SHA-256 of 'password123')
INSERT INTO users (username, password_hash, role, ref_id) VALUES
('admin', 'ef92b778bafe771e89245b89ecbcf8a6d36baf0cbdb53d4d8ed229dd36e47d7b', 'admin', NULL),
('alice_owner', 'ef92b778bafe771e89245b89ecbcf8a6d36baf0cbdb53d4d8ed229dd36e47d7b', 'owner', 1),
('bob_owner', 'ef92b778bafe771e89245b89ecbcf8a6d36baf0cbdb53d4d8ed229dd36e47d7b', 'owner', 2),
('charlie_tenant', 'ef92b778bafe771e89245b89ecbcf8a6d36baf0cbdb53d4d8ed229dd36e47d7b', 'tenant', 1),
('daisy_tenant', 'ef92b778bafe771e89245b89ecbcf8a6d36baf0cbdb53d4d8ed229dd36e47d7b', 'tenant', 2);
