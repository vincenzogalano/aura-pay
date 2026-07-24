package com.aurapay.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AuraWebhookApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuraWebhookApplication.class, args);
    }
}
