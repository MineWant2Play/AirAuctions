CREATE TABLE {prefix}listings (
    id TEXT PRIMARY KEY,
    listing_type TEXT NOT NULL,
    status TEXT NOT NULL,
    seller TEXT NOT NULL,
    item BLOB NOT NULL,
    amount INTEGER NOT NULL,
    economy TEXT NOT NULL,
    fee REAL NOT NULL,
    tax REAL NOT NULL,
    tax_rate REAL NOT NULL DEFAULT -1,
    category TEXT NOT NULL,
    search_name TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    ended_at INTEGER,
    price REAL NOT NULL,
    remaining_amount INTEGER,
    current_price REAL,
    current_bidder TEXT,
    total_bidders INTEGER NOT NULL DEFAULT 0,
    reminders_shown INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_{prefix}listings_seller_status ON {prefix}listings (seller, status);
CREATE INDEX idx_{prefix}listings_bidder_status ON {prefix}listings (current_bidder, status);
CREATE INDEX idx_{prefix}listings_status_type ON {prefix}listings (status, listing_type);
CREATE INDEX idx_{prefix}listings_status_expires ON {prefix}listings (status, expires_at);
CREATE INDEX idx_{prefix}listings_category ON {prefix}listings (category);
CREATE INDEX idx_{prefix}listings_economy ON {prefix}listings (economy);
CREATE INDEX idx_{prefix}listings_search_name ON {prefix}listings (search_name);

CREATE TABLE {prefix}bid_entries (
    listing_id TEXT NOT NULL REFERENCES {prefix}listings (id) ON DELETE CASCADE,
    bidder TEXT NOT NULL,
    offer REAL NOT NULL,
    total_offers INTEGER NOT NULL,
    PRIMARY KEY (listing_id, bidder)
);

CREATE INDEX idx_{prefix}bid_entries_listing_offer ON {prefix}bid_entries (listing_id, offer DESC);

CREATE TABLE {prefix}history (
    entry_id INTEGER PRIMARY KEY AUTOINCREMENT,
    listing_id TEXT NOT NULL,
    listing_type TEXT NOT NULL,
    seller TEXT NOT NULL,
    buyer TEXT NOT NULL,
    item BLOB NOT NULL,
    amount INTEGER NOT NULL,
    economy TEXT NOT NULL,
    fee REAL NOT NULL,
    tax REAL NOT NULL,
    category TEXT NOT NULL,
    search_name TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    completed_at INTEGER NOT NULL,
    price REAL,
    starting_price REAL,
    final_price REAL,
    total_bidders INTEGER
);

CREATE INDEX idx_{prefix}history_seller_completed ON {prefix}history (seller, completed_at);
CREATE INDEX idx_{prefix}history_buyer_completed ON {prefix}history (buyer, completed_at);
CREATE INDEX idx_{prefix}history_category ON {prefix}history (category);
CREATE INDEX idx_{prefix}history_economy ON {prefix}history (economy);
CREATE INDEX idx_{prefix}history_search_name ON {prefix}history (search_name);

CREATE TABLE {prefix}players (
    uuid TEXT PRIMARY KEY,
    name TEXT NOT NULL COLLATE NOCASE,
    skin TEXT NOT NULL,
    skin_signature TEXT NOT NULL,
    extra_slots INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_{prefix}players_name ON {prefix}players (name);

CREATE TABLE {prefix}player_earnings (
    uuid TEXT NOT NULL REFERENCES {prefix}players (uuid) ON DELETE CASCADE,
    economy TEXT NOT NULL,
    amount REAL NOT NULL,
    PRIMARY KEY (uuid, economy)
);

CREATE TABLE {prefix}player_refunds (
    uuid TEXT NOT NULL REFERENCES {prefix}players (uuid) ON DELETE CASCADE,
    economy TEXT NOT NULL,
    amount REAL NOT NULL,
    PRIMARY KEY (uuid, economy)
);