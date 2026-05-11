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

CREATE TABLE IF NOT EXISTS items (
    id VARCHAR(64) PRIMARY KEY,
    seller_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    starting_price BIGINT NOT NULL,
    current_price BIGINT NOT NULL,
    bid_step BIGINT NOT NULL DEFAULT 500000,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    brand VARCHAR(255),
    warranty_months INT,
    manufacturer VARCHAR(255),
    production_year INT,
    mileage INT,
    artist VARCHAR(255),
    year_created INT,
    image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_items_seller FOREIGN KEY (seller_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS auctions (
    id VARCHAR(64) PRIMARY KEY,
    item_id VARCHAR(64) NOT NULL UNIQUE,
    seller_id VARCHAR(64) NOT NULL,
    highest_bidder_id VARCHAR(64),
    starting_price BIGINT NOT NULL,
    current_price BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    finished BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_auctions_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT fk_auctions_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    CONSTRAINT fk_auctions_highest_bidder FOREIGN KEY (highest_bidder_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS bid_transactions (
    id VARCHAR(64) PRIMARY KEY,
    auction_id VARCHAR(64) NOT NULL,
    bidder_id VARCHAR(64) NOT NULL,
    bid_amount BIGINT NOT NULL,
    bid_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bid_transactions_auction FOREIGN KEY (auction_id) REFERENCES auctions(id),
    CONSTRAINT fk_bid_transactions_bidder FOREIGN KEY (bidder_id) REFERENCES users(id)
);
