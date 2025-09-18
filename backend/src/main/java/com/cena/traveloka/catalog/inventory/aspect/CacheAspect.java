package com.cena.traveloka.catalog.inventory.aspect;

import com.cena.traveloka.catalog.inventory.metrics.InventoryMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheAspect {

    private final InventoryMetrics metrics;
    private final CacheManager cacheManager;

    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object measureCacheHit(ProceedingJoinPoint joinPoint) throws Throwable {
        // This is a simplified implementation
        // In a real scenario, you'd need to inspect the @Cacheable annotation
        // to get the cache name and key

        try {
            Object result = joinPoint.proceed();

            // For now, we'll just log cache operations
            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();

            log.debug("Cache operation for {}.{}", className, methodName);

            return result;
        } catch (Exception e) {
            metrics.incrementError("cache.operation");
            throw e;
        }
    }

    @Around("@annotation(org.springframework.cache.annotation.CacheEvict)")
    public Object measureCacheEviction(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Object result = joinPoint.proceed();

            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();

            log.debug("Cache eviction for {}.{}", className, methodName);

            return result;
        } catch (Exception e) {
            metrics.incrementError("cache.eviction");
            throw e;
        }
    }
}