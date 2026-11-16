-- Rewrite demo Unsplash URLs to SPA-bundled /media assets (idempotent).
-- Fresh installs: 005/006 seed remote URLs; this changeset localizes them.
-- Existing DBs: same UPDATE matches only remaining Unsplash seed rows.

UPDATE hotel_images SET url = '/media/hotels/grand-lobby.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1566073771259-6a8506099945%';

UPDATE hotel_images SET url = '/media/hotels/grand-pool.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1582719508461-905c673771fd%';

UPDATE hotel_images SET url = '/media/hotels/grand-corridor.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1611892440504-42a792e24d32%';

UPDATE hotel_images SET url = '/media/hotels/harbour-facade.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1520250497591-112f2f40a3f4%';

UPDATE hotel_images SET url = '/media/hotels/harbour-pool.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1571896349842-33c89424de2d%';

UPDATE hotel_images SET url = '/media/hotels/inle-lake.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1439066615861-d1af74d74000%';

UPDATE hotel_images SET url = '/media/hotels/inle-water.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e%';

UPDATE hotel_images SET url = '/media/hotels/bagan-plains.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1548013146-72479768bada%';

UPDATE hotel_images SET url = '/media/hotels/bagan-courtyard.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b%';

UPDATE hotel_images SET url = '/media/hotels/mandalay-facade.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa%';

UPDATE hotel_images SET url = '/media/hotels/mandalay-lounge.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1564501049412-61c2a3083791%';

UPDATE room_images SET url = '/media/rooms/standard.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304%';

UPDATE room_images SET url = '/media/rooms/deluxe.jpg', updated_at = CURRENT_TIMESTAMP
WHERE room_id = 3 AND url LIKE 'https://images.unsplash.com/photo-1590490360182-c33d57733427%';

UPDATE room_images SET url = '/media/rooms/harbour-twin.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1591088398332-8a7791972843%';

UPDATE room_images SET url = '/media/rooms/skyline-suite.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1618773928121-c32242e63f39%';

UPDATE room_images SET url = '/media/rooms/lake-cabin.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af%';

UPDATE room_images SET url = '/media/rooms/garden-pavilion.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1595576508898-0ad5c879a061%';

UPDATE room_images SET url = '/media/rooms/sandstone-double.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658%';

UPDATE room_images SET url = '/media/rooms/sunrise-balcony.jpg', updated_at = CURRENT_TIMESTAMP
WHERE room_id = 12 AND url LIKE 'https://images.unsplash.com/photo-1590490360182-c33d57733427%';

UPDATE room_images SET url = '/media/rooms/teak-classic.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1616594039964-ae9021a400a0%';

UPDATE room_images SET url = '/media/rooms/garden-suite.jpg', updated_at = CURRENT_TIMESTAMP
WHERE url LIKE 'https://images.unsplash.com/photo-1578683010236-d716f9a3f461%';
