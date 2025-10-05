package com.cena.traveloka.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@ConditionalOnProperty(name = "app.async.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    @Value("${app.async.core-pool-size:10}")
    private int corePoolSize;

    @Value("${app.async.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${app.async.queue-capacity:1000}")
    private int queueCapacity;

    @Value("${app.async.thread-name-prefix:AsyncExecutor-}")
    private String threadNamePrefix;

    @Value("${app.async.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Value("${app.async.allow-core-thread-timeout:true}")
    private boolean allowCoreThreadTimeOut;

    @Value("${app.async.wait-for-tasks-to-complete-on-shutdown:true}")
    private boolean waitForTasksToCompleteOnShutdown;

    @Value("${app.async.await-termination-seconds:30}")
    private int awaitTerminationSeconds;

    @Value("${app.async.rejected-execution-policy:CALLER_RUNS}")
    private String rejectedExecutionPolicy;

    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);

        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setAllowCoreThreadTimeOut(allowCoreThreadTimeOut);

        executor.setWaitForTasksToCompleteOnShutdown(waitForTasksToCompleteOnShutdown);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);

        executor.setRejectedExecutionHandler(getRejectedExecutionHandler());

        executor.setTaskDecorator(runnable -> () -> {
            String originalThreadName = Thread.currentThread().getName();
            try {
                runnable.run();
            } finally {
                Thread.currentThread().setName(originalThreadName);
            }
        });

        executor.initialize();

        logger.info("Async task executor configured: core={}, max={}, queue={}, prefix={}",
            corePoolSize, maxPoolSize, queueCapacity, threadNamePrefix);

        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }

    @Bean(name = "emailTaskExecutor")
    public AsyncTaskExecutor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("EmailExecutor-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        logger.info("Email task executor configured with core=2, max=10, queue=100");
        return executor;
    }

    @Bean(name = "notificationTaskExecutor")
    public AsyncTaskExecutor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(15);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("NotificationExecutor-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        logger.info("Notification task executor configured with core=3, max=15, queue=200");
        return executor;
    }

    @Bean(name = "searchTaskExecutor")
    public AsyncTaskExecutor searchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("SearchExecutor-");
        executor.setKeepAliveSeconds(120);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        logger.info("Search task executor configured with core=5, max=20, queue=500");
        return executor;
    }

    @Bean(name = "fileTaskExecutor")
    public AsyncTaskExecutor fileTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("FileExecutor-");
        executor.setKeepAliveSeconds(300);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        logger.info("File task executor configured with core=2, max=8, queue=50");
        return executor;
    }

    private RejectedExecutionHandler getRejectedExecutionHandler() {
        return switch (rejectedExecutionPolicy.toUpperCase()) {
            case "ABORT" -> new ThreadPoolExecutor.AbortPolicy();
            case "DISCARD" -> new ThreadPoolExecutor.DiscardPolicy();
            case "DISCARD_OLDEST" -> new ThreadPoolExecutor.DiscardOldestPolicy();
            case "CALLER_RUNS" -> new ThreadPoolExecutor.CallerRunsPolicy();
            default -> {
                logger.warn("Unknown rejected execution policy: {}, using CALLER_RUNS", rejectedExecutionPolicy);
                yield new ThreadPoolExecutor.CallerRunsPolicy();
            }
        };
    }

    public static class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(CustomAsyncExceptionHandler.class);

        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            logger.error("Async method execution failed. Method: {}.{}, Parameters: {}",
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                params != null ? params.length : 0,
                ex);

        }
    }
}