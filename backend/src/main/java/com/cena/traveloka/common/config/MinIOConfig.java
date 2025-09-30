package com.cena.traveloka.common.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;

/**
 * MinIO configuration with auto-bucket creation and S3-compatible setup.
 * Features:
 * - MinIO client configuration with S3 compatibility
 * - Automatic bucket creation on startup
 * - Configurable bucket policies (public/private)
 * - Connection timeout and retry settings
 * - Environment-specific endpoint configuration
 * - Health check and monitoring support
 */
@Configuration
@ConditionalOnProperty(name = "app.minio.enabled", havingValue = "true", matchIfMissing = true)
public class MinIOConfig {

    private static final Logger logger = LoggerFactory.getLogger(MinIOConfig.class);

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Value("${minio.region:us-east-1}")
    private String region;

    @Value("${minio.bucket:traveloka-default}")
    private String defaultBucket;

    @Value("${minio.bucket.auto-create:true}")
    private boolean autoCreateBuckets;

    @Value("${minio.bucket.names:traveloka-default,traveloka-images,traveloka-documents,traveloka-avatars}")
    private List<String> bucketNames;

    @Value("${minio.bucket.public-buckets:traveloka-images,traveloka-avatars}")
    private List<String> publicBuckets;

    @Value("${minio.connection.timeout:10s}")
    private Duration connectionTimeout;

    @Value("${minio.connection.read-timeout:30s}")
    private Duration readTimeout;

    @Value("${minio.connection.write-timeout:30s}")
    private Duration writeTimeout;

    @Value("${minio.connection.secure:false}")
    private boolean secure;

    private MinioClient minioClient;

    /**
     * Configure MinIO client with S3-compatible settings
     * @return configured MinioClient
     */
    @Bean
    public MinioClient minioClient() {
        try {
            minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region(region)
                .build();

            // Set timeouts
            minioClient.setTimeout(
                connectionTimeout.toMillis(),
                writeTimeout.toMillis(),
                readTimeout.toMillis()
            );

            logger.info("MinIO client configured successfully. Endpoint: {}, Region: {}", endpoint, region);
            return minioClient;

        } catch (Exception e) {
            logger.error("Failed to configure MinIO client", e);
            throw new RuntimeException("MinIO configuration failed", e);
        }
    }

    /**
     * Initialize buckets after MinIO client is configured
     */
    @PostConstruct
    public void initializeBuckets() {
        if (!autoCreateBuckets) {
            logger.info("Auto-creation of buckets is disabled");
            return;
        }

        if (minioClient == null) {
            logger.warn("MinIO client is not available, skipping bucket initialization");
            return;
        }

        try {
            for (String bucketName : bucketNames) {
                createBucketIfNotExists(bucketName);
                configureBucketPolicy(bucketName);
            }
            logger.info("MinIO bucket initialization completed successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize MinIO buckets", e);
            throw new RuntimeException("MinIO bucket initialization failed", e);
        }
    }

    /**
     * Create bucket if it doesn't exist
     * @param bucketName name of the bucket to create
     */
    private void createBucketIfNotExists(String bucketName) {
        try {
            boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            );

            if (!bucketExists) {
                minioClient.makeBucket(
                    MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .region(region)
                        .build()
                );
                logger.info("Created MinIO bucket: {}", bucketName);
            } else {
                logger.debug("MinIO bucket already exists: {}", bucketName);
            }
        } catch (Exception e) {
            logger.error("Failed to create bucket: {}", bucketName, e);
            throw new RuntimeException("Failed to create bucket: " + bucketName, e);
        }
    }

    /**
     * Configure bucket policy based on public/private settings
     * @param bucketName name of the bucket to configure
     */
    private void configureBucketPolicy(String bucketName) {
        try {
            if (publicBuckets.contains(bucketName)) {
                // Set public read policy for public buckets
                String publicReadPolicy = """
                    {
                        "Version": "2012-10-17",
                        "Statement": [
                            {
                                "Effect": "Allow",
                                "Principal": {"AWS": "*"},
                                "Action": ["s3:GetObject"],
                                "Resource": ["arn:aws:s3:::%s/*"]
                            }
                        ]
                    }
                    """.formatted(bucketName);

                minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(publicReadPolicy)
                        .build()
                );
                logger.info("Set public read policy for bucket: {}", bucketName);
            } else {
                // Private buckets don't need explicit policy (default is private)
                logger.debug("Bucket {} configured as private (default)", bucketName);
            }
        } catch (Exception e) {
            logger.warn("Failed to set policy for bucket: {} - {}", bucketName, e.getMessage());
            // Don't throw exception here as the bucket is still usable
        }
    }

    /**
     * Get the default bucket name
     * @return default bucket name
     */
    public String getDefaultBucket() {
        return defaultBucket;
    }

    /**
     * Get list of configured bucket names
     * @return list of bucket names
     */
    public List<String> getBucketNames() {
        return bucketNames;
    }

    /**
     * Check if a bucket is configured as public
     * @param bucketName bucket name to check
     * @return true if bucket is public
     */
    public boolean isPublicBucket(String bucketName) {
        return publicBuckets.contains(bucketName);
    }

    /**
     * Get the MinIO endpoint URL
     * @return endpoint URL
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Get the configured region
     * @return region name
     */
    public String getRegion() {
        return region;
    }
}