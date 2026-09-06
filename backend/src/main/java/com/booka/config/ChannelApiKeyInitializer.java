package com.booka.config;

import com.booka.domain.entity.ChannelApiKey;
import com.booka.repository.ChannelApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChannelApiKeyInitializer implements ApplicationRunner {

    private final ChannelApiKeyRepository channelApiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${booka.channel.api-key:}")
    private String channelApiKey;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(channelApiKey)) {
            return;
        }
        boolean exists = channelApiKeyRepository.findByChannelAndActiveTrue("OTA").stream()
                .anyMatch(k -> passwordEncoder.matches(channelApiKey, k.getKeyHash()));
        if (exists) {
            return;
        }
        channelApiKeyRepository.save(ChannelApiKey.builder()
                .channel("OTA")
                .keyHash(passwordEncoder.encode(channelApiKey))
                .active(true)
                .build());
        channelApiKeyRepository.save(ChannelApiKey.builder()
                .channel("CHANNEL_MANAGER")
                .keyHash(passwordEncoder.encode(channelApiKey))
                .active(true)
                .build());
        log.info("Seeded channel API keys for OTA and CHANNEL_MANAGER from CHANNEL_API_KEY");
    }
}
