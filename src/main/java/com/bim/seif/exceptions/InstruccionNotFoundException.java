package com.bim.seif.exceptions;

public class InstruccionNotFoundException extends RuntimeException {
    public InstruccionNotFoundException(String folio) {
        super("No se encontró la instrucción con el folio: " + folio);
    }
}
