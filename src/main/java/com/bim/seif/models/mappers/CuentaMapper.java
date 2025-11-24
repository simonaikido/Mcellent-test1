package com.bim.seif.models.mappers;

import com.bim.seif.models.CuentaAbono;
import com.bim.seif.models.dto.CuentaAbonoDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface CuentaMapper {
    CuentaMapper INSTANCE = Mappers.getMapper(CuentaMapper.class);

    // Métodos para CuentaAbono
    @Mapping(source = "solicitudCuenta", target = "solicitudCuenta")
    CuentaAbonoDto toCuentaAbonoDto(CuentaAbono cuenta);

    List<CuentaAbonoDto> toCuentaAbonoDtoList(List<CuentaAbono> cuentasAbono); // Nombre cambiado

    CuentaAbono toCuentaAbonoEntity(CuentaAbonoDto cuentaAbono);

}
