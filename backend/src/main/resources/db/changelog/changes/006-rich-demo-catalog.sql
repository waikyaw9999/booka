-- Rich demo catalog: extra hotels, inventory, imagery, guests, bookings, folios, RAG.
-- Idempotent via WHERE NOT EXISTS. Portable across PostgreSQL and H2 (PostgreSQL mode).

INSERT INTO hotels (id, name, description, address, city, country, amenities, cancellation_policy, created_at, updated_at)
SELECT 2, 'Harbourview Suites', 'Waterfront suites with skyline views, quiet mornings, and a rooftop pool above the strand.', '88 Strand Road', 'Yangon', 'Myanmar', 'WiFi, Pool, Breakfast, River View, Concierge', 'Free cancellation up to 24 hours before check-in.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE id = 2);

INSERT INTO hotels (id, name, description, address, city, country, amenities, cancellation_policy, created_at, updated_at)
SELECT 3, 'Inle Lake Lodge', 'Stilted lodge rooms overlooking the lake, with canoe transfers and garden dining at dusk.', '12 Lakeside Path', 'Nyaungshwe', 'Myanmar', 'WiFi, Restaurant, Garden, Bike Rental, Lake View', 'Free cancellation up to 72 hours before check-in. Later cancellations charge one night.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE id = 3);

INSERT INTO hotels (id, name, description, address, city, country, amenities, cancellation_policy, created_at, updated_at)
SELECT 4, 'Bagan Sandstone Inn', 'Boutique rooms near the temple plains, sunrise balconies, and a quiet courtyard cafe.', '5 Anawrahta Road', 'Bagan', 'Myanmar', 'WiFi, Courtyard, Temple Tours Desk, Cafe, Parking', 'Non-refundable within 7 days of arrival; otherwise free cancellation.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE id = 4);

INSERT INTO hotels (id, name, description, address, city, country, amenities, cancellation_policy, created_at, updated_at)
SELECT 5, 'Mandalay Heritage House', 'Colonial-era townhouse with teak suites, afternoon tea, and walks to the palace walls.', '22 84th Street', 'Mandalay', 'Myanmar', 'WiFi, Tea Lounge, Library, Courtyard, Airport Shuttle', 'Free cancellation up to 48 hours before check-in.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE id = 5);

INSERT INTO room_types (id, hotel_id, name, description, max_occupancy, base_rate, amenities, created_at, updated_at)
SELECT 3, 2, 'Harbour Twin', 'Twin beds with harbour balcony and morning coffee.', 2, 119.00, 'WiFi, Balcony, Mini Bar', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_types WHERE id = 3);

INSERT INTO room_types (id, hotel_id, name, description, max_occupancy, base_rate, amenities, created_at, updated_at)
SELECT 4, 2, 'Skyline Suite', 'Corner suite with lounge seating and city light.', 3, 189.00, 'WiFi, Lounge, Bathtub, City View', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_types WHERE id = 4);

INSERT INTO room_types (id, hotel_id, name, description, max_occupancy, base_rate, amenities, created_at, updated_at)
SELECT 5, 3, 'Lake Cabin', 'Timber cabin facing the water — ideal for two.', 2, 79.00, 'WiFi, Fan, Lake View', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_types WHERE id = 5);

INSERT INTO room_types (id, hotel_id, name, description, max_occupancy, base_rate, amenities, created_at, updated_at)
SELECT 6, 3, 'Garden Pavilion', 'Larger pavilion with private deck for four.', 4, 129.00, 'WiFi, Deck, Daybed', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_types WHERE id = 6);

INSERT INTO room_types (id, hotel_id, name, description, max_occupancy, base_rate, amenities, created_at, updated_at)
SELECT 7, 4, 'Sandstone Double', 'Quiet double with courtyard light.', 2, 69.00, 'WiFi, AC, Courtyard View', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_types WHERE id = 7);

INSERT INTO room_types (id, hotel_id, name, description, max_occupancy, base_rate, amenities, created_at, updated_at)
SELECT 8, 4, 'Sunrise Balcony', 'East-facing balcony for temple dawns.', 2, 99.00, 'WiFi, Balcony, Desk', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_types WHERE id = 8);

