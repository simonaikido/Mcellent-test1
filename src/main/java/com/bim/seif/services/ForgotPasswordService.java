package com.bim.seif.services;

import java.util.List;
import java.util.Map;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;
import com.bim.seif.models.dto.ForgotPasswordDTO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

@Slf4j
@Data
@Service
@RequiredArgsConstructor
public class ForgotPasswordService {

    private final LdapTemplate ldapTemplate;
    private final Environment env;
    private final AuthService authService;
    private final EmpleadoService empleadoService;

    public ResponseEntity<?> validateUsr(ForgotPasswordDTO request) {
        HttpStatus status = HttpStatus.OK;
        String msj = "";

        try {
            List<String> search = ldapTemplate.search("ou=empleados", "uid=" + request.getUsuario(),
                    (AttributesMapper<String>) attrs -> (String) attrs.get("uid").get());
            status = (search.size() > 0 ? HttpStatus.OK : HttpStatus.NOT_FOUND);
            msj = (search.size() > 0 ? "" : "Correo no registrado");

        } catch (Exception e) {
            log.error(e.getMessage());
            status = HttpStatus.BAD_REQUEST;
            msj = e.getMessage();
        }

        return ResponseEntity
                .status(status)
                .body(Map.of("mensaje", msj));

    }

    public ResponseEntity<?> otpSend(ForgotPasswordDTO request) {
        HttpStatus status = HttpStatus.OK;
        String msj = "OK";
        try {
            authService.generarOtp(request.getUsuario());
        } catch (Exception e) {
            log.error(e.getMessage());
            status = HttpStatus.BAD_REQUEST;
            msj = e.getMessage();
        }
        return ResponseEntity
                .status(status)
                .body(Map.of("code", msj));
    }

    public ResponseEntity<?> otpValidate(ForgotPasswordDTO request) {
        HttpStatus status = HttpStatus.OK;
        String msj = "Codigo correcto";

        try {
            if (!authService.validarOTP(request.getUsuario(), request.getOtp())) {
                status = HttpStatus.UNAUTHORIZED;
                msj = "Codigo Incorrecto";
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            status = HttpStatus.BAD_REQUEST;
            msj = e.getMessage();
        }

        return ResponseEntity
                .status(status)
                .body(Map.of("code", msj));
    }

    public ResponseEntity<?> updatePass(ForgotPasswordDTO request) {
        final String uid = norm(request.getUsuario());
        log.info("Actualizando contrasena para el usuario: {}", uid);

        if (uid.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "usuario requerido"));
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "password requerido"));
        }

        HttpStatus status = HttpStatus.OK;
        String msj = "updated password";

        try {
            // 1) Localiza el contexto por uid
            DirContextOperations ctx = ldapTemplate.searchForContext(query().where("uid").is(uid));

            // 2) Actualiza el password (se asume ya viene en SHA-256 HEX del front)
            ctx.setAttributeValue("userPassword", request.getPassword());
            ldapTemplate.modifyAttributes(ctx);

            // 3) Marcar primer login completado => employeeType=ACTIVE
            try {
                empleadoService.marcarPrimerLoginCompletado(uid);
            } catch (Exception e) {
                log.warn("Password cambiado pero no se pudo marcar employeeType=ACTIVE en {}: {}", uid, e.getMessage());
            }

            // 4) (Opcional) Intentar limpiar lock de ppolicy si existiera
            try {
                ModificationItem[] unlock = new ModificationItem[] {
                        new ModificationItem(DirContext.REMOVE_ATTRIBUTE,
                                new BasicAttribute("pwdAccountLockedTime"))
                };
                ldapTemplate.modifyAttributes(ctx.getDn(), unlock);
            } catch (Exception ignore) {
                // No todos los directorios lo soportan / atributo no existe: no-op
            }

        } catch (NameNotFoundException nf) {
            status = HttpStatus.NOT_FOUND;
            msj = "usuario no encontrado";
        } catch (Exception e) {
            status = HttpStatus.BAD_REQUEST;
            msj = e.getMessage();
            log.warn("updatePass: error al modificar password de {}: {}", uid, e.getMessage());
        }

        return ResponseEntity.status(status).body(Map.of("mensaje", msj));
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
