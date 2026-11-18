-- One manager and one front-desk login per demo property.
-- Booka Grand (hotel 1) already has admin@booka.com and staff@booka.com.
-- Explicit ids: the users sequence can sit at 1 after the original seed inserts.
-- Password hash matches the seeded demo password; startup re-encodes "password".

INSERT INTO users (id, email, password_hash, full_name, role, hotel_id, created_at, updated_at)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM users), 'harbour.admin@booka.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Harbourview Manager', 'ADMIN', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'harbour.admin@booka.com');

INSERT INTO users (id, email, password_hash, full_name, role, hotel_id, created_at, updated_at)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM users), 'harbour.staff@booka.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Harbourview Front Desk', 'STAFF', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'harbour.staff@booka.com');

INSERT INTO users (id, email, password_hash, full_name, role, hotel_id, created_at, updated_at)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM users), 'inle.admin@booka.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Inle Lodge Manager', 'ADMIN', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'inle.admin@booka.com');

INSERT INTO users (id, email, password_hash, full_name, role, hotel_id, created_at, updated_at)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM users), 'inle.staff@booka.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Inle Front Desk', 'STAFF', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'inle.staff@booka.com');

INSERT INTO users (id, email, password_hash, full_name, role, hotel_id, created_at, updated_at)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM users), 'bagan.admin@booka.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Bagan Inn Manager', 'ADMIN', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'bagan.admin@booka.com');

INSERT INTO users (id, email, password_hash, full_name, role, hotel_id, created_at, updated_at)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM users), 'bagan.staff@booka.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Bagan Front Desk', 'STAFF', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'bagan.staff@booka.com');

INSERT INTO users (id, email, password_hash, full_name, role, hotel_id, created_at, updated_at)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM users), 'mandalay.admin@booka.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Mandalay House Manager', 'ADMIN', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'mandalay.admin@booka.com');

INSERT INTO users (id, email, password_hash, full_name, role, hotel_id, created_at, updated_at)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM users), 'mandalay.staff@booka.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Mandalay Front Desk', 'STAFF', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'mandalay.staff@booka.com');
