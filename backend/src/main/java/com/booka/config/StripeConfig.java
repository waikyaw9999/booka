package com.booka.config;

import com.stripe.StripeClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class StripeConfig {

    @Bean
    StripeClient stripeClient(StripeProperties properties) {
        if (!StringUtils.hasText(properties.getSecretKey())) {
            return new StripeClient("sk_test_unconfigured");
        }
        return new StripeClient(properties.getSecretKey());
    }
}
