package com.bim.seif.exceptions;

import lombok.Builder;

@Builder
public class Error {
    String message;
    String type;
    String code;
    String traceId;
}
