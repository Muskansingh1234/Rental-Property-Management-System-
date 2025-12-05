-- ==========================================
-- DROP AND RECREATE DATABASE
-- ==========================================
DROP DATABASE IF EXISTS rental_db;
CREATE DATABASE rental_db;
USE rental_db;

-- ==========================================
-- OWNERS TABLE
-- ==========================================
CREATE TABLE owners (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    password VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- TENANTS TABLE
-- ==========================================
CREATE TABLE tenants (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    password VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- PROPERTIES TABLE
-- ==========================================
CREATE TABLE properties (
    id INT AUTO_INCREMENT PRIMARY KEY,
    owner_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    address VARCHAR(255),
    city VARCHAR(100),
    rent DECIMAL(10,2),
    property_type ENUM('Apartment', 'House', 'Studio', 'PG', 'Villa') DEFAULT 'Apartment',
    status ENUM('Available', 'Rented', 'Under Maintenance') DEFAULT 'Available',
    image_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES owners(id) ON DELETE CASCADE
);

-- ==========================================
-- BOOKINGS TABLE
-- ==========================================
CREATE TABLE bookings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    property_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    total_amount DECIMAL(10,2),
    status ENUM('Pending', 'Approved', 'Rejected', 'Cancelled', 'Completed') DEFAULT 'Pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE
);

-- ==========================================
-- PAYMENTS TABLE
-- ==========================================
CREATE TABLE payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    payment_method ENUM('Credit Card', 'Debit Card', 'UPI', 'Net Banking', 'Cash') DEFAULT 'UPI',
    status ENUM('Pending', 'Paid', 'Failed') DEFAULT 'Pending',
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- ==========================================
-- REVIEWS TABLE
-- ==========================================
CREATE TABLE reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    property_id INT NOT NULL,
    rating INT CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE
);

-- ==========================================
-- FAVORITES TABLE
-- ==========================================
CREATE TABLE favorites (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT NOT NULL,
    property_id INT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    UNIQUE (tenant_id, property_id)
);

-- ==========================================
-- MAINTENANCE REQUESTS TABLE
-- ==========================================
CREATE TABLE maintenance_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    property_id INT NOT NULL,
    tenant_id INT NOT NULL,
    issue_description TEXT NOT NULL,
    status ENUM('Pending', 'In Progress', 'Resolved', 'Closed') DEFAULT 'Pending',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

-- ==========================================
-- NOTIFICATIONS TABLE
-- ==========================================
CREATE TABLE notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_type ENUM('Owner', 'Tenant', 'Admin'),
    user_id INT NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- ADMIN TABLE
-- ==========================================
CREATE TABLE admin (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(100) UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- SAMPLE DATA
-- ==========================================

-- Owners
INSERT INTO owners (name, email, phone, password)
VALUES 
('Rahul Mehta', 'rahul@renthub.com', '9876543210', 'owner123'),
('Priya Sharma', 'priya@renthub.com', '9123456789', 'owner456');

-- Tenants
INSERT INTO tenants (name, email, phone, password)
VALUES 
('Amit Verma', 'amit@tenant.com', '9988776655', 'tenant123'),
('Sneha Patil', 'sneha@tenant.com', '8877665544', 'tenant456');

-- Properties
INSERT INTO properties (owner_id, title, description, address, city, rent, property_type, image_url)
VALUES
(1, '2BHK Apartment in Pune', 'Spacious 2BHK flat near Hinjewadi', 'Hinjewadi Phase 1', 'Pune', 18000.00, 'Apartment', 'property1.jpg'),
(2, '1BHK Studio in Mumbai', 'Cozy studio flat near Andheri', 'Andheri West', 'Mumbai', 25000.00, 'Studio', 'property2.jpg');

-- Bookings
INSERT INTO bookings (tenant_id, property_id, start_date, end_date, total_amount, status)
VALUES
(1, 1, '2025-01-01', '2025-06-30', 108000.00, 'Approved'),
(2, 2, '2025-02-15', '2025-08-15', 150000.00, 'Pending');

-- Payments
INSERT INTO payments (booking_id, amount, payment_method, status)
VALUES
(1, 18000.00, 'UPI', 'Paid'),
(2, 25000.00, 'Credit Card', 'Pending');

-- Reviews
INSERT INTO reviews (tenant_id, property_id, rating, comment)
VALUES
(1, 1, 5, 'Excellent property, clean and well maintained!'),
(2, 2, 4, 'Good location but slightly overpriced.');

-- Favorites
INSERT INTO favorites (tenant_id, property_id)
VALUES
(1, 2),
(2, 1);

-- Maintenance Requests
INSERT INTO maintenance_requests (property_id, tenant_id, issue_description, status)
VALUES
(1, 1, 'Water leakage in bathroom', 'In Progress'),
(2, 2, 'Air conditioner not working', 'Pending');

-- Notifications
INSERT INTO notifications (user_type, user_id, message)
VALUES
('Tenant', 1, 'Your booking has been approved!'),
('Owner', 2, 'New maintenance request received.');

-- Admin
INSERT INTO admin (username, email, password)
VALUES
('admin', 'admin@renthub.com', 'admin123');

-- ==========================================
-- END OF rental_db
-- ==========================================
