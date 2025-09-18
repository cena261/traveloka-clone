package com.cena.traveloka.catalog.inventory.aspect;

import com.cena.traveloka.catalog.inventory.metrics.InventoryMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsAspect {

    private final InventoryMetrics metrics;

    @Pointcut("execution(* com.cena.traveloka.catalog.inventory.service.*.find*(..))")
    public void searchMethods() {}

    @Around("searchMethods()")
    public Object measureSearchTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            metrics.recordSearchTime(duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            metrics.recordSearchTime(duration);
            throw e;
        }
    }

    @Around("execution(* com.cena.traveloka.catalog.inventory.service.*.create*(..))")
    public Object measureCreateOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        try {
            Object result = joinPoint.proceed();

            // Record specific creation metrics
            if (className.contains("Partner") && methodName.contains("create")) {
                metrics.incrementPartnerCreated();
            } else if (className.contains("Property") && methodName.contains("create")) {
                metrics.incrementPropertyCreated();
            }

            return result;
        } catch (Exception e) {
            metrics.incrementError(className + "." + methodName);
            throw e;
        }
    }
}