package com.cena.traveloka.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "minio")
public class MinIOProperties {

    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String bucket = "default";

    private String region = "us-east-1";

    private boolean autoCreateBuckets = true;

    private String externalUrl;

    private int connectionTimeout = 5000;

    private int socketTimeout = 10000;

    private int maxConnections = 100;

    private boolean pathStyleAccess = true;

    private boolean secure = false;

    public MinIOProperties() {
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public boolean isAutoCreateBuckets() {
        return autoCreateBuckets;
    }

    public void setAutoCreateBuckets(boolean autoCreateBuckets) {
        this.autoCreateBuckets = autoCreateBuckets;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public boolean isPathStyleAccess() {
        return pathStyleAccess;
    }

    public void setPathStyleAccess(boolean pathStyleAccess) {
        this.pathStyleAccess = pathStyleAccess;
    }

    public boolean isSecure() {
        return secure;
    }

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