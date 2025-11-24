package com.bim.seif.models;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum ConfiguracionEvento {



    ;

    private final Map<String, Set<String>> variables ;
    private final TipoEvento tipoEvento;
    ConfiguracionEvento(Map<String, Set<String>> variables, TipoEvento tipoEvento){
        this.variables = variables;
        this.tipoEvento = tipoEvento;
    }

    public Map<String, Set<String>> getVariables() {
        if (this.variables == null)
        {
            return new HashMap<>();
        }
        return variables;
    }


    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }
}



