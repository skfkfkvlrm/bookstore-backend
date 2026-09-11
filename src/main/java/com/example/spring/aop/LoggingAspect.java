package com.example.spring.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("execution(* com.example.spring.presentation.controller..*(..))")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        return logWithTiming("CONTROLLER", joinPoint);
    }

    @Around("execution(* com.example.spring.application.service..*(..))"
            + " && !execution(* com.example.spring.application.service.Slf4jLoggingService.*(..))")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        return logWithTiming("SERVICE", joinPoint);
    }

    private Object logWithTiming(String layer, ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();

        log.info("[{}] {}.{} start args={}", layer, className, methodName,
                formatParameters(parameterNames, joinPoint.getArgs()));

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            log.info("[{}] {}.{} done {}ms", layer, className, methodName, executionTime);
            return result;
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[{}] {}.{} fail {}ms error={}",
                    layer, className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    private String formatParameters(String[] parameterNames, Object[] args) {
        if (args == null || args.length == 0 || parameterNames == null || parameterNames.length == 0) {
            return "{}";
        }

        List<String> chunks = new ArrayList<>();
        int size = Math.min(parameterNames.length, args.length);
        for (int i = 0; i < size; i++) {
            String name = parameterNames[i];
            Object value = args[i];
            if (isIgnoredType(value)) {
                continue;
            }
            chunks.add(name + "=" + sanitize(name, value));
        }

        if (chunks.isEmpty()) {
            return "{}";
        }
        return "{" + String.join(", ", chunks) + "}";
    }

    private boolean isIgnoredType(Object value) {
        if (value == null) {
            return false;
        }
        String typeName = value.getClass().getName();
        return typeName.startsWith("jakarta.servlet.")
                || typeName.startsWith("org.springframework.validation.")
                || typeName.startsWith("org.springframework.web.context.request.");
    }

    private static final java.util.regex.Pattern SENSITIVE_KEY_PATTERN = java.util.regex.Pattern.compile(
            "(?i)\\b(password|passwd|pwd|token|secret|authorization|cardnumber|cvc|accountnumber)\\s*=\\s*['\"]?([^,)\"'\\s]+)['\"]?"
    );

    private static final java.util.regex.Pattern PHONE_PATTERN = java.util.regex.Pattern.compile(
            "\\b(01[016789])[-.]?(\\d{3,4})[-.]?(\\d{4})\\b"
    );

    private static final java.util.regex.Pattern RRN_PATTERN = java.util.regex.Pattern.compile(
            "\\b(\\d{6})[-.]?[1-4]\\d{6}\\b"
    );

    private String sanitize(String fieldName, Object value) {
        if (value == null) {
            return "null";
        }

        String key = fieldName == null ? "" : fieldName.toLowerCase(Locale.ROOT);
        if (key.contains("password") || key.contains("token") || key.contains("secret")
                || key.contains("authorization") || key.contains("cardnumber")) {
            return "\"***\"";
        }

        String strValue = (value instanceof CharSequence s) ? "\"" + s + "\"" : String.valueOf(value);
        String masked = maskSensitiveData(strValue);
        return truncate(masked);
    }

    private String maskSensitiveData(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        // 1. DTO 내부 민감 키워드(비밀번호, 카드번호, 토큰 등) 정규식 마스킹
        String result = SENSITIVE_KEY_PATTERN.matcher(input).replaceAll("$1=***");
        // 2. 전화번호 정규식 마스킹 (010-****-5678)
        result = PHONE_PATTERN.matcher(result).replaceAll("$1-****-$3");
        // 3. 주민등록번호 정규식 마스킹
        result = RRN_PATTERN.matcher(result).replaceAll("$1-*******");
        return result;
    }

    private String truncate(String value) {
        int max = 500;
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }
}