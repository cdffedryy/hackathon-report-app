-- Database schema creation script
-- Customer table
CREATE TABLE customer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    type VARCHAR(20),
    status VARCHAR(20),
    email VARCHAR(100),
    phone VARCHAR(20),
    address VARCHAR(200),
    registration_date DATE,
    credit_score INT,
    account_balance DECIMAL(18,2) DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Transaction table
CREATE TABLE transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT,
    amount DECIMAL(18,2),
    type VARCHAR(20),
    status VARCHAR(20),
    category VARCHAR(50),
    description TEXT,
    transaction_date DATE,
    reference_number VARCHAR(50),
    merchant_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id)
);

-- Merchant table
CREATE TABLE merchant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    category VARCHAR(50),
    status VARCHAR(20),
    commission_rate DECIMAL(5,4),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Product table
CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    category VARCHAR(50),
    price DECIMAL(18,2),
    cost DECIMAL(18,2),
    stock_quantity INT,
    supplier_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order table
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT,
    order_date DATE,
    total_amount DECIMAL(18,2),
    status VARCHAR(20),
    shipping_address TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer(id)
);

-- Order items table
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT,
    product_id BIGINT,
    quantity INT,
    unit_price DECIMAL(18,2),
    total_price DECIMAL(18,2),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
);

-- Department table
CREATE TABLE department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    manager VARCHAR(100),
    budget DECIMAL(18,2),
    location VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Employee table
CREATE TABLE employee (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    email VARCHAR(100),
    department_id BIGINT,
    position VARCHAR(50),
    salary DECIMAL(10,2),
    hire_date DATE,
    status VARCHAR(20),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES department(id)
);

-- Report configuration table
CREATE TABLE report_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    sql TEXT,
    description VARCHAR(500),
    is_deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Manual edit metadata for report_run table (guarded for existing deployments)
ALTER TABLE IF EXISTS report_run ADD COLUMN IF NOT EXISTS manual_note TEXT;
ALTER TABLE IF EXISTS report_run ADD COLUMN IF NOT EXISTS manual_edited_at TIMESTAMP;
ALTER TABLE IF EXISTS report_run ADD COLUMN IF NOT EXISTS has_manual_edits BOOLEAN DEFAULT FALSE;
