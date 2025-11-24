package com.bim.seif.config;

import org.slf4j.MDC;

public class TraceConfig {

    public static void initContext(String transactionId, String userId) {
        MDC.put("traceId", transactionId);
        MDC.put("userId", userId);
    }

    public static void clearCOntext() {
        MDC.clear();
    }

}