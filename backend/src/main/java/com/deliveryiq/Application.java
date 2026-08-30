package com.deliveryiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DeliveryIQ Spring Boot entrypoint.
 * Bootstraps tracking, routing, analytics, security and Redis cache modules.
 */
@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
