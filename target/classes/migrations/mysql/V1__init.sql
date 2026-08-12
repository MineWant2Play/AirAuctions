CREATE TABLE {prefix}listings (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    listing_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    seller VARCHAR(36) NOT NULL,
    item MEDIUMBLOB NOT NULL,
    amount INT NOT NULL,
    economy VARCHAR(64) NOT NULL,
    fee DOUBLE NOT NULL,
    tax DOUBLE NOT NULL,
    tax_rate DOUBLE NOT NULL DEFAULT -1,
    category VARCHAR(64) NOT NULL,
    search_name VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    ended_at BIGINT,
    price DOUBLE NOT NULL,
    remaining_amount INT,
    current_price DOUBLE,
    current_bidder VARCHAR(36),
    total_bidders INT NOT NULL DEFAULT 0,
    reminders_shown INT NOT NULL DEFAULT 0,
    INDEX idx_listings_seller_status (seller, status),
    INDEX idx_listings_bidder_status (current_bidder, status),
    INDEX idx_listings_status_type (status, listing_type),
    INDEX idx_listings_status_expires (status, expires_at),
    INDEX idx_listings_category (category),
    INDEX idx_listings_economy (economy),
    INDEX idx_listings_search_name (search_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE {prefix}bid_entries (
    listing_id VARCHAR(36) NOT NULL,
    bidder VARCHAR(36) NOT NULL,
    offer DOUBLE NOT NULL,
    total_offers INT NOT NULL,
    PRIMARY KEY (listing_id, bidder),
    INDEX idx_bid_entries_listing_offer (listing_id, offer DESC),
    FOREIGN KEY (listing_id) REFERENCES {prefix}listings (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE {prefix}history (
    entry_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    listing_id VARCHAR(36) NOT NULL,
    listing_type VARCHAR(16) NOT NULL,
    seller VARCHAR(36) NOT NULL,
    buyer VARCHAR(36) NOT NULL,
    item MEDIUMBLOB NOT NULL,
    amount INT NOT NULL,
    economy VARCHAR(64) NOT NULL,
    fee DOUBLE NOT NULL,
    tax DOUBLE NOT NULL,
    category VARCHAR(64) NOT NULL,
    search_name VARCHAR(255) NOT NULL,
    created_at BIGINT NOT NULL,
    completed_at BIGINT NOT NULL,
    price DOUBLE,
    starting_price DOUBLE,
    final_price DOUBLE,
    total_bidders INT,
    INDEX idx_history_seller_completed (seller, completed_at),
    INDEX idx_history_buyer_completed (buyer, completed_at),
    INDEX idx_history_category (category),
    INDEX idx_history_economy (economy),
    INDEX idx_history_search_name (search_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE {prefix}players (
    uuid VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(16) NOT NULL COLLATE utf8mb4_general_ci,
    skin TEXT NOT NULL,
    skin_signature TEXT NOT NULL,
    extra_slots INT NOT NULL DEFAULT 0,
    UNIQUE INDEX idx_players_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE {prefix}player_earnings (
    uuid VARCHAR(36) NOT NULL,
    economy VARCHAR(64) NOT NULL,
    amount DOUBLE NOT NULL,
    PRIMARY KEY (uuid, economy),
    FOREIGN KEY (uuid) REFERENCES {prefix}players (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE {prefix}player_refunds (
    uuid VARCHAR(36) NOT NULL,
    economy VARCHAR(64) NOT NULL,
    amount DOUBLE NOT NULL,
    PRIMARY KEY (uuid, economy),
    FOREIGN KEY (uuid) REFERENCES {prefix}players (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;