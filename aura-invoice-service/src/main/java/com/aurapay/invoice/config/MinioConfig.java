package com.aurapay.invoice.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${aurapay.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${aurapay.minio.access-key:minio_admin}")
    private String accessKey;

    @Value("${aurapay.minio.secret-key:minio_password}")
    private String secretKey;

    @Value("${aurapay.minio.bucket:aurapay-invoices}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public CommandLineRunner minioBucketInitializer(MinioClient minioClient) {
        return args -> {
            try {
                boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
                if (!found) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                    log.info("MinIO bucket '{}' created successfully", bucketName);
                } else {
                    log.info("MinIO bucket '{}' already exists", bucketName);
                }
            } catch (Exception e) {
                log.warn("Could not auto-create MinIO bucket '{}' at startup (Endpoint: {}). Error: {}",
                        bucketName, endpoint, e.getMessage());
            }
        };
    }
}
