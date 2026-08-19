package com.m1kllz.ecommerce.notification.utility;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("within(com.m1kllz.ecommerce..service..*)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = joinPoint.getSignature().toShortString();
        long start = System.nanoTime();
        log.debug("→ {}", signature);
        try {
            Object result = joinPoint.proceed();
            log.debug("← {} ({} ms)", signature, elapsedMs(start));
            return result;
        } catch (Exception ex) {
            log.error("✖ {} failed after {} ms: {}", signature, elapsedMs(start), ex.getMessage());
            throw ex;
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
