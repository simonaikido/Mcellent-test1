package com.bim.seif.models.mappers;

import com.bim.seif.models.Empleado;
import com.bim.seif.models.dto.EmpleadoDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface EmpleadoMapper {
    EmpleadoMapper INSTANCE = Mappers.getMapper(EmpleadoMapper.class);
    EmpleadoDto empleadoToEmpleadoDto(Empleado empleado);
    List<EmpleadoDto> empleadoToEmpleadoDtoList(List<Empleado> empleado);
}
