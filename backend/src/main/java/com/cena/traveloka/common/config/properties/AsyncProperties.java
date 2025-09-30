package com.cena.traveloka.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for async task execution thread pool.
 *
 * <p>Binds to properties with prefix "spring.task.execution.pool".</p>
 *
 * <p>Configuration example in application.yml:</p>
 * <pre>
 * spring:
 *   task:
 *     execution:
 *       pool:
 *         core-size: 10
 *         max-size: 50
 *         queue-capacity: 1000
 *       thread-name-prefix: "AsyncExecutor-"
 * </pre>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Thread pool sizing for async operations</li>
 *   <li>Queue capacity configuration</li>
 *   <li>Thread naming for debugging</li>
 *   <li>Keep-alive time settings</li>
 * </ul>
 *
 * <p>From research.md specifications:</p>
 * <ul>
 *   <li>Core size: 10 (maintains threads for immediate async task execution)</li>
 *   <li>Max size: 50 (handles burst loads without overwhelming system resources)</li>
 *   <li>Queue capacity: 1000 (buffers tasks during high load periods)</li>
 *   <li>Thread name prefix: "AsyncExecutor-" (simplifies debugging and monitoring)</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "spring.task.execution.pool")
public class AsyncProperties {

    /**
     * Core number of threads in the pool
     * This is the minimum number of threads that will be maintained
     * Default: 10 (from specification)
     */
    private int coreSize = 10;

    /**
     * Maximum number of threads in the pool
     * This is the maximum number of threads that can be created
     * Default: 50 (from specification)
     */
    private int maxSize = 50;

    /**
     * Queue capacity for pending tasks
     * Tasks will be queued if all core threads are busy
     * Default: 1000 (from specification)
     */
    private int queueCapacity = 1000;

    /**
     * Thread name prefix for identification
     * Default: "AsyncExecutor-" (from specification)
     */
    private String threadNamePrefix = "AsyncExecutor-";

    /**
     * Keep-alive time in seconds for idle threads above core size
     * Default: 60 seconds
     */
    private int keepAliveSeconds = 60;

    /**
     * Whether to allow core threads to timeout
     * Default: false
     */
    private boolean allowCoreThreadTimeout = false;

    /**
     * Whether to wait for tasks to complete on shutdown
     * Default: true
     */
    private boolean waitForTasksToCompleteOnShutdown = true;

    /**
     * Grace period in seconds to wait for tasks on shutdown
     * Default: 30 seconds
     */
    private int awaitTerminationSeconds = 30;

    /**
     * Whether threads should be daemon threads
     * Default: false
     */
    private boolean daemon = false;

    /**
     * Thread priority (1-10, where 5 is normal)
     * Default: 5 (Thread.NORM_PRIORITY)
     */
    private int threadPriority = Thread.NORM_PRIORITY;

    /**
     * Default constructor
     */
    public AsyncProperties() {
    }

    /**
     * Gets the core pool size.
     *
     * @return Core pool size
     */
    public int getCoreSize() {
        return coreSize;
    }

    /**
     * Sets the core pool size.
     *
     * @param coreSize Core pool size
     */
    public void setCoreSize(int coreSize) {
        this.coreSize = coreSize;
    }

    /**
     * Gets the maximum pool size.
     *
     * @return Maximum pool size
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Sets the maximum pool size.
     *
     * @param maxSize Maximum pool size
     */
    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Gets the queue capacity.
     *
     * @return Queue capacity
     */
    public int getQueueCapacity() {
        return queueCapacity;
    }

    /**
     * Sets the queue capacity.
     *
     * @param queueCapacity Queue capacity
     */
    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    /**
     * Gets the thread name prefix.
     *
     * @return Thread name prefix
     */
    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    /**
     * Sets the thread name prefix.
     *
     * @param threadNamePrefix Thread name prefix
     */
    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    /**
     * Gets the keep-alive time in seconds.
     *
     * @return Keep-alive time
     */
    public int getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    /**
     * Sets the keep-alive time in seconds.
     *
     * @param keepAliveSeconds Keep-alive time
     */
    public void setKeepAliveSeconds(int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    /**
     * Checks if core thread timeout is allowed.
     *
     * @return true if core thread timeout is allowed
     */
    public boolean isAllowCoreThreadTimeout() {
        return allowCoreThreadTimeout;
    }

    /**
     * Sets whether to allow core thread timeout.
     *
     * @param allowCoreThreadTimeout true to allow timeout
     */
    public void setAllowCoreThreadTimeout(boolean allowCoreThreadTimeout) {
        this.allowCoreThreadTimeout = allowCoreThreadTimeout;
    }

    /**
     * Checks if executor should wait for tasks on shutdown.
     *
     * @return true if should wait for tasks
     */
    public boolean isWaitForTasksToCompleteOnShutdown() {
        return waitForTasksToCompleteOnShutdown;
    }

    /**
     * Sets whether to wait for tasks to complete on shutdown.
     *
     * @param waitForTasksToCompleteOnShutdown true to wait
     */
    public void setWaitForTasksToCompleteOnShutdown(boolean waitForTasksToCompleteOnShutdown) {
        this.waitForTasksToCompleteOnShutdown = waitForTasksToCompleteOnShutdown;
    }

    /**
     * Gets the await termination time in seconds.
     *
     * @return Await termination seconds
     */
    public int getAwaitTerminationSeconds() {
        return awaitTerminationSeconds;
    }

    /**
     * Sets the await termination time in seconds.
     *
     * @param awaitTerminationSeconds Await termination seconds
     */
    public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
        this.awaitTerminationSeconds = awaitTerminationSeconds;
    }

    /**
     * Checks if threads should be daemon threads.
     *
     * @return true if daemon threads
     */
    public boolean isDaemon() {
        return daemon;
    }

    /**
     * Sets whether threads should be daemon threads.
     *
     * @param daemon true for daemon threads
     */
    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    /**
     * Gets the thread priority.
     *
     * @return Thread priority (1-10)
     */
    public int getThreadPriority() {
        return threadPriority;
    }

    /**
     * Sets the thread priority.
     *
     * @param threadPriority Thread priority (1-10)
     */
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