INSERT INTO room_types (id, hotel_id, name, description, max_occupancy, base_rate, amenities, created_at, updated_at)
SELECT 9, 5, 'Teak Classic', 'Heritage double with writing desk.', 2, 95.00, 'WiFi, Desk, Heritage Furnishings', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_types WHERE id = 9);

INSERT INTO room_types (id, hotel_id, name, description, max_occupancy, base_rate, amenities, created_at, updated_at)
SELECT 10, 5, 'Garden Suite', 'Suite opening onto the courtyard.', 3, 155.00, 'WiFi, Sitting Area, Courtyard Access', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_types WHERE id = 10);

INSERT INTO rooms (id, room_type_id, room_number, status, housekeeping_status, floor, created_at, updated_at)
SELECT 4, 3, '301', 'AVAILABLE', 'CLEAN', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 4);
INSERT INTO rooms (id, room_type_id, room_number, status, housekeeping_status, floor, created_at, updated_at)
SELECT 5, 3, '302', 'AVAILABLE', 'CLEAN', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 5);
INSERT INTO rooms (id, room_type_id, room_number, status, housekeeping_status, floor, created_at, updated_at)
SELECT 6, 4, '401', 'AVAILABLE', 'CLEAN', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 6);
INSERT INTO rooms (id, room_type_id, room_number, status, housekeeping_status, floor, created_at, updated_at)
SELECT 7, 5, 'L1', 'AVAILABLE', 'CLEAN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 7);
INSERT INTO rooms (id, room_type_id, room_number, status, housekeeping_status, floor, created_at, updated_at)
SELECT 8, 5, 'L2', 'AVAILABLE', 'DIRTY', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 8);
INSERT INTO rooms (id, room_type_id, room_number, status, housekeeping_status, floor, created_at, updated_at)
SELECT 9, 6, 'G1', 'AVAILABLE', 'CLEAN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 9);
INSERT INTO rooms (id, room_type_id, room_number, status, housekeeping_status, floor, created_at, updated_at)
SELECT 10, 7, '11', 'AVAILABLE', 'CLEAN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 10);
INSERT INTO rooms (id, room_type_id, room_number, status, housekeeping_status, floor, created_at, updated_at)
SELECT 11, 7, '12', 'AVAILABLE', 'INSPECTING', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 11);
INSERT INTO rooms (id, room_type_id, room_number, status, housekeeping_status, floor, created_at, updated_at)
SELECT 12, 8, '21', 'AVAILABLE', 'CLEAN', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 12);
INSERT INTO rooms (id, room_type_id, room_number, status, housekeeping_status, floor, created_at, updated_at)
SELECT 13, 9, 'H1', 'AVAILABLE', 'CLEAN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 13);
INSERT INTO rooms (id, room_type_id, room_number, status, housekeeping_status, floor, created_at, updated_at)
SELECT 14, 9, 'H2', 'AVAILABLE', 'CLEAN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 14);
INSERT INTO rooms (id, room_type_id, room_number, status, housekeeping_status, floor, created_at, updated_at)
SELECT 15, 10, 'HS1', 'AVAILABLE', 'CLEAN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 15);
INSERT INTO rooms (id, room_type_id, room_number, status, housekeeping_status, floor, created_at, updated_at)
SELECT 16, 1, '103', 'AVAILABLE', 'CLEAN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 16);
INSERT INTO rooms (id, room_type_id, room_number, status, housekeeping_status, floor, created_at, updated_at)
SELECT 17, 1, '104', 'AVAILABLE', 'DIRTY', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 17);
INSERT INTO rooms (id, room_type_id, room_number, status, housekeeping_status, floor, created_at, updated_at)
SELECT 18, 2, '202', 'AVAILABLE', 'CLEAN', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 18);
INSERT INTO rooms (id, room_type_id, room_number, status, housekeeping_status, floor, created_at, updated_at)
SELECT 19, 2, '203', 'AVAILABLE', 'CLEAN', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE id = 19);

