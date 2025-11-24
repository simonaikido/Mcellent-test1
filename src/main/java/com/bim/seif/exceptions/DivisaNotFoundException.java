package com.bim.seif.exceptions;

public class DivisaNotFoundException extends RuntimeException {
    public DivisaNotFoundException(String clave) {
        super("No se encontró la divisa con la clave: " + clave);
    }
}