package com.booka.service;

import com.booka.config.UploadProperties;
import com.booka.domain.entity.HotelImage;
import com.booka.domain.entity.Room;
import com.booka.domain.entity.RoomImage;
import com.booka.domain.entity.RoomType;
import com.booka.dto.ImageDtos.AddImageRequest;
import com.booka.dto.ImageDtos.ImageResponse;
import com.booka.exception.BusinessException;
import com.booka.exception.ResourceNotFoundException;
import com.booka.repository.HotelImageRepository;
import com.booka.repository.RoomImageRepository;
import com.booka.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private final HotelImageRepository hotelImageRepository;
    private final RoomImageRepository roomImageRepository;
    private final RoomRepository roomRepository;
    private final HotelService hotelService;
    private final UploadProperties uploadProperties;

    public List<ImageResponse> listHotelImages(Long hotelId) {
        hotelService.findHotel(hotelId);
        return hotelImageRepository.findByHotelIdOrderBySortOrderAscIdAsc(hotelId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ImageResponse addHotelImage(Long hotelId, AddImageRequest request, Long staffHotelId) {
        hotelService.assertHotelAccess(hotelId, staffHotelId);
        hotelService.findHotel(hotelId);
        String url = normalizeUrl(request.url());
        boolean makePrimary = Boolean.TRUE.equals(request.primary())
                || hotelImageRepository.countByHotelId(hotelId) == 0;
        if (makePrimary) {
            hotelImageRepository.clearPrimaryForHotel(hotelId);
        }
        int sortOrder = request.sortOrder() != null
                ? request.sortOrder()
                : hotelImageRepository.findMaxSortOrder(hotelId) + 1;
        HotelImage image = HotelImage.builder()
                .hotelId(hotelId)
                .url(url)
                .caption(normalizeCaption(request.caption()))
                .sortOrder(sortOrder)
                .primary(makePrimary)
                .build();
        return toResponse(hotelImageRepository.save(image));
    }

    @Transactional
    public void deleteHotelImage(Long hotelId, Long imageId, Long staffHotelId) {
        hotelService.assertHotelAccess(hotelId, staffHotelId);
        HotelImage image = hotelImageRepository.findByIdAndHotelId(imageId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel image not found: " + imageId));
        boolean wasPrimary = image.isPrimary();
        hotelImageRepository.delete(image);
        if (wasPrimary) {
            promoteFirstHotelImage(hotelId);
        }
    }

    @Transactional
    public ImageResponse setPrimaryHotelImage(Long hotelId, Long imageId, Long staffHotelId) {
        hotelService.assertHotelAccess(hotelId, staffHotelId);
        HotelImage image = hotelImageRepository.findByIdAndHotelId(imageId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel image not found: " + imageId));
        hotelImageRepository.clearPrimaryForHotel(hotelId);
        image.setPrimary(true);
        return toResponse(hotelImageRepository.save(image));
    }

    @Transactional
    public ImageResponse uploadHotelImage(
            Long hotelId, MultipartFile file, String caption, Boolean primary, Long staffHotelId) {
        String publicUrl = storeUpload(file);
        return addHotelImage(hotelId, new AddImageRequest(publicUrl, caption, null, primary), staffHotelId);
    }

    public List<ImageResponse> listRoomImages(Long roomId, Long staffHotelId) {
        Room room = findRoomWithAccess(roomId, staffHotelId);
        return roomImageRepository.findByRoomIdOrderBySortOrderAscIdAsc(room.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ImageResponse addRoomImage(Long roomId, AddImageRequest request, Long staffHotelId) {
        Room room = findRoomWithAccess(roomId, staffHotelId);
        String url = normalizeUrl(request.url());
        boolean makePrimary = Boolean.TRUE.equals(request.primary())
                || roomImageRepository.countByRoomId(room.getId()) == 0;
        if (makePrimary) {
            roomImageRepository.clearPrimaryForRoom(room.getId());
        }
        int sortOrder = request.sortOrder() != null
                ? request.sortOrder()
                : roomImageRepository.findMaxSortOrder(room.getId()) + 1;
        RoomImage image = RoomImage.builder()
                .roomId(room.getId())
                .url(url)
                .caption(normalizeCaption(request.caption()))
                .sortOrder(sortOrder)
                .primary(makePrimary)
                .build();
        return toResponse(roomImageRepository.save(image));
    }

    @Transactional
    public void deleteRoomImage(Long roomId, Long imageId, Long staffHotelId) {
        findRoomWithAccess(roomId, staffHotelId);
        RoomImage image = roomImageRepository.findByIdAndRoomId(imageId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room image not found: " + imageId));
        boolean wasPrimary = image.isPrimary();
        roomImageRepository.delete(image);
        if (wasPrimary) {
            promoteFirstRoomImage(roomId);
        }
    }

    @Transactional
    public ImageResponse setPrimaryRoomImage(Long roomId, Long imageId, Long staffHotelId) {
        findRoomWithAccess(roomId, staffHotelId);
        RoomImage image = roomImageRepository.findByIdAndRoomId(imageId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room image not found: " + imageId));
        roomImageRepository.clearPrimaryForRoom(roomId);
        image.setPrimary(true);
        return toResponse(roomImageRepository.save(image));
    }

    @Transactional
    public ImageResponse uploadRoomImage(
            Long roomId, MultipartFile file, String caption, Boolean primary, Long staffHotelId) {
        String publicUrl = storeUpload(file);
        return addRoomImage(roomId, new AddImageRequest(publicUrl, caption, null, primary), staffHotelId);
    }

    public String primaryHotelImageUrl(Long hotelId) {
        return hotelImageRepository.findFirstByHotelIdAndPrimaryTrue(hotelId)
                .map(HotelImage::getUrl)
                .orElse(null);
    }

    public List<ImageResponse> hotelImagesForResponse(Long hotelId) {
        return hotelImageRepository.findByHotelIdOrderBySortOrderAscIdAsc(hotelId).stream()
                .map(this::toResponse)
                .toList();
    }

    public String primaryRoomImageUrl(Long roomId) {
        return roomImageRepository.findFirstByRoomIdAndPrimaryTrue(roomId)
                .map(RoomImage::getUrl)
                .orElse(null);
    }

    private void promoteFirstHotelImage(Long hotelId) {
        List<HotelImage> remaining = hotelImageRepository.findByHotelIdOrderBySortOrderAscIdAsc(hotelId);
        if (!remaining.isEmpty()) {
            HotelImage first = remaining.get(0);
            first.setPrimary(true);
            hotelImageRepository.save(first);
        }
    }

    private void promoteFirstRoomImage(Long roomId) {
        List<RoomImage> remaining = roomImageRepository.findByRoomIdOrderBySortOrderAscIdAsc(roomId);
        if (!remaining.isEmpty()) {
            RoomImage first = remaining.get(0);
            first.setPrimary(true);
            roomImageRepository.save(first);
        }
    }

    private Room findRoomWithAccess(Long roomId, Long staffHotelId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
        RoomType roomType = hotelService.findRoomType(room.getRoomTypeId());
        hotelService.assertHotelAccess(roomType.getHotelId(), staffHotelId);
        return room;
    }

    private String storeUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Upload file is required");
        }
        if (file.getSize() > uploadProperties.getMaxFileSizeBytes()) {
            throw new BusinessException("File exceeds max size of "
                    + uploadProperties.getMaxFileSizeBytes() + " bytes");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("Only JPEG, PNG, WebP, and GIF images are allowed");
        }
        String ext = extensionFor(contentType);
        try {
            Path dir = Path.of(uploadProperties.getDir()).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + ext;
            Path target = dir.resolve(filename);
            file.transferTo(target);
            String publicPath = uploadProperties.getPublicPath();
            if (!publicPath.startsWith("/")) {
                publicPath = "/" + publicPath;
            }
            if (publicPath.endsWith("/")) {
                publicPath = publicPath.substring(0, publicPath.length() - 1);
            }
            return publicPath + "/" + filename;
        } catch (IOException e) {
            log.error("Failed to store upload", e);
            throw new BusinessException("Failed to store uploaded file");
        }
    }

    private static String extensionFor(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new BusinessException("Image URL is required");
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("/uploads/")) {
            return trimmed;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("https://") || lower.startsWith("http://"))) {
            throw new BusinessException("Image URL must be http(s) or a /uploads/ path");
        }
        if (trimmed.length() > 2048) {
            throw new BusinessException("Image URL is too long");
        }
        return trimmed;
    }

    private String normalizeCaption(String caption) {
        if (caption == null || caption.isBlank()) {
            return null;
        }
        String trimmed = caption.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    private ImageResponse toResponse(HotelImage image) {
        return new ImageResponse(
                image.getId(),
                image.getUrl(),
                image.getCaption(),
                image.getSortOrder(),
                image.isPrimary());
    }

    private ImageResponse toResponse(RoomImage image) {
        return new ImageResponse(
                image.getId(),
                image.getUrl(),
                image.getCaption(),
                image.getSortOrder(),
                image.isPrimary());
    }
}
