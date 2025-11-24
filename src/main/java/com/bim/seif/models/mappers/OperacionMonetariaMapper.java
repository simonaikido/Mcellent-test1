package com.bim.seif.models.mappers;

import com.bim.seif.models.*;
import com.bim.seif.models.dto.*;
import org.mapstruct.*;

import java.util.List;
import java.util.Optional;

@Mapper(
    componentModel = "spring",
    uses = { FideicomisoMapper.class},
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface OperacionMonetariaMapper {

    @Named("sinInstruccion")
    @Mapping(target = "instruccion.operaciones", ignore = true)
    @Mapping(target = "instruccion.estatus", source = "instruccion.estatus.cve")
    @Mapping(target = "instruccion.estatusDescripcion", source = "instruccion.estatus.descripcion")
    @Mapping(target = "monto", source = "campos", qualifiedByName = "mapMontoFromCampos")
    @Mapping(target = "referencia", source = "campos", qualifiedByName = "mapReferenciaFromCampos")
    @Mapping(target = "cuentaAbono", source = "campos", qualifiedByName = "mapCampoCuentaAbonoFromCampos")
    @Mapping(target = "cuentaCargo", source = "campos", qualifiedByName = "mapCampoCuentaCargoFromCampos")
    OperacionMonetariaDto toDtoSinInstruccion(OperacionMonetaria operacionMonetaria);

    @IterableMapping(qualifiedByName = "sinInstruccion")
    List<OperacionMonetariaDto> toDto(List<OperacionMonetaria> operacionMonetaria);

    @Mapping(target = "instruccion.estatus.cve", source = "instruccion.estatus")
    OperacionMonetaria toEntity(OperacionMonetariaDto operacionMonetariaDto);

    List<OperacionMonetaria> toEntity(List<OperacionMonetariaDto> operacionMonetariaDto);

    // --- Mapeos simples
    CampoMontoDto campoMontoToCampoMontoDto(CampoMonto campoMonto);
    CampoReferenciaDto campoReferenciaToCampoReferenciaDto(CampoReferencia campoReferencia);

    // ÚNICO CAMBIO: indicar que 'cuenta' (entity) va en 'cuentaAbono' (DTO)
    @Mapping(source = "cuenta", target = "cuentaAbono")
    CampoCuentaAbonoDto campoCuentaAbonoToCampoCuentaAbonoDto(CampoCuentaAbono campoCuentaAbono);

    CampoCuentaCargoDto campoCuentaCargoToCampoCuentaCargoDto(CampoCuentaCargo campoCuentaCargo);

    // --- Extractores desde la lista 'campos'
    @Named("mapMontoFromCampos")
    default CampoMontoDto mapMontoFromCampos(List<Campo> campos) {
        if (campos == null) return null;
        Optional<CampoMonto> montoCampo = campos.stream()
                .filter(CampoMonto.class::isInstance)
                .map(CampoMonto.class::cast)
                .findFirst();
        return montoCampo.map(this::campoMontoToCampoMontoDto).orElse(null);
    }

    @Named("mapReferenciaFromCampos")
    default CampoReferenciaDto mapReferenciaFromCampos(List<Campo> campos) {
        if (campos == null) return null;
        Optional<CampoReferencia> referenciaCampo = campos.stream()
                .filter(CampoReferencia.class::isInstance)
                .map(CampoReferencia.class::cast)
                .findFirst();
        return referenciaCampo.map(this::campoReferenciaToCampoReferenciaDto).orElse(null);
    }

    @Named("mapCampoCuentaAbonoFromCampos")
    default CampoCuentaAbonoDto mapCampoCuentaAbonoFromCampos(List<Campo> campos) {
        if (campos == null) return null;
        Optional<CampoCuentaAbono> campoCuentaAbono = campos.stream()
                .filter(CampoCuentaAbono.class::isInstance)
                .map(CampoCuentaAbono.class::cast)
                .findFirst();
        return campoCuentaAbono.map(this::campoCuentaAbonoToCampoCuentaAbonoDto).orElse(null);
    }

    @Named("mapCampoCuentaCargoFromCampos")
    default CampoCuentaCargoDto mapCampoCuentaCargoFromCampos(List<Campo> campos) {
        if (campos == null) return null;
        Optional<CampoCuentaCargo> campoCuentaCargo = campos.stream()
                .filter(CampoCuentaCargo.class::isInstance)
                .map(CampoCuentaCargo.class::cast)
                .findFirst();
        return campoCuentaCargo.map(this::campoCuentaCargoToCampoCuentaCargoDto).orElse(null);
    }
}