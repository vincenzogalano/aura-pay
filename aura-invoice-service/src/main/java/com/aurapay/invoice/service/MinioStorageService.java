package com.aurapay.invoice.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${aurapay.minio.bucket:aurapay-invoices}")
    private String bucketName;

    public void uploadPdf(String objectKey, byte[] pdfData) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfData)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(inputStream, pdfData.length, -1)
                            .contentType("application/pdf")
                            .build()
            );
            log.info("Successfully uploaded PDF to MinIO: bucket={}, objectKey={}", bucketName, objectKey);
        } catch (Exception e) {
            log.error("Failed to upload PDF to MinIO objectKey={}: {}", objectKey, e.getMessage(), e);
            throw new RuntimeException("MinIO upload failed for objectKey=" + objectKey, e);
        }
    }

    public byte[] downloadPdf(String objectKey) {
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .build()
        )) {
            byte[] bytes = inputStream.readAllBytes();
            log.info("Successfully downloaded PDF from MinIO: bucket={}, objectKey={}, bytes={}", bucketName, objectKey, bytes.length);
            return bytes;
        } catch (Exception e) {
            log.error("Failed to download PDF from MinIO objectKey={}: {}", objectKey, e.getMessage(), e);
            throw new RuntimeException("MinIO download failed for objectKey=" + objectKey, e);
        }
    }
}
