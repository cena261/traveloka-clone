package com.cena.traveloka.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for MinIO object storage.
 *
 * <p>Binds to properties with prefix "minio".</p>
 *
 * <p>Configuration example in application.yml:</p>
 * <pre>
 * minio:
 *   endpoint: http://localhost:9000
 *   accessKey: minio
 *   secretKey: minio123
 *   bucket: images
 *   region: us-east-1
 *   autoCreateBuckets: true
 * </pre>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>S3-compatible object storage configuration</li>
 *   <li>Automatic bucket creation support</li>
 *   <li>Region configuration</li>
 *   <li>Connection pool settings</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "minio")
public class MinIOProperties {

    /**
     * MinIO server endpoint URL
     * Example: http://localhost:9000 or https://minio.example.com
     */
    private String endpoint;

    /**
     * MinIO access key (username)
     */
    private String accessKey;

    /**
     * MinIO secret key (password)
     */
    private String secretKey;

    /**
     * Default bucket name for storing objects
     */
    private String bucket = "default";

    /**
     * AWS region (for S3 compatibility)
     * Default: us-east-1
     */
    private String region = "us-east-1";

    /**
     * Whether to automatically create buckets if they don't exist
     * Default: true
     */
    private boolean autoCreateBuckets = true;

    /**
     * External URL for accessing MinIO objects (if different from endpoint)
     * Used for generating public URLs
     */
    private String externalUrl;

    /**
     * Connection timeout in milliseconds
     * Default: 5000ms (5 seconds)
     */
    private int connectionTimeout = 5000;

    /**
     * Socket timeout in milliseconds
     * Default: 10000ms (10 seconds)
     */
    private int socketTimeout = 10000;

    /**
     * Maximum number of connections in the pool
     * Default: 100
     */
    private int maxConnections = 100;

    /**
     * Whether to use path-style access (true) or virtual-hosted-style access (false)
     * Default: true (path-style for MinIO)
     */
    private boolean pathStyleAccess = true;

    /**
     * Whether to use HTTPS for connections
     * Default: false
     */
    private boolean secure = false;

    /**
     * Default constructor
     */
    public MinIOProperties() {
    }

    /**
     * Gets the MinIO server endpoint URL.
     *
     * @return MinIO endpoint URL
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Sets the MinIO server endpoint URL.
     *
     * @param endpoint MinIO endpoint URL
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Gets the MinIO access key.
     *
     * @return Access key
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * Sets the MinIO access key.
     *
     * @param accessKey Access key
     */
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * Gets the MinIO secret key.
     *
     * @return Secret key
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * Sets the MinIO secret key.
     *
     * @param secretKey Secret key
     */
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * Gets the default bucket name.
     *
     * @return Bucket name
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Sets the default bucket name.
     *
     * @param bucket Bucket name
     */
    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    /**
     * Gets the AWS region.
     *
     * @return Region name
     */
    public String getRegion() {
        return region;
    }

    /**
     * Sets the AWS region.
     *
     * @param region Region name
     */
    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * Checks if automatic bucket creation is enabled.
     *
     * @return true if auto-create is enabled
     */
    public boolean isAutoCreateBuckets() {
        return autoCreateBuckets;
    }

    /**
     * Sets whether to automatically create buckets.
     *
     * @param autoCreateBuckets true to enable auto-create
     */
    public void setAutoCreateBuckets(boolean autoCreateBuckets) {
        this.autoCreateBuckets = autoCreateBuckets;
    }

    /**
     * Gets the external URL for MinIO objects.
     *
     * @return External URL
     */
    public String getExternalUrl() {
        return externalUrl;
    }

    /**
     * Sets the external URL for MinIO objects.
     *
     * @param externalUrl External URL
     */
    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    /**
     * Gets the connection timeout in milliseconds.
     *
     * @return Connection timeout
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Sets the connection timeout in milliseconds.
     *
     * @param connectionTimeout Connection timeout
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Gets the socket timeout in milliseconds.
     *
     * @return Socket timeout
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * Sets the socket timeout in milliseconds.
     *
     * @param socketTimeout Socket timeout
     */
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    /**
     * Gets the maximum number of connections.
     *
     * @return Maximum connections
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    /**
     * Sets the maximum number of connections.
     *
     * @param maxConnections Maximum connections
     */
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    /**
     * Checks if path-style access is enabled.
     *
     * @return true if path-style access is enabled
     */
    public boolean isPathStyleAccess() {
        return pathStyleAccess;
    }

    /**
     * Sets whether to use path-style access.
     *
     * @param pathStyleAccess true for path-style access
     */
    public void setPathStyleAccess(boolean pathStyleAccess) {
        this.pathStyleAccess = pathStyleAccess;
    }

    /**
     * Checks if HTTPS is enabled.
     *
     * @return true if secure connections are enabled
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * Sets whether to use HTTPS.
     *
     * @param secure true for HTTPS connections
     */
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    @Override
    public String toString() {
        return "MinIOProperties{" +
                "endpoint='" + endpoint + '\'' +
                ", accessKey='" + (accessKey != null ? "***" : "null") + '\'' +
                ", secretKey='" + (secretKey != null ? "***" : "null") + '\'' +
                ", bucket='" + bucket + '\'' +
                ", region='" + region + '\'' +
                ", autoCreateBuckets=" + autoCreateBuckets +
                ", externalUrl='" + externalUrl + '\'' +
                ", connectionTimeout=" + connectionTimeout +
                ", socketTimeout=" + socketTimeout +
                ", maxConnections=" + maxConnections +
                ", pathStyleAccess=" + pathStyleAccess +
                ", secure=" + secure +
                '}';
    }
}