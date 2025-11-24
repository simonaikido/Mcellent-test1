package com.bim.seif.models;

import java.util.HashSet;
import java.util.Set;

public enum TipoNotificacion {

    INST_INI("Instrucción recibida",Set.of(TipoPropiedad.fideicomiso))
    ,INS_PROC("Instrucción en proceso",Set.of(TipoPropiedad.fideicomiso, TipoPropiedad.instruccion))
    ,FID_BLOQ("Fideicomiso bloqueado",Set.of(TipoPropiedad.fideicomiso));

    private final Set<TipoPropiedad> variables ;
    private final String descripcion;
    TipoNotificacion(String descripcion, Set<TipoPropiedad> variables){
        this.variables = variables;
        this.descripcion = descripcion;
    }

    public Set<TipoPropiedad> getVariables() {
        if (this.variables == null)
        {
            return new HashSet<>();
        }
        return variables;
    }

    public String getDescripcion() {
        return descripcion;
    }

}