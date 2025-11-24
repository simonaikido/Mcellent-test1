package com.bim.seif.services;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("execution(* com.bim.seif.controllers..*(..))")
    public void serviciosPointcut() {}

    @Around("serviciosPointcut()")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();

        // ---- ENTRADA
        Object[] args = pjp.getArgs();
        String safeArgs = buildArgsSummary(args);
        log.debug("Entrando a: {} con args: {}", method, safeArgs);

        try {
            Object result = pjp.proceed();

            // ---- SALIDA (sin toString profundo)
            String resultSummary = summarizeResult(result);
            log.debug("Saliendo de: {} con resultado: {}", method, resultSummary);

            return result;
        } catch (Throwable ex) {
            // No imprimimos los argumentos/resultado completos para evitar recursión.
            log.error("Excepción en: {} - {}", method, ex.getMessage(), ex);
            throw ex;
        }
    }

    // -------------------- Helpers de impresión segura --------------------

    private String buildArgsSummary(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(safePrint(args[i]));
        }
        return sb.append("]").toString();
    }

    private String summarizeResult(Object result) {
        if (result == null) return "null";
        if (result instanceof Collection<?> c) {
            return "Collection(size=" + c.size() + ", type=" + result.getClass().getSimpleName() + ")";
        }
        if (result.getClass().isArray()) {
            return "Array(len=" + Array.getLength(result) + ", type=" + result.getClass().getComponentType().getSimpleName() + ")";
        }
        if (result instanceof Map<?,?> m) {
            return "Map(size=" + m.size() + ", type=" + result.getClass().getSimpleName() + ")";
        }
        // Para objetos propios, no llamamos a toString():
        Package p = result.getClass().getPackage();
        if (p != null && p.getName().startsWith("com.bim.seif")) {
            return result.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(result));
        }
        // Tipos simples
        return String.valueOf(result);
    }

    private String safePrint(Object o) {
        if (o == null) return "null";
        if (o instanceof CharSequence) return "\"" + o + "\"";
        if (o instanceof Number || o instanceof Boolean) return String.valueOf(o);

        if (o instanceof Collection<?> c) return "Collection(size=" + c.size() + ")";
        if (o.getClass().isArray()) return "Array(len=" + Array.getLength(o) + ")";
        if (o instanceof Map<?,?> m) return "Map(size=" + m.size() + ")";

        Package p = o.getClass().getPackage();
        if (p != null && p.getName().startsWith("com.bim.seif")) {
            // Evitar toString en entidades/DTOs propios
            return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
        }
        // Tipos de JDK seguros
        return o.getClass().getSimpleName();
    }
}