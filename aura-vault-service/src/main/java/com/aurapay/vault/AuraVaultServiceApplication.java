package com.aurapay.vault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AuraVaultServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuraVaultServiceApplication.class, args);
    }
}
