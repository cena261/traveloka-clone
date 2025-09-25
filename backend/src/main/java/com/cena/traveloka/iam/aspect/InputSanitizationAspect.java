package com.cena.traveloka.iam.aspect;

import com.cena.traveloka.iam.service.InputSanitizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Aspect for automatic input sanitization
 *
 * Provides automatic sanitization of:
 * - Controller method parameters
 * - Service method inputs
 * - DTO field values
 * - Request body objects
 *
 * Key Features:
 * - Automatic detection of input parameters
 * - Selective sanitization based on annotations
 * - Performance-optimized field access
 * - Comprehensive logging
 * - Exception handling for reflection operations
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class InputSanitizationAspect {

    private final InputSanitizationService sanitizationService;

    /**
     * Sanitize inputs for all controller methods
     */
    @Around("execution(* com.cena.traveloka.iam.controller..*(..))")
    public Object sanitizeControllerInputs(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                args[i] = sanitizeObject(args[i]);
            }
        }

        return joinPoint.proceed(args);
    }

    /**
     * Sanitize inputs for service methods that handle user data
     */
    @Around("execution(* com.cena.traveloka.iam.service..*(..)) && " +
            "(execution(* *..create*(..)) || execution(* *..update*(..)) || execution(* *..save*(..)))")
    public Object sanitizeServiceInputs(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                args[i] = sanitizeObject(args[i]);
            }
        }

        return joinPoint.proceed(args);
    }

    /**
     * Sanitize an object based on its type
     */
    private Object sanitizeObject(Object obj) {
        if (obj == null) {
            return null;
        }

        // Handle primitive types and wrappers
        if (obj instanceof String) {
            return sanitizationService.sanitizeText((String) obj);
        }

        // Skip sanitization for certain types
        if (isSkippableType(obj.getClass())) {
            return obj;
        }

        // Handle DTOs and request objects
        if (isUserInputObject(obj)) {
            return sanitizeUserInputObject(obj);
        }

        return obj;
    }

    /**
     * Check if an object represents user input that should be sanitized
     */
    private boolean isUserInputObject(Object obj) {
        String className = obj.getClass().getName();

        // Sanitize DTO objects
        return className.contains(".dto.request.") ||
               className.contains(".dto.") ||
               className.endsWith("Request") ||
               className.endsWith("DTO") ||
               className.endsWith("Form");
    }

    /**
     * Check if a type should be skipped during sanitization
     */
    private boolean isSkippableType(Class<?> clazz) {
        return clazz.isPrimitive() ||
               clazz.isEnum() ||
               Number.class.isAssignableFrom(clazz) ||
               Boolean.class.isAssignableFrom(clazz) ||
               java.time.temporal.Temporal.class.isAssignableFrom(clazz) ||
               java.util.Date.class.isAssignableFrom(clazz) ||
               clazz.getPackage() != null &&
               (clazz.getPackage().getName().startsWith("java.") ||
                clazz.getPackage().getName().startsWith("javax.") ||
                clazz.getPackage().getName().startsWith("org.springframework."));
    }

    /**
     * Sanitize all String fields in a user input object
     */
    private Object sanitizeUserInputObject(Object obj) {
        try {
            Class<?> clazz = obj.getClass();
            Field[] fields = getAllFields(clazz);

            for (Field field : fields) {
                if (field.getType() == String.class) {
                    sanitizeStringField(obj, field);
                } else if (field.getType().getName().contains("com.cena.traveloka")) {
                    // Recursively sanitize nested objects
                    field.setAccessible(true);
                    Object nestedObj = field.get(obj);
                    if (nestedObj != null && isUserInputObject(nestedObj)) {
                        Object sanitizedNested = sanitizeUserInputObject(nestedObj);
                        field.set(obj, sanitizedNested);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error during object sanitization for class: {}", obj.getClass().getName(), e);
        }

        return obj;
    }

    /**
     * Sanitize a specific String field
     */
    private void sanitizeStringField(Object obj, Field field) throws IllegalAccessException {
        field.setAccessible(true);
        String value = (String) field.get(obj);

        if (StringUtils.hasText(value)) {
            String sanitized = getSanitizedValue(field, value);

            // Only update if value changed
            if (!value.equals(sanitized)) {
                field.set(obj, sanitized);
                log.debug("Sanitized field '{}' in class '{}'",
                         field.getName(), obj.getClass().getSimpleName());
            }
        }
    }

    /**
     * Get sanitized value based on field name and context
     */
    private String getSanitizedValue(Field field, String value) {
        String fieldName = field.getName().toLowerCase();

        // Apply specific sanitization based on field name
        if (fieldName.contains("email")) {
            return sanitizationService.sanitizeEmail(value);
        } else if (fieldName.contains("phone") || fieldName.contains("mobile")) {
            return sanitizationService.sanitizePhoneNumber(value);
        } else if (fieldName.contains("url") || fieldName.contains("link")) {
            return sanitizationService.sanitizeUrl(value);
        } else if (fieldName.contains("name") || fieldName.contains("firstname") || fieldName.contains("lastname")) {
            return sanitizationService.sanitizeName(value);
        } else if (fieldName.contains("search") || fieldName.contains("query")) {
            return sanitizationService.sanitizeSearchQuery(value);
        } else {
            return sanitizationService.sanitizeText(value);
        }
    }

    /**
     * Get all fields including inherited ones
     */
    private Field[] getAllFields(Class<?> clazz) {
        java.util.List<Field> fields = new java.util.ArrayList<>();

        // Get fields from current class and all superclasses
        Class<?> currentClass = clazz;
        while (currentClass != null && !currentClass.equals(Object.class)) {
            fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }

        return fields.toArray(new Field[0]);
    }
}