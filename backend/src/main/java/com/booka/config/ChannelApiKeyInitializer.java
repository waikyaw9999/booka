package com.booka.config;

import com.booka.repository.HotelRepository;
import com.booka.service.ChannelApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChannelApiKeyInitializer implements ApplicationRunner {

    private final HotelRepository hotelRepository;
    private final ChannelApiKeyService channelApiKeyService;

    @Value("${booka.channel.api-key:}")
    private String channelApiKey;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(channelApiKey)) {
            return;
        }
        hotelRepository.findAll().forEach(hotel ->
                channelApiKeyService.ensurePropertyChannelKeys(hotel.getId(), channelApiKey));
        log.info("Ensured per-hotel channel API keys for {} properties", hotelRepository.count());
    }
}
