package com.booka.repository;

import com.booka.domain.entity.ChannelApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelApiKeyRepository extends JpaRepository<ChannelApiKey, Long> {
    List<ChannelApiKey> findByChannelAndActiveTrue(String channel);
}
