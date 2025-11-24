package com.bim.seif.services;

import com.bim.seif.exceptions.FirstLoginRequiredException;
import com.bim.seif.exceptions.UsuarioBloqueadoException;
import com.bim.seif.models.Propiedad;
import com.bim.seif.models.TipoEvento;
import com.bim.seif.models.dto.AuthRequest;
import com.bim.seif.models.dto.AuthResponse;
import com.bim.seif.models.dto.EmpleadoDto;
import com.bim.seif.models.dto.UserRedisJSONModel;
import com.bim.seif.security.AccessTokenFactory;
import com.bim.seif.security.RefreshTokenService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.naming.InvalidNameException;
import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${auth.code.expiration}")
    private int AUTH_OTP_EXPIRATION;

    @Value("${auth.block.expiration}")
    private int AUTH_BLOCK_EXPIRATION;

    @Value("${auth.block.attemps}")
    private int AUTH_BLOCK_ATTEMPS;

    @Value("${security.oauth2.access-token.ttl-minutes:10}")
    private int accessTtlMinutes;

    private final String OTP_KEY = "OTP";
    private final String INTENTOS_KEY = "INTENTOS";

    private final EmpleadoService empleadoService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final EmailService emailService;

    private final AccessTokenFactory accessTokenFactory;
    private final RefreshTokenService refreshTokenService;

    private UserRedisJSONModel userRedisJSONModel;

    public ResponseEntity<?> autenticar(AuthRequest request)
            throws UsernameNotFoundException, InvalidNameException, UsuarioBloqueadoException {

        // 1) PASSWORD: si falla aquí, AuthenticationManager lanza
        // BadCredentialsException.
        // El conteo/bloqueo de password lo haces en enviarCodigoAutenticacion(),
        // perfecto.
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsuario(),
                        request.getPassword()));

        final String uid = request.getUsuario();
        final String otpNsKey = "OTP:" + uid; // namespace para separar intentos de OTP

        // 2) OTP: si ya está bloqueado por intentos previos de OTP, corta aquí
        if (getIntentosActuales(otpNsKey) >= getAUTH_BLOCK_ATTEMPS()) {
            throw new UsuarioBloqueadoException("Usuario Bloqueado (OTP)");
        }

        // 3) Validar OTP. Si es incorrecto, incrementa e informa intentos restantes o
        // bloquea.
        if (!validarOTP(uid, request.getOtp())) {
            Long intentos = agregarIntentoFallido(otpNsKey);
            int limite = getAUTH_BLOCK_ATTEMPS();

            if (intentos >= limite) {
                throw new UsuarioBloqueadoException("Usuario Bloqueado (OTP)");
            }

            int restantes = Math.max(limite - intentos.intValue(), 0);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "mensaje", "El OTP no es correcto",
                            "intentosRestantes", restantes));
        }

        // 4) OTP OK → limpia los intentos de OTP
        borrarIntentosFallidos(otpNsKey);

        // 5) Genera tokens como ya lo haces
        EmpleadoDto user = empleadoService.loadUserByUsername(uid);

        var jwt = accessTokenFactory.encodeAccessToken(auth.getName(), user, accessTtlMinutes);
        String refreshToken = refreshTokenService.createRefreshToken(auth.getName());

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .token(jwt.getTokenValue())
                        .refreshToken(refreshToken)
                        .tokenType("Bearer")
                        .expiresIn(Duration.ofMinutes(accessTtlMinutes).getSeconds())
                        .build());
    }

    public boolean validarOTP(String usuario, String otp) {
        boolean valido = compararOTP(usuario, otp);
        // Solo borra el OTP si fue correcto.
        if (valido) {
            borrarCodigoAutenticacion(usuario);
        }
        return valido;
    }

    private boolean compararOTP(String user, String otp) {
        Map<String, Object> data = otpService.getUserData(user);
        boolean equals = false;
        String targetOTP = (String) data.get(OTP_KEY);
        if (targetOTP != null && targetOTP.equalsIgnoreCase(otp)) {
            equals = true;
        }
        return equals;
    }

    public void enviarCodigoAutenticacion(AuthRequest request)
            throws Exception, UsuarioBloqueadoException {

        final String uid = request.getUsuario();

        // 1) Cargar datos del usuario
        EmpleadoDto emDTO = empleadoService.loadUserByUsername(uid);
        final boolean activo = Boolean.TRUE.equals(emDTO.getActivo());

        // 2) Validar credenciales primero (LDAP)
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(uid, request.getPassword()));
        } catch (Exception e) {
            agregarIntentoFallido(uid);
            throw new BadCredentialsException("Credenciales invalidas", e);
        }

        // 3) ¿Primer login? (employeeType=FIRST_LOGIN)
        boolean firstLogin = empleadoService.isFirstLogin(uid);
        if (firstLogin) {
            borrarIntentosFallidos(uid);
            throw new FirstLoginRequiredException("Requiere cambio de contraseña");
        }

        // 4) Validar estado activo / intentos
        if (!activo || agregarIntentoFallido(uid) >= getAUTH_BLOCK_ATTEMPS() + 1) {
            throw new UsuarioBloqueadoException("Usuario Bloqueado");
        }

        // 5) Flujo normal: limpiar intentos y generar OTP
        borrarIntentosFallidos(uid);
        generarOtp(uid);
    }

    public void generarOtp(String usuario) {
        String otp = generaNumeroAuntenticacion();
        log.info("Código aleatorio generado: {}", otp);
        guardarCodigoAutenticacion(usuario, otp);

        emailService.enviarCorreo(usuario,
                TipoEvento.seguridad_token,
                Map.of(Propiedad.otp, otp));
    }

    private void guardarCodigoAutenticacion(String key, String value) {
        otpService.setHashValue(key, OTP_KEY, value, AUTH_OTP_EXPIRATION, TimeUnit.MINUTES);
    }

    private Long agregarIntentoFallido(String key) {
        return otpService.incrementHashValue(key, INTENTOS_KEY, AUTH_BLOCK_EXPIRATION, TimeUnit.MINUTES);
    }

    private void borrarIntentosFallidos(String key) {
        otpService.deleteHashValue(key, INTENTOS_KEY);
    }

    private void borrarCodigoAutenticacion(String key) {
        otpService.deleteHashValue(key, OTP_KEY);
    }

    private String generaNumeroAuntenticacion() {
        Random random = new Random();
        StringBuilder codigo = new StringBuilder();
        for (int i = 0; i < 6; i++)
            codigo.append(random.nextInt(10));
        return codigo.toString();
    }

    private long getIntentosActuales(String key) {
        try {
            Map<String, Object> data = otpService.getUserData(key);
            Object v = (data != null) ? data.get(INTENTOS_KEY) : null;
            return (v == null) ? 0L : Long.parseLong(v.toString());
        } catch (Exception e) {
            // Si no se puede leer, asumimos 0 para no bloquear por error de lectura
            log.warn("No se pudieron leer intentos para key {}: {}", key, e.getMessage());
            return 0L;
        }
    }
}