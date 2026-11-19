package com.booka.service;

import com.booka.domain.entity.ChannelApiKey;
import com.booka.repository.ChannelApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChannelApiKeyService {

    private final ChannelApiKeyRepository channelApiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final HotelService hotelService;

    @Value("${booka.channel.api-key:}")
    private String configuredChannelApiKey;

    @Transactional
    public void ensurePropertyChannelKeys(Long hotelId, String plaintextKey) {
        hotelService.findHotel(hotelId);
        if (!StringUtils.hasText(plaintextKey)) {
            return;
        }
        ensureKey("OTA", hotelId, plaintextKey);
        ensureKey("CHANNEL_MANAGER", hotelId, plaintextKey);
    }

    @Transactional
    public void ensurePropertyChannelKeysFromConfig(Long hotelId) {
        ensurePropertyChannelKeys(hotelId, configuredChannelApiKey);
    }

    private void ensureKey(String channel, Long hotelId, String plaintextKey) {
        List<ChannelApiKey> existing =
                channelApiKeyRepository.findByChannelAndHotelIdAndActiveTrue(channel, hotelId);
        boolean matches = existing.stream()
                .anyMatch(k -> passwordEncoder.matches(plaintextKey, k.getKeyHash()));
        if (matches) {
            return;
        }
        channelApiKeyRepository.save(ChannelApiKey.builder()
                .channel(channel)
                .hotelId(hotelId)
                .keyHash(passwordEncoder.encode(plaintextKey))
                .active(true)
                .build());
    }
}
