package com.bim.seif.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(UsuarioBloqueadoException.class)
    public ResponseEntity<Map<String, String>> handleUsuarioBloqueado(UsuarioBloqueadoException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("code", "user_disabled");
        error.put("message", ex.getMessage());
        error.put("type", "authentication");
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    private Map<String, Object> apiError(int status, String code, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status);
        body.put("error", HttpStatus.valueOf(status).getReasonPhrase());
        body.put("code", code);
        body.put("message", message);
        body.put("path", path);
        return body;
    }

    @ExceptionHandler(DuplicateUidException.class)
    public ResponseEntity<Object> handleDuplicateUid(DuplicateUidException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(apiError(409, "UID_DUPLICADO", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler({ org.springframework.ldap.NameAlreadyBoundException.class,
            javax.naming.NameAlreadyBoundException.class })
    public ResponseEntity<Object> handleNameAlreadyBound(Exception ex, HttpServletRequest req) {
        // Por si en algún punto no convertimos a DuplicateUidException
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(apiError(409, "UID_DUPLICADO", "El UID ya existe en LDAP", req.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(apiError(400, "ARGUMENTO_INVALIDO", ex.getMessage(), req.getRequestURI()));
    }

    // (opcional) tu “catch-all” final — NUNCA retorne null.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex, HttpServletRequest req) {
        // Log detallado aquí, pero no lances nada desde el handler
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(apiError(500, "ERROR_INTERNO", "Ocurrió un error inesperado.", req.getRequestURI()));
    }
    
    @ExceptionHandler(InstruccionNotFoundException.class)
    public ResponseEntity<String> handleInstruccionNotFound(InstruccionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(DivisaNotFoundException.class)
    public ResponseEntity<String> handleDivisaNotFound(DivisaNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}