-- Hotel gallery images (hotels 2–5)
INSERT INTO hotel_images (hotel_id, url, caption, sort_order, is_primary, created_at, updated_at)
SELECT 2, 'https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=1600&q=80', 'Harbour facade', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM hotel_images WHERE hotel_id = 2);
INSERT INTO hotel_images (hotel_id, url, caption, sort_order, is_primary, created_at, updated_at)
SELECT 2, 'https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=1600&q=80', 'Rooftop pool', 1, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE (SELECT COUNT(*) FROM hotel_images WHERE hotel_id = 2) < 2;

INSERT INTO hotel_images (hotel_id, url, caption, sort_order, is_primary, created_at, updated_at)
SELECT 3, 'https://images.unsplash.com/photo-1439066615861-d1af74d74000?auto=format&fit=crop&w=1600&q=80', 'Lake view', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM hotel_images WHERE hotel_id = 3);
INSERT INTO hotel_images (hotel_id, url, caption, sort_order, is_primary, created_at, updated_at)
SELECT 3, 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1600&q=80', 'Water edge', 1, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE (SELECT COUNT(*) FROM hotel_images WHERE hotel_id = 3) < 2;

INSERT INTO hotel_images (hotel_id, url, caption, sort_order, is_primary, created_at, updated_at)
SELECT 4, 'https://images.unsplash.com/photo-1548013146-72479768bada?auto=format&fit=crop&w=1600&q=80', 'Temple plains', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM hotel_images WHERE hotel_id = 4);
INSERT INTO hotel_images (hotel_id, url, caption, sort_order, is_primary, created_at, updated_at)
SELECT 4, 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1600&q=80', 'Courtyard stay', 1, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE (SELECT COUNT(*) FROM hotel_images WHERE hotel_id = 4) < 2;

INSERT INTO hotel_images (hotel_id, url, caption, sort_order, is_primary, created_at, updated_at)
SELECT 5, 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=1600&q=80', 'Heritage facade', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM hotel_images WHERE hotel_id = 5);
INSERT INTO hotel_images (hotel_id, url, caption, sort_order, is_primary, created_at, updated_at)
SELECT 5, 'https://images.unsplash.com/photo-1564501049412-61c2a3083791?auto=format&fit=crop&w=1600&q=80', 'Tea lounge', 1, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE (SELECT COUNT(*) FROM hotel_images WHERE hotel_id = 5) < 2;

-- Room images for type thumbs across properties
INSERT INTO room_images (room_id, url, caption, sort_order, is_primary, created_at, updated_at)
SELECT 4, 'https://images.unsplash.com/photo-1591088398332-8a7791972843?auto=format&fit=crop&w=1200&q=80', 'Harbour Twin', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_images WHERE room_id = 4);
INSERT INTO room_images (room_id, url, caption, sort_order, is_primary, created_at, updated_at)
SELECT 6, 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 'Skyline Suite', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_images WHERE room_id = 6);
INSERT INTO room_images (room_id, url, caption, sort_order, is_primary, created_at, updated_at)
SELECT 7, 'https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?auto=format&fit=crop&w=1200&q=80', 'Lake Cabin', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_images WHERE room_id = 7);
INSERT INTO room_images (room_id, url, caption, sort_order, is_primary, created_at, updated_at)
SELECT 9, 'https://images.unsplash.com/photo-1595576508898-0ad5c879a061?auto=format&fit=crop&w=1200&q=80', 'Garden Pavilion', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_images WHERE room_id = 9);
INSERT INTO room_images (room_id, url, caption, sort_order, is_primary, created_at, updated_at)
SELECT 10, 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658?auto=format&fit=crop&w=1200&q=80', 'Sandstone Double', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_images WHERE room_id = 10);
INSERT INTO room_images (room_id, url, caption, sort_order, is_primary, created_at, updated_at)
SELECT 12, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80', 'Sunrise Balcony', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_images WHERE room_id = 12);
INSERT INTO room_images (room_id, url, caption, sort_order, is_primary, created_at, updated_at)
SELECT 13, 'https://images.unsplash.com/photo-1616594039964-ae9021a400a0?auto=format&fit=crop&w=1200&q=80', 'Teak Classic', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_images WHERE room_id = 13);
INSERT INTO room_images (room_id, url, caption, sort_order, is_primary, created_at, updated_at)
SELECT 15, 'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=1200&q=80', 'Garden Suite', 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_images WHERE room_id = 15);

