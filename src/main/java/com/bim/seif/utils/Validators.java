package com.bim.seif.utils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.bim.seif.models.dto.OperacionMonetariaDto;

public class Validators {

	public static ResponseEntity<List<OperacionMonetariaDto>> sanitizeUsername(String username) {
		try {
			if (username.contains("@")) {
				validaUsuarioTipoCorreo(username);
			} else {
				validaUsuario(username);
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Collections.emptyList());
		}
		
		return null;
	}

	public static void validaUsuarioTipoCorreo(String username) throws Exception {
		if (username == null || !username.matches("^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$")) {
			throw new SecurityException("Token inválido");
		}

	}

	public static void validaUsuario(String username) throws Exception {
		if (username == null || !username.matches("^[a-zA-Z0-9._-]{3,50}$")) {
			throw new SecurityException("Usuario del token inválido");
		}

	}
	
	public static  String sanitizeLdapInput(String input) {
	    if (input == null) return "";
	    return input.replaceAll("([\\\\()*&|=!<>~])", "\\\\$1");
	}
	
	public static List<String> camposPermitidos = List.of(
		    "id", "fechaRegistro", "fechaRevision", "fechaEcritura", "fechaMantenimiento",
		    "numeroEscritura", "secuenciaCartaComplemento", "clienteCorreoElectronico",
		    "descripcionOperacion", "comentario", "observaciones"
	);

	public static List<String> camposPermitidosOperacionesMonetarias = List.of(
		    "id", "concepto", "comentario", "observacion", "fechaRegistro", "fechaRevision", "aprobada", "omitida"
	);
	
	public static String escapeDnValue(String value) {
	    if (value == null) {
	        return "";
	    }

	    StringBuilder sb = new StringBuilder();

	    for (int i = 0; i < value.length(); i++) {
	        char c = value.charAt(i);
	        switch (c) {
	            case ',':
	            case '+':
	            case '"':
	            case '\\':
	            case '<':
	            case '>':
	            case ';':
	            case '=':
	            case '#':
	                sb.append('\\').append(c);
	                break;
	            default:
	                sb.append(c);
	        }
	    }

	    if (sb.length() > 0 && sb.charAt(0) == ' ') {
	        sb.insert(0, '\\');
	    }
	    if (sb.length() > 1 && sb.charAt(sb.length() - 1) == ' ') {
	        sb.insert(sb.length() - 1, '\\');
	    }

	    return sb.toString();
	}


	public static String sanitizeUid(String uid)throws Exception {
		if (uid == null || !uid.matches("^[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException("UID inválido");
        }
		
		return escapeDnValue(uid);
	}
	
	public static String sanitizarUid(String uid) {
	    if (uid == null) return "";
	    return uid.replaceAll("[^a-zA-Z0-9._-]", "");
	}
	
	public static String escapeLdapFilter(String input) {
	    if (input == null) {
	        return "";
	    }

	    StringBuilder sb = new StringBuilder();
	    for (int i = 0; i < input.length(); i++) {
	        char c = input.charAt(i);
	        switch (c) {
	            case '\\': sb.append("\\5c"); break;
	            case '*':  sb.append("\\2a"); break;
	            case '(' : sb.append("\\28"); break;
	            case ')' : sb.append("\\29"); break;
	            case '\0': sb.append("\\00"); break;
	            default:   sb.append(c);
	        }
	    }
	    return sb.toString();
	}
	
	public static String sanitizeHeader(String input) {
        if (input == null) return "";
     
        return input.replaceAll("[\\n\\r\\t<>\"'\\\\]", "");
    }
	
	public static Integer tamanioRazonable(Integer tamanio) {
		if (tamanio < 1 || tamanio > 1000) {
            return 15;
        }
		
		return tamanio;
	}
	
	public static Integer paginadoRazonable(Integer pagina) {
		if (pagina < 0) {
		    pagina = 0; 
		}
		
		if (pagina > 10_000) {
		    pagina = 10_000; 
		}
	     
	     return pagina;
	}
	
	public static void  validaFechaHasta(LocalDate hasta) {
		if (hasta == null || hasta.isAfter(LocalDate.now().plusYears(1))) {
		    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha 'hasta' inválida");
		}
	}
	
	public static void  validaFechaDesde(LocalDate desde) {
		if (desde == null || desde.isAfter(LocalDate.now())) {
		    throw new IllegalArgumentException("Fecha 'desde' inválida");
		}
	}
	

}
