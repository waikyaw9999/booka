package com.booka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "booka.stripe")
@Getter
@Setter
public class StripeProperties {
    private String secretKey = "";
    private String webhookSecret = "";
    private String successUrl = "http://localhost:5173/book?payment=success";
    private String cancelUrl = "http://localhost:5173/book?payment=cancelled";
    private String currency = "usd";
}