INSERT INTO blackout_dates (id, hotel_id, room_type_id, start_date, end_date, reason, created_at, updated_at)
SELECT 3, 2, NULL, '2026-12-10', '2026-12-13', 'Waterfront festival — lobby events only', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM blackout_dates WHERE id = 3);
INSERT INTO blackout_dates (id, hotel_id, room_type_id, start_date, end_date, reason, created_at, updated_at)
SELECT 4, 3, 6, '2027-01-05', '2027-01-12', 'Garden Pavilion deck maintenance', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM blackout_dates WHERE id = 4);
INSERT INTO blackout_dates (id, hotel_id, room_type_id, start_date, end_date, reason, created_at, updated_at)
SELECT 5, 4, NULL, '2026-12-20', '2026-12-22', 'Full property private wedding buyout', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM blackout_dates WHERE id = 5);
INSERT INTO blackout_dates (id, hotel_id, room_type_id, start_date, end_date, reason, created_at, updated_at)
SELECT 6, 5, 10, '2027-02-01', '2027-02-06', 'Garden Suite restoration', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM blackout_dates WHERE id = 6);

INSERT INTO guests (id, email, full_name, phone, created_at, updated_at)
SELECT 1, 'aye.chan@example.com', 'Aye Chan', '+9591110001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM guests WHERE id = 1);
INSERT INTO guests (id, email, full_name, phone, created_at, updated_at)
SELECT 2, 'min.htet@example.com', 'Min Htet', '+9591110002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM guests WHERE id = 2);
INSERT INTO guests (id, email, full_name, phone, created_at, updated_at)
SELECT 3, 'su.myat@example.com', 'Su Myat', '+9591110003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM guests WHERE id = 3);
INSERT INTO guests (id, email, full_name, phone, created_at, updated_at)
SELECT 4, 'ko.ko@example.com', 'Ko Ko', '+9591110004', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM guests WHERE id = 4);
INSERT INTO guests (id, email, full_name, phone, created_at, updated_at)
SELECT 5, 'thin.zar@example.com', 'Thin Zar', '+9591110005', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM guests WHERE id = 5);
INSERT INTO guests (id, email, full_name, phone, created_at, updated_at)
SELECT 6, 'nwe.nwe@example.com', 'Nwe Nwe', '+9591110006', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM guests WHERE id = 6);
INSERT INTO guests (id, email, full_name, phone, created_at, updated_at)
SELECT 7, 'zaw.min@example.com', 'Zaw Min', '+9591110007', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM guests WHERE id = 7);

-- Sample bookings across properties (confirmation codes unique)
INSERT INTO bookings (id, hotel_id, room_id, room_type_id, guest_id, check_in_date, check_out_date, num_guests, total_price, status, confirmation_code, channel, payment_status, created_at, updated_at)
SELECT 101, 1, 1, 1, 1, '2026-11-20', '2026-11-22', 2, 195.80, 'CONFIRMED', 'BK-DEMO-101', 'DIRECT', 'N_A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM bookings WHERE id = 101 OR confirmation_code = 'BK-DEMO-101');

INSERT INTO bookings (id, hotel_id, room_id, room_type_id, guest_id, check_in_date, check_out_date, num_guests, total_price, status, confirmation_code, channel, payment_status, created_at, updated_at)
SELECT 102, 1, 3, 2, 2, '2026-12-01', '2026-12-03', 2, 327.80, 'CONFIRMED', 'BK-DEMO-102', 'DIRECT', 'N_A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM bookings WHERE id = 102 OR confirmation_code = 'BK-DEMO-102');

INSERT INTO bookings (id, hotel_id, room_id, room_type_id, guest_id, check_in_date, check_out_date, num_guests, total_price, status, confirmation_code, channel, payment_status, created_at, updated_at)
SELECT 103, 2, 4, 3, 3, '2026-11-18', '2026-11-20', 2, 261.80, 'CONFIRMED', 'BK-DEMO-103', 'OTA', 'N_A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM bookings WHERE id = 103 OR confirmation_code = 'BK-DEMO-103');

