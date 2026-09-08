package com.booka.repository;

import com.booka.domain.entity.RoomImage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {

    List<RoomImage> findByRoomIdOrderBySortOrderAscIdAsc(Long roomId);

    Optional<RoomImage> findByIdAndRoomId(Long id, Long roomId);

    Optional<RoomImage> findFirstByRoomIdAndPrimaryTrue(Long roomId);

    List<RoomImage> findByRoomIdInAndPrimaryTrue(Collection<Long> roomIds);

    /**
     * Representative image for a room type: prefer a primary room image, then lowest sort order.
     */
    @Query("""
            SELECT i.url FROM RoomImage i
            WHERE i.roomId IN (SELECT r.id FROM Room r WHERE r.roomTypeId = :roomTypeId)
            ORDER BY i.primary DESC, i.sortOrder ASC, i.id ASC
            """)
    List<String> findUrlsForRoomType(@Param("roomTypeId") Long roomTypeId, Pageable pageable);

    default Optional<String> findDisplayUrlForRoomType(Long roomTypeId) {
        List<String> urls = findUrlsForRoomType(roomTypeId, PageRequest.of(0, 1));
        return urls.stream().findFirst();
    }

    @Query("SELECT COALESCE(MAX(i.sortOrder), -1) FROM RoomImage i WHERE i.roomId = :roomId")
    int findMaxSortOrder(@Param("roomId") Long roomId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RoomImage i SET i.primary = false WHERE i.roomId = :roomId")
    void clearPrimaryForRoom(@Param("roomId") Long roomId);

    long countByRoomId(Long roomId);
}
