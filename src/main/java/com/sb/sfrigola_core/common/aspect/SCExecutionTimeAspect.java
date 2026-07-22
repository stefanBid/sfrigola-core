package com.sb.sfrigola_core.common.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Measures execution time of every service-layer method (domains.*.service.impl.*)
 * and logs it as FAST / NORMAL / SLOW based on the configured thresholds.
 */
@Aspect
@Component
@Slf4j
public class SCExecutionTimeAspect {

    @Value("${sc.aspect.execution-time.fast-threshold-ms}")
    private long fastThresholdMs;

    @Value("${sc.aspect.execution-time.slow-threshold-ms}")
    private long slowThresholdMs;

    @Pointcut("execution(* com.sb.sfrigola_core.domains..service.impl..*.*(..))")
    public void serviceImplMethods() {
    }

    @Around("serviceImplMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedMs = System.currentTimeMillis() - start;
            String signature = joinPoint.getSignature().toShortString();

            if (elapsedMs < fastThresholdMs) {
                log.debug("[FAST] {} executed in {} ms", signature, elapsedMs);
            } else if (elapsedMs <= slowThresholdMs) {
                log.info("[NORMAL] {} executed in {} ms", signature, elapsedMs);
            } else {
                log.warn("[SLOW] {} executed in {} ms", signature, elapsedMs);
            }
        }
    }
}
