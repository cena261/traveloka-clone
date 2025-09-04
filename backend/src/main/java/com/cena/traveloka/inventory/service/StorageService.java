package com.cena.traveloka.inventory.service;

import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class StorageService {
    private final MinioClient minio;

    @Value("${minio.bucket}") private String bucket;
    @Value("${minio.external-url}") private String externalUrl;

    @PostConstruct
    public void ensureBucket() throws Exception {
        boolean exists = minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
    }

    public String put(String objectKey, MultipartFile file) throws Exception {
        try (InputStream in = file.getInputStream()) {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .build();
            minio.putObject(args);
        }
        // URL công khai qua reverse proxy/CDN hoặc link trực tiếp MinIO
        return externalUrl + "/" + bucket + "/" + objectKey;
    }

    public String presignGet(String objectKey, int minutes) throws Exception {
        return minio.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET).bucket(bucket).object(objectKey)
                .expiry(minutes, TimeUnit.MINUTES).build());
    }
}
