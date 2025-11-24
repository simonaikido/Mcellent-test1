package com.bim.seif.models;

import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ConfiguracionOperacionMonetariaId implements Serializable {

    private String tipoOperacionMonetariaCve;
    private String tipoCampoCve;

}
