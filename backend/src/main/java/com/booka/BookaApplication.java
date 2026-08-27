package com.booka;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BookaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookaApplication.class, args);
    }
}
