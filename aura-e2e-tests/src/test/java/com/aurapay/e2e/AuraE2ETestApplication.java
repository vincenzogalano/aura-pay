package com.aurapay.e2e;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.aurapay")
@EntityScan(basePackages = "com.aurapay")
@EnableJpaRepositories(basePackages = "com.aurapay")
public class AuraE2ETestApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuraE2ETestApplication.class, args);
    }
}
