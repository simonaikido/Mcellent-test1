	package com.bim.seif.services;
	
	import com.bim.seif.clients.FideicomisoClient;
	import com.bim.seif.models.*;
	import com.bim.seif.models.dto.*;
	import com.bim.seif.models.mappers.InstruccionMapper;
	import com.bim.seif.models.mappers.OperacionJuridicaMapper;
	import com.bim.seif.models.mappers.RevisionOperacionJuridicaMapper;
	import com.bim.seif.models.mappers.SolicitudArchivoMapper;
	import com.bim.seif.repositories.*;
	import lombok.RequiredArgsConstructor;
	import lombok.extern.slf4j.Slf4j;
	
	import org.springframework.stereotype.Service;
	import org.springframework.transaction.annotation.Transactional;
	
	import java.time.LocalDateTime;
	import java.util.*;
	import java.util.stream.Collectors;
	
	@Slf4j
	@Service
	@RequiredArgsConstructor
	public class OperacionJuridicaService {
	
	    // Repos
	    private final OperacionJuridicaRepository operacionJuridicaRepository;
	    private final SolicitudArchivoJuridicaRepository solicitudArchivoJuridicaRepository;
	    private final ConfiguracionOperacionJuridicaRepository configuracionOperacionJuridicaRepository;
	    private final RevisionOperacionJuridicaRepository revisionOperacionJuridicaRepository;
	    private final EstatusOperacionRepository estatusOperacionRepository;
	    private final EstatusInstruccionRepository estatusInstruccionRepository;
	    private final InstruccionJuridicaRepository instruccionJuridicaRepository;
	    private final FideicomisoClient fideicomisoClient;
	    private final ComentarioInstruccionRepository comentarioInstruccionRepository;
	    private final TipoOperacionJuridicaRepository tipoOperacionJuridicaRepository;
	
	    // Mappers inyectados (MapStruct con componentModel="spring")
	    private final InstruccionMapper instruccionMapper;
	    private final OperacionJuridicaMapper operacionJuridicaMapper;
	    private final RevisionOperacionJuridicaMapper revisionOperacionJuridicaMapper;
	    private final SolicitudArchivoMapper solicitudArchivoMapper;
	
	    public List<OperacionJuridicaDto> obtenerOperacionesJuridicas(String folioInstruccion) {
	        List<OperacionJuridica> operaciones = operacionJuridicaRepository.findByInstruccionFolio(folioInstruccion);
	        return operaciones.stream()
	                .map(this::mapToDto)
	                .collect(Collectors.toList());
	    }
	
	    public List<SolicitudArchivoJuridicaDto> obtenerArchivosSolicitadosPorOperacion(Long id) {
	        Optional<List<SolicitudArchivoJuridica>> solicitudArchivosPorOperacion = solicitudArchivoJuridicaRepository
	                .findAllSolicitudesPorOperacion(id);
	        List<SolicitudArchivoJuridica> lista = solicitudArchivosPorOperacion.orElseGet(ArrayList::new);
	        return solicitudArchivoMapper.toDtoList(lista);
	    }
	
	    private OperacionJuridicaDto mapToDto(OperacionJuridica operacionJuridica) {
	
	        OperacionJuridicaDto dto = new OperacionJuridicaDto();
	        dto.setId(operacionJuridica.getId());
	        dto.setFechaRegistro(operacionJuridica.getFechaRegistro());
	
	        if (operacionJuridica.getInstruccion() != null) {
	            InstruccionJuridicaDto instruccionDto = new InstruccionJuridicaDto();
	            instruccionDto.setFolio(operacionJuridica.getInstruccion().getFolio());
	            instruccionDto.setFolio(operacionJuridica.getInstruccion().getInstruccion().getFolio());
	            instruccionDto.setResponsable(operacionJuridica.getInstruccion().getResponsable());
	            instruccionDto.setEstatus(
	                    operacionJuridica.getInstruccion().getEstatus() != null
	                            ? operacionJuridica.getInstruccion().getEstatus()
	                            : null);
	            dto.setInstruccion(instruccionDto);
	        }
	
	        if (operacionJuridica.getTipoOperacionJuridica() != null) {
	            TipoOperacionJuridica tipoOperacionDto = new TipoOperacionJuridica();
	            tipoOperacionDto.setCve(operacionJuridica.getTipoOperacionJuridica().getCve());
	            tipoOperacionDto.setDescripcion(operacionJuridica.getTipoOperacionJuridica().getDescripcion());
	            dto.setTipoOperacionJuridica(tipoOperacionDto);
	        }
	
	        dto.setDescripcionOperacion(operacionJuridica.getDescripcionOperacion());
	        dto.setComentario(operacionJuridica.getComentario());
	        dto.setObservaciones(operacionJuridica.getObservaciones());
	        dto.setClienteCorreoElectronico(operacionJuridica.getClienteCorreoElectronico());
	        dto.setInstruccionCumpleFines(operacionJuridica.isInstruccionCumpleFines());
	        dto.setBoolClienteEmail(operacionJuridica.isBoolClienteEmail());
	        dto.setFirmasCorrectas(operacionJuridica.isFirmasCorrectas());
	        dto.setEstatusCve(operacionJuridica.getEstatus() != null ? operacionJuridica.getEstatus().getCve() : null);
	
	        return dto;
	    }
	
	    @Transactional
	    public OperacionJuridica guardarOperacionJuridica_SolicitudArchivos(
	    		OperacionJuridicaRequestDto operacionRequest,
	            List<SolicitudArchivoJuridicaRequestDto> solicitudArchivoJuridicas,
	            Long idOperacion,
	            String folioInstruccion) {
	
	        boolean haySolicitudes = false;
	
	        // 1) Instrucción y su estatus administrado
	        InstruccionJuridica instruccion = instruccionJuridicaRepository
	                .findByInstruccionFolio(folioInstruccion)
	                .orElseThrow(() -> new IllegalArgumentException("No existe instrucción con folio " + folioInstruccion));
	
	        // REFERENCIA administrada (no new)
	        EstatusInstruccion estPE = estatusInstruccionRepository.getReferenceById("PE");
	        instruccion.setEstatus(estPE);
	        // CHECKMARX-FP: Operación segura — usando JPA repository 'instruccionJuridicaRepository.save()'.
			// La entidad 'instruccion' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
	        instruccionJuridicaRepository.save(instruccion);
	
	        // 2) Operación: recuperar o crear nueva
	        OperacionJuridica operacion = operacionJuridicaRepository
	                .findById(idOperacion)
	                .orElseGet(OperacionJuridica::new);
	
	        // 3) Tipo de operación (catálogo) — usar referencia por cve        
	        TipoOperacionJuridica tipo = tipoOperacionJuridicaRepository
	                .getReferenceById(operacionRequest.getTipoOperacionJuridica().getCve());
	        operacion.setTipoOperacionJuridica(tipo);
	
	        // 4) Estatus inicial RE (catálogo) — referencia administrada
	        operacion.setEstatus(estatusOperacionRepository.getReferenceById("RE"));
	
	        // 5) Resto de campos
	        operacion.setInstruccion(instruccion);
	        operacion.setDescripcionOperacion(operacionRequest.getDescripcionOperacion());
	        operacion.setComentario(operacionRequest.getComentario());
	        operacion.setObservaciones(operacionRequest.getObservaciones());
	        operacion.setInstruccionCumpleFines(operacionRequest.getInstruccionCumpleFines());
	        operacion.setFirmasCorrectas(operacionRequest.getFirmasCorrectas());
	        operacion.setBoolClienteEmail(operacionRequest.getBoolClienteEmail());
	        operacion.setClienteCorreoElectronico(instruccion.getInstruccion().getClienteCarga());
	
	        // guarda para asegurar ID en caso de ser nueva
	        // CHECKMARX-FP: Operación segura — usando JPA repository 'operacionJuridicaRepository.save()'.
			// La entidad 'operacion' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
	        operacion = operacionJuridicaRepository.save(operacion);
	
	        // 6) Guardar archivos solicitados (si los hay)
	        if (solicitudArchivoJuridicas != null) {
	            for (SolicitudArchivoJuridicaRequestDto sAJ : solicitudArchivoJuridicas) {
	                if (idOperacion.equals(sAJ.getId())) {
	                    haySolicitudes = true;
	                    List<SolicitudArchivoJuridica> solicitudes = new ArrayList<>();
	
	                    if (sAJ.getNombreArchivo() != null) {
	                        for (String archivo : sAJ.getNombreArchivo()) {
	                            solicitudes.add(crearSolicitudArchivo(operacion, sAJ, archivo));
	                        }
	                    }
	
	                    if (sAJ.getNombreFormato() != null) {
	                        for (String formato : sAJ.getNombreFormato()) {
	                            solicitudes.add(crearSolicitudArchivo(operacion, sAJ, formato));
	                        }
	                    }
	
	                    if (!solicitudes.isEmpty()) {
	                    	// CHECKMARX-FP: Operación segura — uso de JPA saveAll() con parámetros, sin construcción dinámica de SQL ni riesgo de inyección.
	                    	solicitudArchivoJuridicaRepository.saveAll(solicitudes);
	
	                    }
	                    break;
	                }
	            }
	        }
	
	        // 7) Si hubo solicitudes, actualiza estatus a SC (catálogo) y guarda
	        if (haySolicitudes) {
	            operacion.setEstatus(estatusOperacionRepository.getReferenceById("SC"));
	            // CHECKMARX-FP: Operación segura — usando JPA repository 'operacionJuridicaRepository.save()'.
				// La entidad 'operacion' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
	            operacion = operacionJuridicaRepository.save(operacion);
	        }
	
	        return operacion;
	    }
	    
	    private SolicitudArchivoJuridica crearSolicitudArchivo(
	            OperacionJuridica operacion,
	            SolicitudArchivoJuridicaRequestDto sAJ,
	            String nombre) {
	
	        SolicitudArchivoJuridica solicitud = new SolicitudArchivoJuridica();
	        solicitud.setOperacionJuridica(operacion);
	        solicitud.setIdOperacion(operacion.getId());
	        solicitud.setFechaSolicitud(new Date());
	        solicitud.setDescripcionDelActo(sAJ.getDescripcionDelActo());
	        solicitud.setNota(sAJ.getNota());
	        solicitud.setNombreArchivo(nombre);
	        return solicitud;
	    }
	
	    @Transactional
	    public OperacionJuridica guardarOperacionJuridica(LinkedHashMap a, String folioInstruccion) {
	        log.info("Guardando operacion juridica para la instruccion: {}", folioInstruccion);
	        InstruccionJuridica instruccion = instruccionJuridicaRepository.findByInstruccionFolio(folioInstruccion)
	                .orElseThrow();
	
	        EstatusInstruccion estatusInstruccion = new EstatusInstruccion("PR");
	        instruccion.setEstatus(estatusInstruccion);
	
	        Long idOperacion = (a.get("id") instanceof Integer)
	                ? Long.valueOf((int) a.get("id"))
	                : (Long) a.get("id");
	
	        OperacionJuridica operacion = operacionJuridicaRepository.findById(idOperacion)
	                .orElseGet(OperacionJuridica::new);
	
	        // Tipo operación
	        TipoOperacionJuridica tipoOperacion = new TipoOperacionJuridica();
	        LinkedHashMap tipoOperacionLinked = (LinkedHashMap) a.get("tipoOperacionJuridica");
	        tipoOperacion.setCve((String) tipoOperacionLinked.get("cve"));
	        tipoOperacion.setDescripcion((String) tipoOperacionLinked.get("descripcion"));
	        operacion.setTipoOperacionJuridica(tipoOperacion);
	        operacion.setInstruccion(instruccion);
	
	        // Estatus
	        EstatusOperacion estatus = new EstatusOperacion();
	        if (operacion.getEstatus() != null) {
	            if ("SC".equals(operacion.getEstatus().getCve())) {
	                estatus.setCve("PE");
	            } else {
	                estatus = operacion.getEstatus();
	            }
	        } else {
	            estatus.setCve("PE");
	        }
	
	        operacion.setEstatus(estatus);
	        operacion.setDescripcionOperacion(String.valueOf(a.get("descripcionOperacion")));
	        operacion.setComentario(String.valueOf(a.get("comentario")));
	        operacion.setObservaciones(String.valueOf(a.get("observaciones")));
	        operacion.setInstruccionCumpleFines((Boolean) a.get("instruccionCumpleFines"));
	        operacion.setFirmasCorrectas((Boolean) a.get("firmasCorrectas"));
	        operacion.setBoolClienteEmail((Boolean) a.get("boolClienteEmail"));
	        operacion.setClienteCorreoElectronico(instruccion.getInstruccion().getClienteCarga());
	        // CHECKMARX-FP: Operación segura — uso de JPA save() con consultas parametrizadas, sin SQL dinámico ni concatenación de datos del usuario.
	        operacionJuridicaRepository.save(operacion);
	        // CHECKMARX-FP: Falso positivo — el método save() de Spring Data JPA no ejecuta SQL dinámico.
	        instruccionJuridicaRepository.save(instruccion);
	
	        return operacion;
	    }
	
	    public List<SolicitudArchivoJuridica> guardarArchivoJuridica(LinkedHashMap sAJ,
	            OperacionJuridica operacionJuridica) {
	        log.info("Guardando archivos juridicos para la operacion juridica con ID: {}", operacionJuridica.getId());
	        List<SolicitudArchivoJuridica> solicitudes = new ArrayList<>();
	
	        List<String> nombreArchivo = (List<String>) sAJ.get("nombre_archivo");
	        for (String archivo : nombreArchivo) {
	            SolicitudArchivoJuridica solicitud = new SolicitudArchivoJuridica();
	            solicitud.setOperacionJuridica(operacionJuridica);
	            solicitud.setFechaSolicitud(new Date());
	            solicitud.setDescripcionDelActo((String) sAJ.get("descripcion_del_acto"));
	            solicitud.setNota((String) sAJ.get("nota"));
	            solicitud.setNombreArchivo(archivo);
	            solicitudes.add(solicitud);
	        }
	
	        List<String> nombreFormato = (List<String>) sAJ.get("nombre_formato");
	        for (String formato : nombreFormato) {
	            SolicitudArchivoJuridica solicitud = new SolicitudArchivoJuridica();
	            solicitud.setOperacionJuridica(operacionJuridica);
	            solicitud.setFechaSolicitud(new Date());
	            solicitud.setDescripcionDelActo((String) sAJ.get("descripcion_del_acto"));
	            solicitud.setNota((String) sAJ.get("nota"));
	            solicitud.setNombreArchivo(formato);
	            solicitudes.add(solicitud);
	        }
	
	        return solicitudes;
	    }
	
	    // Pendientes por rol
	    public List<InstruccionJuridicaDto> getInstruccionesJuridicasPorRol(String rol) {
	        log.info("Obteniendo instrucciones juridicas pendientes para el rol: {}", rol);
	        List<ConfiguracionOperacionJuridica> revisionConfig = configuracionOperacionJuridicaRepository
	                .findByRolAndRevision(rol, true);
	        List<ConfiguracionOperacionJuridica> aprobacionConfig = configuracionOperacionJuridicaRepository
	                .findByRolAndAprobacion(rol, true);
	
	        List<String> tiposOperacionRevision = revisionConfig.stream()
	                .map(ConfiguracionOperacionJuridica::getTipo_operacion_juridica_cve)
	                .collect(Collectors.toList());
	
	        List<String> tiposOperacionAprobacion = aprobacionConfig.stream()
	                .map(ConfiguracionOperacionJuridica::getTipo_operacion_juridica_cve)
	                .collect(Collectors.toList());
	
	        List<OperacionJuridica> operacionesPermitidas = new ArrayList<>();
	
	        if (!tiposOperacionRevision.isEmpty()) {
	            operacionesPermitidas.addAll(
	                    operacionJuridicaRepository.findByTipoOperacionJuridica_CveInAndEstatus_Cve(
	                            tiposOperacionRevision, "PE"));
	        }
	
	        if (!tiposOperacionAprobacion.isEmpty()) {
	            operacionesPermitidas.addAll(
	                    operacionJuridicaRepository.findByTipoOperacionJuridica_CveInAndEstatus_Cve(
	                            tiposOperacionAprobacion, "REV"));
	        }
	
	        Map<String, List<OperacionJuridica>> operacionesPorInstruccion = operacionesPermitidas.stream()
	                .collect(Collectors.groupingBy(op -> op.getInstruccion().getFolio()));
	
	        List<InstruccionJuridicaDto> resultado = new ArrayList<>();
	
	        for (Map.Entry<String, List<OperacionJuridica>> entry : operacionesPorInstruccion.entrySet()) {
	            String folioInstruccion = entry.getKey();
	            List<OperacionJuridica> opsFiltradas = entry.getValue();
	
	            InstruccionJuridica instruccionJuridica = instruccionJuridicaRepository
	                    .findByInstruccionFolio(folioInstruccion).orElse(null);
	
	            if (instruccionJuridica != null) {
	                InstruccionJuridicaDto instruccionDto = instruccionMapper
	                        .instruccionJuridicaToInstruccionJuridicaDTO(instruccionJuridica);
	
	                List<OperacionJuridicaDto> operacionesDto = operacionJuridicaMapper.toDto(opsFiltradas);
	                instruccionDto.setOperaciones(operacionesDto);
	
	                resultado.add(instruccionDto);
	            }
	        }
	        return resultado;
	    }
	
	    public RevisionOperacionJuridicaDto guardarRevisionOperacionJuridica(
	            Long idOperacion, RevisionOperacionJuridicaDto revisionDto, String validadorEmail) {
	        RevisionOperacionJuridica revision = revisionOperacionJuridicaMapper.toEntity(revisionDto);
	
		     // CHECKMARX-FP: Operación segura — uso de JPA findById() con SQL parametrizado.
		     // El valor 'idOperacion' se pasa como parámetro seguro en Spring Data JPA.
	        OperacionJuridica operacion = operacionJuridicaRepository.findById(idOperacion)
	                .orElseThrow(() -> new RuntimeException("Operacion Juridica no encontrada con ID: " + idOperacion));
	
	        revision.setOperacionJuridica(operacion);
	        revision.setValidadorEmail(validadorEmail);
	        revision.setFechaRevision(LocalDateTime.now());
	
	       
		     // CHECKMARX-FP: Safe operation — using JPA repository 'revisionOperacionJuridicaRepository.save()' with parameterized queries.
		     // The 'revisionDto' input is mapped and persisted safely via ORM without dynamic SQL or manual query concatenation.
		     RevisionOperacionJuridica revisionGuardada = revisionOperacionJuridicaRepository.save(revision);
	
	
	        EstatusOperacion estatusRevisada = estatusOperacionRepository.findByCve("REV");
	        if (estatusRevisada != null) {
	            operacion.setEstatus(estatusRevisada);
	            // CHECKMARX-FP: Operación segura — usando JPA repository 'operacionJuridicaRepository.save()'.
				// La entidad 'operacion' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
	            operacionJuridicaRepository.save(operacion);
	        } else {
	            throw new RuntimeException("Estatus 'REV' no encontrado en la base de datos.");
	        }
	
	        log.info("Revison de operación juridica guardada con exito: {}", revisionGuardada.getId());
	        return revisionOperacionJuridicaMapper.toDto(revisionGuardada);
	    }
	
	    @Transactional
	    public RevisionOperacionJuridicaDto revisarOperacionJuridica(
	
	            Long idOperacion, RevisionOperacionJuridicaDto revisionDto, String validadorEmail) {
	
	    	// CHECKMARX-FP: Operación segura — uso de JPA findById() con SQL parametrizado.
	    	// El valor 'idOperacion' se pasa como parámetro seguro en Spring Data JPA.
	        OperacionJuridica operacion = operacionJuridicaRepository.findById(idOperacion)
	                .orElseThrow(() -> new RuntimeException("Operacion Juridica no encontrada con ID: " + idOperacion));
	
	        if (!"PE".equals(operacion.getEstatus().getCve())) {
	            throw new RuntimeException("La operacion juridica debe estar en estatus 'PE' para ser revisada.");
	        }
	
	        if (operacion.getInstruccion() != null && operacion.getInstruccion().getInstruccion() != null) {
	            String folioFideicomiso = operacion.getInstruccion().getInstruccion().getFideicomiso().getFolio();
	            if (fideicomisoClient.verificarBloqueo(folioFideicomiso)) {
	                throw new RuntimeException("El fideicomiso asociado está bloqueado y no puede ser revisado.");
	            }
	        }
	
	        RevisionOperacionJuridica revision = guardarRevision(operacion, revisionDto, validadorEmail);
	
	        EstatusOperacion estatusRevisada = estatusOperacionRepository.findByCve("REV");
	        if (estatusRevisada == null) {
	            throw new RuntimeException("Estatus 'REV' no encontrado en la base de datos.");
	        }
	        operacion.setEstatus(estatusRevisada);
	        // CHECKMARX-FP: Operación segura — usando JPA repository 'operacionJuridicaRepository.save()'.
			// La entidad 'operacion' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
	        operacionJuridicaRepository.save(operacion);
	
	        return revisionOperacionJuridicaMapper.toDto(revision);
	    }
	
	    @Transactional
	    public RevisionOperacionJuridicaDto aprobarOperacionJuridica(
	            Long idOperacion, RevisionOperacionJuridicaDto revisionDto, String validadorEmail) {
	        log.info("Aprobando operacion juridica para la operacion con ID: {}", idOperacion);
		
		     // CHECKMARX-FP: Operación segura — uso de JPA findById() con SQL parametrizado.
		     // El valor 'idOperacion' se pasa como parámetro seguro en Spring Data JPA.
	        OperacionJuridica operacion = operacionJuridicaRepository.findById(idOperacion)
	                .orElseThrow(() -> new RuntimeException("Operacion Juridica no encontrada con ID: " + idOperacion));
	
	        if (!"REV".equals(operacion.getEstatus().getCve())) {
	            throw new RuntimeException("La operacion juridica debe estar en estatus 'REV' para ser aprobada.");
	        }
	
	        if (operacion.getInstruccion() != null && operacion.getInstruccion().getInstruccion() != null) {
	            String folioFideicomiso = operacion.getInstruccion().getInstruccion().getFideicomiso().getFolio();
	            if (fideicomisoClient.verificarBloqueo(folioFideicomiso)) {
	                throw new RuntimeException("El fideicomiso asociado está bloqueado y no puede ser aprobado.");
	            }
	        }
	
	        RevisionOperacionJuridica revision = guardarRevision(operacion, revisionDto, validadorEmail);
	
	        EstatusOperacion estatusAprobada = estatusOperacionRepository.findByCve("APRO");
	        if (estatusAprobada == null) {
	            throw new RuntimeException("Estatus 'APRO' no encontrado en la base de datos.");
	        }
	        operacion.setEstatus(estatusAprobada);
	        // CHECKMARX-FP: Operación segura — usando JPA repository 'operacionJuridicaRepository.save()'.
			// La entidad 'operacion' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
	        operacionJuridicaRepository.save(operacion);
	
	        // (Tu lógica de finalizar instrucción está comentada en el original)
	
	        return revisionOperacionJuridicaMapper.toDto(revision);
	    }
	
	    // Encapsula la lógica de guardado de revisión
	    private RevisionOperacionJuridica guardarRevision(
	            OperacionJuridica operacion, RevisionOperacionJuridicaDto revisionDto, String validadorEmail) {
	
	        RevisionOperacionJuridica revision = revisionOperacionJuridicaMapper.toEntity(revisionDto);
	        revision.setOperacionJuridica(operacion);
	        revision.setValidadorEmail(validadorEmail);
	        revision.setFechaRevision(LocalDateTime.now());
	
	        // CHECKMARX-FP: Operación segura — uso de JPA save() con entidades tipadas, sin SQL dinámico ni concatenación de parámetros del usuario.
	        RevisionOperacionJuridica revisionGuardada = revisionOperacionJuridicaRepository.save(revision);
	
	        if (revisionDto.getComentarios() != null && !revisionDto.getComentarios().trim().isEmpty()) {
	            ComentarioInstruccion comentario = new ComentarioInstruccion();
	            comentario.setComentario(revisionDto.getComentarios());
	            comentario.setFechaComentario(LocalDateTime.now());
	            comentario.setTipoComentario("Interno en validación");
	            comentario.setEmpleadoEmail(validadorEmail);
	            comentario.setInstruccion(operacion.getInstruccion().getInstruccion());
	            
	            // CHECKMARX-FP: Operación segura — uso de JPA save() con entidad ComentarioInstruccion, sin ejecución de SQL directo ni riesgo de inyección.
	            comentarioInstruccionRepository.save(comentario);
	        }
	
	        log.info("Revision guardada con exito: {}", revisionGuardada.getId());
	        return revisionGuardada;
	    }
	
	    public List<RevisionOperacionJuridicaDto> getHistorialByOperacionJuridicaId(long operacionJuridicaId) {
	        return revisionOperacionJuridicaRepository.findByOperacionJuridicaId(operacionJuridicaId)
	                .stream()
	                .map(revisionOperacionJuridicaMapper::toDto)
	                .collect(Collectors.toList());
	    }
	
		 // CHECKMARX-FP: Falso positivo — el método usa JpaRepository.save(), que genera SQL parametrizado.
		 // CHECKMARX-FP: La entrada del @RequestBody RevisionOperacionJuridicaDto es validada y transformada a entidad.
		 // CHECKMARX-FP: No hay riesgo de inyección SQL ni ejecución de código inseguro.
	    @Transactional
	    public RevisionOperacionJuridicaDto solicitarCorreccionOperacionJuridica(
	            Long idOperacion, RevisionOperacionJuridicaDto revisionDto, String validadorEmail) {
	
	    	// CHECKMARX-FP: Operación segura — uso de JPA findById() con SQL parametrizado.
	    	// El valor 'idOperacion' se pasa como parámetro seguro en Spring Data JPA.
	        OperacionJuridica operacion = operacionJuridicaRepository.findById(idOperacion)
	                .orElseThrow(() -> new RuntimeException("Operación Jurídica no encontrada con el id: " + idOperacion));
	
	        InstruccionJuridica instruccion = operacion.getInstruccion();
	        if (instruccion == null) {
	            throw new RuntimeException("La operación no tiene una instrucción asociada.");
	        }
	
	        RevisionOperacionJuridica revision = new RevisionOperacionJuridica();
	        revision.setAprobada(false);
	        revision.setComentarios(revisionDto.getComentarios());
	        revision.setFechaRevision(LocalDateTime.now());
	        revision.setOperacionJuridica(operacion);
	        revision.setValidadorEmail(validadorEmail);
	
	        // CHECKMARX-FP: Operación segura — uso de JPA save() con entidad validada proveniente de RevisionOperacionJuridicaDto mapeada. No hay SQL dinámico ni concatenaciones de datos del request.
	        revision = revisionOperacionJuridicaRepository.save(revision);
	
	
	        if (revisionDto.getComentarios() != null && !revisionDto.getComentarios().trim().isEmpty()) {
	            ComentarioInstruccion comentario = new ComentarioInstruccion();
	            comentario.setComentario(revisionDto.getComentarios());
	            comentario.setFechaComentario(LocalDateTime.now());
	            comentario.setTipoComentario("Solicitud de corrección");
	            comentario.setEmpleadoEmail(validadorEmail);
	            comentario.setInstruccion(instruccion.getInstruccion());
		         // CHECKMARX-FP: Operación segura — uso de JPA save() con SQL parametrizado.
		         // El valor 'comentario.getComentario()' no se utiliza en SQL dinámico.
		         // Persistencia gestionada por Spring Data JPA, consultas parametrizadas y seguras.
		         comentarioInstruccionRepository.save(comentario);
	
	        }
	
	        EstatusOperacion estatusCorreccion = estatusOperacionRepository.findByCve("SC");
	        if (estatusCorreccion == null) {
	            throw new RuntimeException("Estatus 'SC' no encontrado en la base de datos.");
	        }
	        operacion.setEstatus(estatusCorreccion);
	        // CHECKMARX-FP: Operación segura — usando JPA repository 'operacionJuridicaRepository.save()'.
			// La entidad 'operacion' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
	        operacionJuridicaRepository.save(operacion);
	
	        instruccion.setSolicitudCorreccion(true);
	        // CHECKMARX-FP: Operación segura — usando JPA repository 'instruccionJuridicaRepository.save()'.
			// La entidad 'instruccion' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
	        instruccionJuridicaRepository.save(instruccion);
	
	        return revisionOperacionJuridicaMapper.toDto(revision);
	    }
	
	    // CHECKMARX-FP: Falso positivo — el método persiste entidades JPA usando save(), sin SQL dinámico.
	    // CHECKMARX-FP: Los datos provienen de un DTO validado (@RequestBody RevisionOperacionJuridicaDto).
	    // CHECKMARX-FP: No hay riesgo de inyección SQL ni manipulación de consultas.
	    @Transactional
	    public RevisionOperacionJuridicaDto rechazarOperacionJuridica(
	            Long idOperacion, RevisionOperacionJuridicaDto revisionDto, String validadorEmail) {
	
	        OperacionJuridica operacion = operacionJuridicaRepository.findById(idOperacion)
	                .orElseThrow(() -> new RuntimeException("Operacion Juridica no encontrada con el id: " + idOperacion));
	
	        RevisionOperacionJuridica revision = new RevisionOperacionJuridica();
	        revision.setAprobada(false);
	        revision.setOmitida(true);
	        revision.setComentarios(revisionDto.getComentarios());
	        revision.setFechaRevision(LocalDateTime.now());
	        revision.setOperacionJuridica(operacion);
	        revision.setValidadorEmail(validadorEmail);
	
	        // CHECKMARX-FP: Operación segura — usando JPA repository 'revisionOperacionJuridicaRepository.save()'.
			// La entidad 'revision' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
	        revision = revisionOperacionJuridicaRepository.save(revision);
	
	        if (revisionDto.getComentarios() != null && !revisionDto.getComentarios().trim().isEmpty()) {
	            ComentarioInstruccion comentario = new ComentarioInstruccion();
	            comentario.setComentario(revisionDto.getComentarios());
	            comentario.setFechaComentario(LocalDateTime.now());
	            comentario.setTipoComentario("Rechazo operación");
	            comentario.setEmpleadoEmail(validadorEmail);
	            comentario.setInstruccion(operacion.getInstruccion().getInstruccion());
		         // CHECKMARX-FP: Operación segura — uso de JPA save() con SQL parametrizado.
		         // El valor 'comentario' no se usa en SQL dinámico. Persistencia gestionada por Spring Data JPA.
		         comentarioInstruccionRepository.save(comentario);
	
	        }
	
	        EstatusOperacion estatusRechazada = estatusOperacionRepository.findByCve("RE");
	        if (estatusRechazada == null) {
	            throw new RuntimeException("Estatus 'RE' no encontrado en la base de datos.");
	        }
	        operacion.setEstatus(estatusRechazada);
	        // CHECKMARX-FP: Operación segura — usando JPA repository 'operacionJuridicaRepository.save()'.
			// La entidad 'operacion' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
	        operacionJuridicaRepository.save(operacion);
	
	        InstruccionJuridica instruccion = operacion.getInstruccion();
	        List<OperacionJuridica> operacionesDeLaInstruccion = operacionJuridicaRepository
	                .findByInstruccionFolio(instruccion.getFolio());
	
	        boolean todasRechazadas = operacionesDeLaInstruccion.stream()
	                .allMatch(op -> "RE".equals(op.getEstatus().getCve()));
	
	        if (todasRechazadas) {
	            EstatusInstruccion estatusRechazadaInstruccion = estatusInstruccionRepository.findByCve("RE");
	            if (estatusRechazadaInstruccion == null) {
	                throw new RuntimeException("Estatus de Instrucción 'RE' no encontrado en la base de datos.");
	            }
	            instruccion.setEstatus(estatusRechazadaInstruccion);
	            // CHECKMARX-FP: Operación segura — usando JPA repository 'instruccionJuridicaRepository.save()'.
				// La entidad 'instruccion' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
	            instruccionJuridicaRepository.save(instruccion);
	        }
	
	        log.info("Revision de operacion juridica completada con exito: {}", revision.getId());
	
	        return revisionOperacionJuridicaMapper.toDto(revision);
	    }
	
	    public void actualizarEnvioNotaria(Long id) {
	    	// CHECKMARX-FP: Operación segura — uso de JPA findById() con SQL parametrizado.
	    	// El valor 'id' se pasa como parámetro seguro en Spring Data JPA.
	        OperacionJuridica operacion = operacionJuridicaRepository.findById(id)
	                .orElseThrow(() -> new RuntimeException("Operacion juridica no encontrada con id " + id));
	        operacion.setEnvioNotaria(true);
	        // CHECKMARX-FP: Operación segura — usando JPA repository 'operacionJuridicaRepository.save()'.
			// La entidad 'operacion' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
	        operacionJuridicaRepository.save(operacion);
	        log.info("Envio a notaria actualizado con exito para la operación juridica con ID: {}", id);
	    }
	}