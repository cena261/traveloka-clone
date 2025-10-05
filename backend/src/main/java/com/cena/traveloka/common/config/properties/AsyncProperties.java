package com.cena.traveloka.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.task.execution.pool")
public class AsyncProperties {

    private int coreSize = 10;

    private int maxSize = 50;

    private int queueCapacity = 1000;

    private String threadNamePrefix = "AsyncExecutor-";

    private int keepAliveSeconds = 60;

    private boolean allowCoreThreadTimeout = false;

    private boolean waitForTasksToCompleteOnShutdown = true;

    private int awaitTerminationSeconds = 30;

    private boolean daemon = false;

    private int threadPriority = Thread.NORM_PRIORITY;

    public AsyncProperties() {
    }

    public int getCoreSize() {
        return coreSize;
    }

    public void setCoreSize(int coreSize) {
        this.coreSize = coreSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    public int getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public void setKeepAliveSeconds(int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    public boolean isAllowCoreThreadTimeout() {
        return allowCoreThreadTimeout;
    }

    public void setAllowCoreThreadTimeout(boolean allowCoreThreadTimeout) {
        this.allowCoreThreadTimeout = allowCoreThreadTimeout;
    }

    public boolean isWaitForTasksToCompleteOnShutdown() {
        return waitForTasksToCompleteOnShutdown;
    }

    public void setWaitForTasksToCompleteOnShutdown(boolean waitForTasksToCompleteOnShutdown) {
        this.waitForTasksToCompleteOnShutdown = waitForTasksToCompleteOnShutdown;
    }

    public int getAwaitTerminationSeconds() {
        return awaitTerminationSeconds;
    }

    public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
        this.awaitTerminationSeconds = awaitTerminationSeconds;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public int getThreadPriority() {
        return threadPriority;
    }

    public void setThreadPriority(int threadPriority) {
        this.threadPriority = threadPriority;
    }

    @Override
    public String toString() {
        return "AsyncProperties{" +
                "coreSize=" + coreSize +
                ", maxSize=" + maxSize +
                ", queueCapacity=" + queueCapacity +
                ", threadNamePrefix='" + threadNamePrefix + '\'' +
                ", keepAliveSeconds=" + keepAliveSeconds +
                ", allowCoreThreadTimeout=" + allowCoreThreadTimeout +
                ", waitForTasksToCompleteOnShutdown=" + waitForTasksToCompleteOnShutdown +
                ", awaitTerminationSeconds=" + awaitTerminationSeconds +
                ", daemon=" + daemon +
                ", threadPriority=" + threadPriority +
                '}';
    }
}