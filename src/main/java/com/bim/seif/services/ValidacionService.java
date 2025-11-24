package com.bim.seif.services;

import java.util.*;
import java.util.stream.Collectors;

import com.bim.seif.models.*;
import com.bim.seif.repositories.*;
import org.springframework.stereotype.Service;

import com.bim.seif.models.dto.EmpleadoDto;
import com.bim.seif.models.dto.TipoOperacionMonetariaDto;
import com.bim.seif.models.mappers.TipoOperacionMonetariaMapper;

import lombok.RequiredArgsConstructor;

import javax.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class ValidacionService {
	
	private final TipoOperacionMonetariaRepository tipoOperacionMonetariaRepository;

    private final EmpleadoService empleadoService; 
	
	private final RevisionOperacionMonetariaRepository revisionRepository;

	private final NivelAprobacionRepository nivelAprobacionRepository;

	private Map<Rol,Long> niveles;

	@PostConstruct
	private void cargarNivelesAprovacion(){
		Map<Rol,Long> nivelesRevision = new HashMap<>();
		for (ConfiguracionNivelAprobacionMonto n:nivelAprobacionRepository.findAll()){
			nivelesRevision.put(n.getRol(),n.getMonto());
		}
		this.niveles = nivelesRevision;
	}

	public Map<Rol,Long> obtenerNivelesAprobacion(){
		return this.niveles;
	}

	public void asignarValidadoresGenerales(List<OperacionMonetaria> operacionesPersistidas, String regionCve) {


		String validadorEmail = buscarValidadorGeneral(regionCve, Rol.ROLE_EA);

		Map<String, Boolean> configuracionValidaciones = obtenerTiposOperacionMonetaria().stream()
			    .collect(Collectors.toMap(TipoOperacionMonetariaDto::getCve, TipoOperacionMonetariaDto::isValidacionGeneral));

		RevisionOperacionMonetaria revisionOperacionMonetaria;
		List<RevisionOperacionMonetaria> revisiones = new ArrayList<>();



		 for(OperacionMonetaria om : operacionesPersistidas) {

			if(configuracionValidaciones.get(om.getTipoOperacion().getCve())) {
				revisionOperacionMonetaria = new RevisionOperacionMonetaria();
				revisionOperacionMonetaria.setOperacionMonetaria(om);
				revisionOperacionMonetaria.setValidadorEmail(validadorEmail);
				revisiones.add(revisionOperacionMonetaria);
			}
	     }
		     revisionRepository.saveAllAndFlush(revisiones);
	}

	
    private List<TipoOperacionMonetariaDto> obtenerTiposOperacionMonetaria(){
        List<TipoOperacionMonetaria> tipos = tipoOperacionMonetariaRepository.findAll();
        return TipoOperacionMonetariaMapper.INSTANCE.toDto(tipos);
    }


	public String buscarValidador(Rol rolActual, String regionCve, boolean validacionGeneral, boolean validacionMontos, Long montoMaximo){
		String aprobador = null;
		if (validacionGeneral && Rol.ROLE_GA.equals(rolActual)) {
			aprobador = buscarValidadorGeneral(
					regionCve
					, Rol.ROLE_GA);
		} else if (validacionMontos) {
			aprobador = buscarValidadorMontos (
					regionCve
					, rolActual,
					montoMaximo
			);
		}
		return aprobador;
	}


	public String buscarValidadorGeneral(String regionCve, Rol rolActual){
		String validadorEmail = null;
		Rol rolSearch = Rol.ROLE_GA;
		if(rolActual.equals(Rol.ROLE_EA)){
			rolSearch = Rol.ROLE_GA;
		} else if(rolActual.equals(Rol.ROLE_GA)){
			rolSearch = Rol.ROLE_GL;
		}

		List<EmpleadoDto> validadores = empleadoService.buscarEmpleados(rolSearch.name(), regionCve);
		// FIX criterio para seleccionar validador
		if (!validadores.isEmpty()) {
			validadorEmail = validadores.get(0).getUid();
		}
		return validadorEmail;
	}

	public String buscarValidadorMontos(String regionCve, Rol rolActual, Long monto){
		String validadorEmail = null;

		List<EmpleadoDto> validadores = new ArrayList<>();

		if((rolActual.equals(Rol.ROLE_GA) || rolActual.equals(Rol.ROLE_GL))
				&& (monto >= obtenerNivelesAprobacion().get(Rol.ROLE_SDF))){
			validadores = empleadoService.buscarEmpleados(Rol.ROLE_SDF.name(), regionCve);
		} else if(rolActual.equals(Rol.ROLE_SDF)
				&& (monto >= obtenerNivelesAprobacion().get(Rol.ROLE_DF))){
			validadores = empleadoService.buscarEmpleados(Rol.ROLE_DF.name(), regionCve);
		} else if(rolActual.equals(Rol.ROLE_DC)
				&& (monto >= obtenerNivelesAprobacion().get(Rol.ROLE_DC))){
			validadores = empleadoService.buscarEmpleados(Rol.ROLE_DC.name(), regionCve);
		} else if(rolActual.equals(Rol.ROLE_DF)
				&& (monto >= obtenerNivelesAprobacion().get(Rol.ROLE_DG))){
			validadores = empleadoService.buscarEmpleados(Rol.ROLE_DG.name(), regionCve);
		}
		// FIX criterio para seleccionar validador
		if (!validadores.isEmpty()) {
			validadorEmail = validadores.get(0).getUid();
		}
		return validadorEmail;
	}
	
	public String buscarSiguienteValidadorGeneral(String validadorActual, String regionCve){
		String validadorEmail = null;
		String rol ="GL";
		
		List<EmpleadoDto> validadores = empleadoService.buscarEmpleados(rol, regionCve);
		// FIX criterio para seleccionar validador		
		if(!validadores.isEmpty()) {
			validadorEmail = validadores.get(0).getEmail();
		}
		return validadorEmail;
	}   
    

}
