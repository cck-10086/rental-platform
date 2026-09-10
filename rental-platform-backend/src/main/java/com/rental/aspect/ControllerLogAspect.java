package com.rental.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 接口耗时日志切面：记录所有 Controller 方法的执行耗时与异常。
 */
@Aspect
@Component
public class ControllerLogAspect {

    private static final Logger log = LoggerFactory.getLogger(ControllerLogAspect.class);

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String target = joinPoint.getTarget().getClass().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.info("接口耗时: {} 参数数={} 耗时={}ms", target, joinPoint.getArgs().length,
                    System.currentTimeMillis() - start);
            return result;
        } catch (Throwable e) {
            log.error("接口异常: {} 耗时={}ms 异常={}", target,
                    System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
    }
}