INSERT INTO bookings (id, hotel_id, room_id, room_type_id, guest_id, check_in_date, check_out_date, num_guests, total_price, status, confirmation_code, channel, payment_status, created_at, updated_at)
SELECT 104, 2, NULL, 4, 4, '2026-12-05', '2026-12-08', 2, 623.70, 'CONFIRMED', 'BK-DEMO-104', 'DIRECT', 'N_A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM bookings WHERE id = 104 OR confirmation_code = 'BK-DEMO-104');

INSERT INTO bookings (id, hotel_id, room_id, room_type_id, guest_id, check_in_date, check_out_date, num_guests, total_price, status, confirmation_code, channel, payment_status, created_at, updated_at)
SELECT 105, 3, 7, 5, 5, '2026-11-25', '2026-11-27', 2, 173.80, 'CONFIRMED', 'BK-DEMO-105', 'WALK_IN', 'N_A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM bookings WHERE id = 105 OR confirmation_code = 'BK-DEMO-105');

INSERT INTO bookings (id, hotel_id, room_id, room_type_id, guest_id, check_in_date, check_out_date, num_guests, total_price, status, confirmation_code, channel, payment_status, created_at, updated_at)
SELECT 106, 4, 10, 7, 6, '2026-09-22', '2026-09-24', 2, 151.80, 'CANCELLED', 'BK-DEMO-106', 'DIRECT', 'N_A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM bookings WHERE id = 106 OR confirmation_code = 'BK-DEMO-106');

INSERT INTO bookings (id, hotel_id, room_id, room_type_id, guest_id, check_in_date, check_out_date, num_guests, total_price, status, confirmation_code, channel, payment_status, created_at, updated_at)
SELECT 107, 5, 13, 9, 7, '2026-12-12', '2026-12-14', 2, 209.00, 'CONFIRMED', 'BK-DEMO-107', 'DIRECT', 'N_A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM bookings WHERE id = 107 OR confirmation_code = 'BK-DEMO-107');

INSERT INTO bookings (id, hotel_id, room_id, room_type_id, guest_id, check_in_date, check_out_date, num_guests, total_price, status, confirmation_code, channel, payment_status, created_at, updated_at)
SELECT 108, 1, 2, 1, 3, '2026-09-08', '2026-09-10', 2, 195.80, 'CHECKED_OUT', 'BK-DEMO-108', 'DIRECT', 'N_A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM bookings WHERE id = 108 OR confirmation_code = 'BK-DEMO-108');

INSERT INTO bookings (id, hotel_id, room_id, room_type_id, guest_id, check_in_date, check_out_date, num_guests, total_price, status, confirmation_code, channel, payment_status, created_at, updated_at)
SELECT 109, 3, 9, 6, 1, '2026-09-15', '2026-09-18', 3, 425.70, 'CONFIRMED', 'BK-DEMO-109', 'CHANNEL_MANAGER', 'N_A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM bookings WHERE id = 109 OR confirmation_code = 'BK-DEMO-109');

INSERT INTO bookings (id, hotel_id, room_id, room_type_id, guest_id, check_in_date, check_out_date, num_guests, total_price, status, confirmation_code, channel, payment_status, created_at, updated_at)
SELECT 110, 5, NULL, 10, 2, '2026-11-28', '2026-11-30', 2, 341.00, 'CONFIRMED', 'BK-DEMO-110', 'DIRECT', 'N_A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM bookings WHERE id = 110 OR confirmation_code = 'BK-DEMO-110');

INSERT INTO folios (booking_id, hotel_id, status, created_at, updated_at)
SELECT b.id, b.hotel_id, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM bookings b
WHERE b.confirmation_code LIKE 'BK-DEMO-%'
  AND NOT EXISTS (SELECT 1 FROM folios f WHERE f.booking_id = b.id);

