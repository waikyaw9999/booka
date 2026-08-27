-- Extra demo properties for Booka (idempotent-ish: skip if already present)
BEGIN;

INSERT INTO hotels (id, name, description, address, city, country, amenities, cancellation_policy, created_at, updated_at)
VALUES
  (2, 'Harbourview Suites', 'Waterfront suites with skyline views, quiet mornings, and a rooftop pool.', '88 Strand Road', 'Yangon', 'Myanmar', 'WiFi, Pool, Breakfast, River View, Concierge', 'Free cancellation up to 24 hours before check-in.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 'Inle Lake Lodge', 'Stilted lodge rooms overlooking the lake, with canoe transfers and garden dining.', '12 Lakeside Path', 'Nyaungshwe', 'Myanmar', 'WiFi, Restaurant, Garden, Bike Rental, Lake View', 'Free cancellation up to 72 hours before check-in. Later cancellations charge one night.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (4, 'Bagan Sandstone Inn', 'Boutique rooms near the temple plains, sunrise balconies, and a quiet courtyard.', '5 Anawrahta Road', 'Bagan', 'Myanmar', 'WiFi, Courtyard, Temple Tours Desk, Cafe, Parking', 'Non-refundable within 7 days of arrival; otherwise free cancellation.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (5, 'Mandalay Heritage House', 'Colonial-era townhouse with teak suites, afternoon tea, and walking distance to the palace walls.', '22 84th Street', 'Mandalay', 'Myanmar', 'WiFi, Tea Lounge, Library, Courtyard, Airport Shuttle', 'Free cancellation up to 48 hours before check-in.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO room_types (id, hotel_id, name, description, max_occupancy, base_rate, amenities, created_at, updated_at)
VALUES
  (3, 2, 'Harbour Twin', 'Twin beds with harbour balcony', 2, 119.00, 'WiFi, Balcony, Mini Bar', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (4, 2, 'Skyline Suite', 'Corner suite with lounge seating', 3, 189.00, 'WiFi, Lounge, Bathtub, City View', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (5, 3, 'Lake Cabin', 'Timber cabin facing the water', 2, 79.00, 'WiFi, Fan, Lake View', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (6, 3, 'Garden Pavilion', 'Larger pavilion with private deck', 4, 129.00, 'WiFi, Deck, Daybed', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (7, 4, 'Sandstone Double', 'Quiet double with courtyard light', 2, 69.00, 'WiFi, AC, Courtyard View', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (8, 4, 'Sunrise Balcony', 'Balcony room for temple dawns', 2, 99.00, 'WiFi, Balcony, Telescope Desk', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9, 5, 'Teak Classic', 'Heritage double with writing desk', 2, 95.00, 'WiFi, Desk, Heritage Furnishings', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (10, 5, 'Garden Suite', 'Suite opening onto the courtyard', 3, 155.00, 'WiFi, Sitting Area, Courtyard Access', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO rooms (id, room_type_id, room_number, status, created_at, updated_at)
VALUES
  (4, 3, '301', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (5, 3, '302', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (6, 4, '401', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (7, 5, 'L1', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (8, 5, 'L2', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9, 6, 'G1', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (10, 7, '11', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (11, 7, '12', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (12, 8, '21', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (13, 9, 'H1', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (14, 9, 'H2', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (15, 10, 'HS1', 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO blackout_dates (id, hotel_id, room_type_id, start_date, end_date, reason, created_at, updated_at)
VALUES
  (3, 2, NULL, '2026-11-10', '2026-11-13', 'Waterfront festival — lobby events only', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (4, 3, 6, '2026-08-01', '2026-08-15', 'Garden Pavilion deck repairs', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (5, 4, NULL, '2026-10-20', '2026-10-22', 'Full property private wedding buyout', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (6, 5, 10, '2026-09-15', '2026-09-20', 'Garden Suite restoration', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO rag_documents (id, title, audience, hotel_id, source_type, created_at, updated_at)
VALUES
  (3, 'Guest FAQ - Harbourview Suites', 'PUBLIC', 2, 'FAQ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (4, 'Guest FAQ - Inle Lake Lodge', 'PUBLIC', 3, 'FAQ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (5, 'Guest FAQ - Bagan Sandstone Inn', 'PUBLIC', 4, 'FAQ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (6, 'Guest FAQ - Mandalay Heritage House', 'PUBLIC', 5, 'FAQ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (7, 'Staff SOP - Multi-property Notes', 'STAFF', NULL, 'SOP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO rag_chunks (id, document_id, content, section, audience, hotel_id, created_at)
VALUES
  (6, 3, 'Harbourview Suites check-in is from 3:00 PM. Rooftop pool hours are 6:00 AM to 9:00 PM. River-view rooms include complimentary morning coffee.', 'Check-in & Amenities', 'PUBLIC', 2, CURRENT_TIMESTAMP),
  (7, 3, 'Free cancellation up to 24 hours before check-in. Airport transfers can be arranged at the concierge desk for a fixed fee.', 'Cancellation & Transfers', 'PUBLIC', 2, CURRENT_TIMESTAMP),
  (8, 4, 'Inle Lake Lodge offers complimentary canoe transfer from the jetty. Dinner is served in the garden from 6:00 PM to 9:00 PM.', 'Transfers & Dining', 'PUBLIC', 3, CURRENT_TIMESTAMP),
  (9, 4, 'Bike rentals are complimentary for guests. Lake Cabin rooms sleep two; Garden Pavilions sleep up to four.', 'Activities & Rooms', 'PUBLIC', 3, CURRENT_TIMESTAMP),
  (10, 5, 'Bagan Sandstone Inn can arrange private temple sunrise tours. Quiet hours begin at 10:00 PM in the courtyard.', 'Tours & Quiet Hours', 'PUBLIC', 4, CURRENT_TIMESTAMP),
  (11, 5, 'Sunrise Balcony rooms face east toward the plains. Sandstone Double rooms overlook the inner courtyard.', 'Room Orientation', 'PUBLIC', 4, CURRENT_TIMESTAMP),
  (12, 6, 'Afternoon tea is served daily from 3:00 PM to 5:00 PM in the lounge. Airport shuttle departs on the hour from 8:00 AM to 6:00 PM.', 'Tea & Shuttle', 'PUBLIC', 5, CURRENT_TIMESTAMP),
  (13, 6, 'Heritage House rooms keep original teak floors. Please use provided slippers indoors.', 'House Notes', 'PUBLIC', 5, CURRENT_TIMESTAMP),
  (14, 7, 'When guests ask about multi-city itineraries, recommend Harbourview for Yangon arrival nights, Inle for mid-trip rest, Bagan for temples, and Heritage House for Mandalay.', 'Itinerary Tips', 'STAFF', NULL, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Sample guests + September bookings for staff month queries
INSERT INTO guests (id, email, full_name, phone, created_at, updated_at)
VALUES
  (1, 'aye.chan@example.com', 'Aye Chan', '+9591110001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 'min.htet@example.com', 'Min Htet', '+9591110002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 'su.myat@example.com', 'Su Myat', '+9591110003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (4, 'ko.ko@example.com', 'Ko Ko', '+9591110004', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (5, 'thin.zar@example.com', 'Thin Zar', '+9591110005', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO bookings (id, hotel_id, room_id, guest_id, check_in_date, check_out_date, num_guests, total_price, status, created_at, updated_at)
VALUES
  (1, 1, 1, 1, '2026-09-05', '2026-09-07', 2, 178.00, 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 1, 3, 2, '2026-09-12', '2026-09-14', 2, 298.00, 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 2, 4, 3, '2026-09-03', '2026-09-05', 2, 238.00, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (4, 2, 6, 4, '2026-09-18', '2026-09-20', 2, 378.00, 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (5, 3, 7, 5, '2026-09-08', '2026-09-10', 2, 158.00, 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (6, 4, 10, 1, '2026-09-22', '2026-09-24', 2, 138.00, 'CANCELLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (7, 5, 13, 2, '2026-09-15', '2026-09-17', 2, 190.00, 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (8, 1, 2, 3, '2026-10-02', '2026-10-04', 2, 178.00, 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('hotels', 'id'), (SELECT MAX(id) FROM hotels));
SELECT setval(pg_get_serial_sequence('room_types', 'id'), (SELECT MAX(id) FROM room_types));
SELECT setval(pg_get_serial_sequence('rooms', 'id'), (SELECT MAX(id) FROM rooms));
SELECT setval(pg_get_serial_sequence('blackout_dates', 'id'), (SELECT MAX(id) FROM blackout_dates));
SELECT setval(pg_get_serial_sequence('rag_documents', 'id'), (SELECT MAX(id) FROM rag_documents));
SELECT setval(pg_get_serial_sequence('rag_chunks', 'id'), (SELECT MAX(id) FROM rag_chunks));
SELECT setval(pg_get_serial_sequence('guests', 'id'), (SELECT MAX(id) FROM guests));
SELECT setval(pg_get_serial_sequence('bookings', 'id'), (SELECT MAX(id) FROM bookings));

COMMIT;

SELECT id, name, city FROM hotels ORDER BY id;
SELECT hotel_id, COUNT(*) AS room_types FROM room_types GROUP BY hotel_id ORDER BY hotel_id;
SELECT COUNT(*) AS bookings FROM bookings;
