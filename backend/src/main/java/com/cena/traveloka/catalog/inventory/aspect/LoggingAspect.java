package com.cena.traveloka.catalog.inventory.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("execution(* com.cena.traveloka.catalog.inventory.service.*.*(..))")
    public void serviceLayer() {}

    @Pointcut("execution(* com.cena.traveloka.catalog.inventory.controller.*.*(..))")
    public void controllerLayer() {}

    @Around("serviceLayer()")
    public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        try {
            log.debug("Starting {}.{}", className, methodName);
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Completed {}.{} in {}ms", className, methodName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Error in {}.{} after {}ms: {}", className, methodName, duration, e.getMessage());
            throw e;
        }
    }

    @Before("controllerLayer()")
    public void logControllerEntry(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        log.info("API call: {}.{} with {} parameters",
                className, methodName, args != null ? args.length : 0);
    }

    @AfterReturning(pointcut = "controllerLayer()", returning = "result")
    public void logControllerSuccess(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.info("API success: {}.{}", className, methodName);
    }

    @AfterThrowing(pointcut = "controllerLayer()", throwing = "exception")
    public void logControllerError(JoinPoint joinPoint, Throwable exception) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.error("API error: {}.{} - {}", className, methodName, exception.getMessage());
    }
}