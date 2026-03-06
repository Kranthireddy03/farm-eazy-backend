package com.farmeazy.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * LOGGING ASPECT - CENTRALIZED API LOGGING
 * 
 * PURPOSE: Provides entry/exit logging for all controllers and services
 * without cluttering individual classes with logging code.
 * 
 * LOGGED INFO:
 * - API_ENTER: Method name, entry timestamp
 * - API_EXIT: Method name, execution time, success/failure
 * - SERVICE_ENTER/EXIT: Service method entry/exit
 * 
 * SENSITIVE DATA:
 * - No parameters logged (may contain passwords, tokens)
 * - No response body logged (may contain user data)
 * - Only method names and timing logged
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Pointcut for all controller methods
     */
    @Pointcut("within(com.farmeazy.controller..*)")
    public void controllerMethods() {}

    /**
     * Pointcut for all service methods
     */
    @Pointcut("within(com.farmeazy.service..*)")
    public void serviceMethods() {}

    /**
     * Log all controller method calls (API requests)
     */
    @Around("controllerMethods()")
    public Object logControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        log.info("API_ENTER: {}.{}", className, methodName);
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("API_EXIT: {}.{} ({}ms) SUCCESS", className, methodName, duration);
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("API_EXIT: {}.{} ({}ms) FAILED: {}", className, methodName, duration, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Log all service method calls
     */
    @Around("serviceMethods()")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        // Skip logging for internal/utility methods to reduce noise
        if (isInternalMethod(methodName)) {
            return joinPoint.proceed();
        }
        
        log.debug("SVC_ENTER: {}.{}", className, methodName);
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.debug("SVC_EXIT: {}.{} ({}ms) SUCCESS", className, methodName, duration);
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("SVC_EXIT: {}.{} ({}ms) FAILED: {}", className, methodName, duration, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Check if method is internal/utility method that shouldn't be logged
     */
    private boolean isInternalMethod(String methodName) {
        return methodName.startsWith("get") || 
               methodName.startsWith("set") || 
               methodName.startsWith("is") ||
               methodName.equals("toString") ||
               methodName.equals("hashCode") ||
               methodName.equals("equals") ||
               methodName.startsWith("lambda") ||
               methodName.contains("$");
    }
}
