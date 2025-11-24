package com.bim.seif.models.dto;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import lombok.Data;

@Data
public class EmpleadoDto implements Serializable {

	@NotBlank(message = "El UID no puede estar vacío")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El UID contiene caracteres no permitidos")
    private String uid;
    private String email;
    private String nombre;
    private String sn;
    private String password;
    private List<RegionDto> regiones;
    private RolDto rol;
    private String numeroEmpleado;
    private String celular;
    private String extension;
    private String area;
    private Boolean activo;
    private String fechaCreacion;
    private String auditNote;

    // === Nuevos campos para cifrado frontend ===
    /** Texto cifrado del password en base64 (AES-GCM) */
    private String planoEnc;

    /** IV usado en AES-GCM (base64) */
    private String ivEnc;

    /** Token efímero JWT (contiene el nonce HKDF) */
    private String token;
}