INSERT INTO rag_documents (id, title, audience, hotel_id, source_type, created_at, updated_at)
SELECT 3, 'Guest FAQ - Harbourview Suites', 'PUBLIC', 2, 'FAQ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rag_documents WHERE id = 3);
INSERT INTO rag_documents (id, title, audience, hotel_id, source_type, created_at, updated_at)
SELECT 4, 'Guest FAQ - Inle Lake Lodge', 'PUBLIC', 3, 'FAQ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rag_documents WHERE id = 4);
INSERT INTO rag_documents (id, title, audience, hotel_id, source_type, created_at, updated_at)
SELECT 5, 'Guest FAQ - Bagan Sandstone Inn', 'PUBLIC', 4, 'FAQ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rag_documents WHERE id = 5);
INSERT INTO rag_documents (id, title, audience, hotel_id, source_type, created_at, updated_at)
SELECT 6, 'Guest FAQ - Mandalay Heritage House', 'PUBLIC', 5, 'FAQ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rag_documents WHERE id = 6);
INSERT INTO rag_documents (id, title, audience, hotel_id, source_type, created_at, updated_at)
SELECT 7, 'Staff SOP - Multi-property Notes', 'STAFF', NULL, 'SOP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP WHERE NOT EXISTS (SELECT 1 FROM rag_documents WHERE id = 7);

INSERT INTO rag_chunks (id, document_id, content, section, audience, hotel_id, created_at)
SELECT 6, 3, 'Harbourview Suites check-in is from 3:00 PM. Rooftop pool hours are 6:00 AM to 9:00 PM. River-view rooms include complimentary morning coffee.', 'Check-in & Amenities', 'PUBLIC', 2, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM rag_chunks WHERE id = 6);
INSERT INTO rag_chunks (id, document_id, content, section, audience, hotel_id, created_at)
SELECT 7, 3, 'Free cancellation up to 24 hours before check-in. Airport transfers can be arranged at the concierge desk for a fixed fee.', 'Cancellation & Transfers', 'PUBLIC', 2, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM rag_chunks WHERE id = 7);
INSERT INTO rag_chunks (id, document_id, content, section, audience, hotel_id, created_at)
SELECT 8, 4, 'Inle Lake Lodge offers complimentary canoe transfer from the jetty. Dinner is served in the garden from 6:00 PM to 9:00 PM.', 'Transfers & Dining', 'PUBLIC', 3, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM rag_chunks WHERE id = 8);
INSERT INTO rag_chunks (id, document_id, content, section, audience, hotel_id, created_at)
SELECT 9, 4, 'Bike rentals are complimentary for guests. Lake Cabin rooms sleep two; Garden Pavilions sleep up to four.', 'Activities & Rooms', 'PUBLIC', 3, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM rag_chunks WHERE id = 9);
INSERT INTO rag_chunks (id, document_id, content, section, audience, hotel_id, created_at)
SELECT 10, 5, 'Bagan Sandstone Inn can arrange private temple sunrise tours. Quiet hours begin at 10:00 PM in the courtyard.', 'Tours & Quiet Hours', 'PUBLIC', 4, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM rag_chunks WHERE id = 10);
INSERT INTO rag_chunks (id, document_id, content, section, audience, hotel_id, created_at)
SELECT 11, 5, 'Sunrise Balcony rooms face east toward the plains. Sandstone Double rooms overlook the inner courtyard.', 'Room Orientation', 'PUBLIC', 4, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM rag_chunks WHERE id = 11);
INSERT INTO rag_chunks (id, document_id, content, section, audience, hotel_id, created_at)
SELECT 12, 6, 'Afternoon tea is served daily from 3:00 PM to 5:00 PM in the lounge. Airport shuttle departs on the hour from 8:00 AM to 6:00 PM.', 'Tea & Shuttle', 'PUBLIC', 5, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM rag_chunks WHERE id = 12);
INSERT INTO rag_chunks (id, document_id, content, section, audience, hotel_id, created_at)
SELECT 13, 6, 'Heritage House rooms keep original teak floors. Please use provided slippers indoors.', 'House Notes', 'PUBLIC', 5, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM rag_chunks WHERE id = 13);
INSERT INTO rag_chunks (id, document_id, content, section, audience, hotel_id, created_at)
SELECT 14, 7, 'When guests ask about multi-city itineraries, recommend Harbourview for Yangon arrival nights, Inle for mid-trip rest, Bagan for temples, and Heritage House for Mandalay.', 'Itinerary Tips', 'STAFF', NULL, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM rag_chunks WHERE id = 14);
