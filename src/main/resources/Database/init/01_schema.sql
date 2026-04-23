CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    balance BIGINT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO users (id, username, full_name, email, password, role, balance, active)
VALUES
    ('admin-001', 'admin', 'Administrator', 'admin@auction.com', '1216985755', 'ADMIN', 100000000, TRUE),
    ('seller-001', 'seller1', 'Seller One', 'seller1@example.com', '1216985755', 'SELLER', 5000000, TRUE),
    ('bidder-001', 'bidder1', 'Bidder One', 'bidder1@example.com', '1216985755', 'BIDDER', 2000000, TRUE)
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    email = VALUES(email),
    password = VALUES(password),
    role = VALUES(role),
    balance = VALUES(balance),
    active = VALUES(active);
