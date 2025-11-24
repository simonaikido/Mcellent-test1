package com.bim.seif.models.mappers;

import com.bim.seif.models.Instruccion;
import com.bim.seif.models.InstruccionJuridica;
import com.bim.seif.models.InstruccionMonetaria;
import com.bim.seif.models.dto.InstruccionDto;
import com.bim.seif.models.dto.InstruccionJuridicaDto;
import com.bim.seif.models.dto.InstruccionMonetariaDto;
import com.bim.seif.models.dto.InstruccionMonetariaResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;

import java.util.List;

@Mapper(
    componentModel = "spring",
    uses = { OperacionMonetariaMapper.class, OperacionJuridicaMapper.class },
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface InstruccionMapper {

    InstruccionDto instruccionToInstruccionDTO(Instruccion instruccion);

    Instruccion toEntity(InstruccionDto dto);

    @Mapping(target = "estatus", source = "estatus.cve")
    InstruccionMonetariaDto instruccionMonetariaToInstruccionMonetariaDTO(InstruccionMonetaria instruccion);

    List<InstruccionDto> instruccionToInstruccionDTOList(List<Instruccion> instrucciones);

    List<InstruccionMonetariaDto> instruccionMonetariaToInstruccionMonetariaDTOList(List<InstruccionMonetaria> instrucciones);

    @Mapping(target = "estatus", source = "estatus.cve")
    @Mapping(target = "fechaRechazo", source = "instruccion.fechaRechazo")
    InstruccionMonetariaResponseDto toDTO(InstruccionMonetaria instrucciones);

    List<InstruccionMonetariaResponseDto> toDTO(List<InstruccionMonetaria> instrucciones);

    InstruccionJuridicaDto instruccionJuridicaToInstruccionJuridicaDTO(InstruccionJuridica instruccion);

    //CORREGIDO: la lista debe recibir entidades, no DTOs
    List<InstruccionJuridicaDto> instruccionJuridicaToInstruccionJuridicaDTOList(List<InstruccionJuridica> instrucciones);

    // Si realmente necesitas una segunda variante, mantenla; si no, elimínala para evitar ambigüedades:
    List<InstruccionJuridicaDto> instruccionJuridicaToInstruccionJuridicaDTOListTwo(List<InstruccionJuridica> instrucciones);
}