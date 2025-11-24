// package com.bim.seif.controllers;
package com.bim.seif.controllers;

import com.bim.seif.services.PasswordResetService;
import com.bim.seif.utils.Validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.naming.InvalidNameException;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@RestController
@RequestMapping("/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * Dispara el correo con la liga
     * 
     * @throws InvalidNameException
     */
    @PostMapping("/request")
    public ResponseEntity<Void> request(@RequestParam("uid") String uid,
            HttpServletRequest req) throws InvalidNameException {
       
        String ip = Optional.ofNullable(req.getHeader("X-Forwarded-For"))
                .filter(s -> !s.isBlank())
                .map(s -> s.split(",")[0].trim()) // solo la primera IP
                .orElse(req.getRemoteAddr());

        ip = HtmlUtils.htmlEscape(ip);
        
        String ua = req.getHeader("User-Agent");
        ua = ua != null ? HtmlUtils.htmlEscape(ua) : "unknown";

        String safeUid = Validators.sanitizarUid(uid);
        
        passwordResetService.enviarLigaReset(safeUid, ip, ua);
        return ResponseEntity.accepted().build();
    }

    /**
     * Si quieres también GET (porque lo pediste explícito)
     * 
     * @throws InvalidNameException
     */
    @GetMapping("/request")
    public ResponseEntity<Void> requestGet(@RequestParam("uid") String uid,
            HttpServletRequest req) throws InvalidNameException {
        return request(uid, req);
    }

    /** Verificar token para que el front sepa el usuario (opcional) */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(@RequestParam("token") String token) {
        String uid = passwordResetService.validarToken(token);
        return ResponseEntity.ok(Map.of("uid", uid));
    }

    /** Confirmar: aplicar nueva contraseña usando el token */
    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPwdHash = body.get("password"); // viene en SHA-256 desde tu front actual
        if (token == null || newPwdHash == null || newPwdHash.isBlank())
            return ResponseEntity.badRequest().build();

        passwordResetService.consumirTokenYCambiarPassword(token, newPwdHash);
        return ResponseEntity.ok().build();
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    // dentro de EmpleadoController
    @PostMapping("/{uid}/reset-password-email")
    public ResponseEntity<Void> enviarReset(@PathVariable String uid, HttpServletRequest req)
            throws InvalidNameException {
        
        String ip = Optional.ofNullable(req.getHeader("X-Forwarded-For"))
                .filter(s -> !s.isBlank())
                .map(s -> s.split(",")[0].trim()) // solo la primera IP
                .orElse(req.getRemoteAddr());

        ip = HtmlUtils.htmlEscape(ip);
        
        String ua = req.getHeader("User-Agent");
        ua = ua != null ? HtmlUtils.htmlEscape(ua) : "unknown";

        String safeUid = Validators.sanitizarUid(uid);
        passwordResetService.enviarLigaReset(safeUid, ip, ua); // inyecta PasswordResetService
        return ResponseEntity.accepted().build();
    }

}