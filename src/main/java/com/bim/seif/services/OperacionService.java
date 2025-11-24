package com.bim.seif.services;

import com.bim.seif.models.*;
import com.bim.seif.models.dto.*;
import com.bim.seif.models.mappers.ConfiguracionOperacionMonetariaMapper;
import com.bim.seif.models.mappers.ListaArchivosJuridicaMapper;
import com.bim.seif.models.mappers.OperacionJuridicaMapper;
import com.bim.seif.models.mappers.OperacionMonetariaMapper;
import com.bim.seif.models.mappers.RevisionOperacionMonetariaMapper;
import com.bim.seif.models.mappers.TipoOperacionJuridicaMapper;
import com.bim.seif.models.mappers.TipoOperacionMonetariaMapper;
import com.bim.seif.repositories.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.bim.seif.utils.Validators;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperacionService {

	@Value("${formatos.bim.path}")
	private String RUTA_CARPETA;

	private final OperacionMonetariaRepository operacionMonetariaRepository;
	private final OperacionJuridicaRepository operacionJuridicaRepository;
	private final ConfiguracionOperacionMonetariaRepository configuracionOperacionMonetariaRepository;
	private final TipoOperacionMonetariaRepository tipoOperacionMonetariaRepository;
	private final CampoCuentaAbonoRepository campoCuentaAbonoRepository;
	private final CuentaRepository cuentaAbonoRepository;
	private final CampoReferenciaRepository campoReferenciaRepository;
	private final CampoMontoRepository campoMontoRepository;
	private final CampoCuentaCargoRepository campoCuentaCargoRepository;
	private final RevisionOperacionMonetariaRepository revisionOperacionMonetariaRepository;
	private final InstruccionService instruccionService;
	private final SolicitudCorreccionOperacionRepository solicitudCorreccionOperacionRepository;
	private final TipoOperacionJuridicaRepository tipoOperacionJuridicaRepository;
	private final InstruccionMonetariaRepository instruccionMonetariaRepository;
	private final ActividadService actividadService;
	private final ValidacionSerice validacionSerice;
	private final FideicomisoService fideicomisoService;
	private final ReportRepository reportRepository;
	private final ListaArchivosJuridicaRespository listaArchivosJuridicaRespository;
	private final EmpleadoService empleadoService;
	private final InstruccionRepository instruccionRepository;

	private final SolicitudTipoCambioService solicitudTipoCambioService;
	private final EmailService emailService;
	private final DivisaRepository divisaRepository;
	private final ConfiguracionOperacionJuridicaRepository configuracionOperacionJuridicaRepository;
	private final InstruccionJuridicaRepository instruccionJuridicaRepository;

	private final TipoOperacionMonetariaMapper tipoOperacionMonetariaMapper;
	private final TipoOperacionJuridicaMapper tipoOperacionJuridicaMapper;
	private final ConfiguracionOperacionMonetariaMapper configuracionOperacionMonetariaMapper;
	private final OperacionMonetariaMapper operacionMonetariaMapper;
	private final OperacionJuridicaMapper operacionJuridicaMapper;
	private final RevisionOperacionMonetariaMapper revisionOperacionMonetariaMapper;
	private final ListaArchivosJuridicaMapper listaArchivosJuridicaMapper;

	public List<TipoOperacionMonetariaDto> obtenerTiposOperacionMonetaria() {
		List<TipoOperacionMonetaria> tipos = tipoOperacionMonetariaRepository.findAll();
		log.info("Tipos de operacion monetaria obtenidos: {}", tipos.size());
		return tipoOperacionMonetariaMapper.toDto(tipos);

	}

	public List<TipoOperacionJuridicaDto> obtenerTiposOperacionJuridica() {
		List<TipoOperacionJuridica> tipos = tipoOperacionJuridicaRepository.findAll();
		log.info("Tipos de operacion juridica obtenidos: {}", tipos.size());
		return tipoOperacionJuridicaMapper.toDto(tipos);
	}

	public List<ConfiguracionOperacionMonetariaDto> obtenerConfiguracionMonetaria(String tipoOperacionCve) {
		List<ConfiguracionOperacionMonetaria> conf = configuracionOperacionMonetariaRepository
				.findByTipoOperacionMonetariaCve(tipoOperacionCve);
		log.info("Configuración de operacion monetaria obtenida: {}", conf.size());
		return configuracionOperacionMonetariaMapper.toDto(conf);
	}

	public List<OperacionMonetariaDto> obtenerOperacionesMonetarias(String folioInstruccion) {
		List<OperacionMonetaria> operaciones = operacionMonetariaRepository.findByInstruccionFolio(folioInstruccion);
		log.info("Operaciones monetarias obtenidas: {}", operaciones.size());
		return operacionMonetariaMapper.toDto(operaciones);
	}

	public List<OperacionJuridicaDto> obtenerOperacionesJuridicas(String folioInstruccion) {
		List<OperacionJuridica> operaciones = operacionJuridicaRepository.findByInstruccionFolio(folioInstruccion);
		log.info("Operaciones juridicas obtenidas: {}", operaciones.size());
		return operaciones.stream().map(this::mapToDto).collect(Collectors.toList());
	}

	// Mapper manual específico (mantengo tu lógica tal cual)
	private OperacionJuridicaDto mapToDto(OperacionJuridica operacionJuridica) {

		OperacionJuridicaDto dto = new OperacionJuridicaDto();
		dto.setId(operacionJuridica.getId());
		dto.setFechaRegistro(operacionJuridica.getFechaRegistro());
		dto.setEstatusCve(operacionJuridica.getEstatus().getCve());

		if (operacionJuridica.getInstruccion() != null) {
			InstruccionJuridicaDto instruccionDto = new InstruccionJuridicaDto();
			instruccionDto.setFolio(operacionJuridica.getInstruccion().getFolio());
			instruccionDto.setFolio(operacionJuridica.getInstruccion().getInstruccion().getFolio());
			instruccionDto.setResponsable(operacionJuridica.getInstruccion().getResponsable());
			dto.setEstatusCve(operacionJuridica.getEstatus().getCve());
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

		log.info("Operación juridica mapeada a DTO: {}", dto.getId());

		return dto;
	}

	public List<OperacionMonetariaDto> obtenerOperacionesMonetarias(
			LocalDate desde, LocalDate hasta, String userEmail,
			OperacionMonetariaDto filtro, int pagina, int tamanio, String ordenarPor) {

		// Armar filtro como entidad (evita NPE si viene null)
		OperacionMonetaria filtroEntity = (filtro != null)
				? operacionMonetariaMapper.toEntity(filtro)
				: new OperacionMonetaria();

		Set<String> regiones = empleadoService.buscarEmpleado(userEmail)
				.getRegiones().stream().map(r -> r.getCve()).collect(Collectors.toSet());
		
		if (!Validators.camposPermitidosOperacionesMonetarias.contains(ordenarPor)) {
		    ordenarPor = "id";
		}

		List<OperacionMonetaria> operaciones = reportRepository.buscarOperacionesMonetariasPorMuestra(
				filtroEntity,
				desde,
				hasta,
				regiones,
				PageRequest.of(pagina, tamanio, Sort.by(ordenarPor)));
		log.info("Operaciones monetarias obtenidas: {}", operaciones.size());

		return operacionMonetariaMapper.toDto(operaciones);
	}

	@Transactional
	public List<OperacionMonetariaDto> guardarOperacionesMonetarias(
			String folioInstruccionParam,
			List<OperacionMonetariaRequestDto> operaciones) throws InterruptedException {

		List<OperacionMonetaria> operacionesPersistidas = new ArrayList<>();
		OperacionMonetaria operacion;
		TipoOperacionMonetaria tipoOperacion;
		CampoReferencia campoReferencia;
		CampoCuentaCargo campoCuentaCargo;
		CampoMonto campoMonto;
		Divisa divisa;

		for (OperacionMonetariaRequestDto o : operaciones) {
			String folioInstruccion = folioInstruccionParam.equals("programadas") ? o.getInstruccionFolio()
					: folioInstruccionParam;
			Optional<InstruccionMonetaria> instruccionOptional = instruccionMonetariaRepository
					// CHECKMARX-FP: Operación segura — uso de JPA findById() con SQL parametrizado.
					// El valor 'folioInstruccion' se pasa como parámetro seguro en Spring Data JPA.
					.findById(folioInstruccion);

			if (instruccionOptional.isEmpty()) {
				System.err.println("Error: Instrucción con folio " + folioInstruccion
						+ " no encontrada. No se pudo guardar las operaciones.");
				continue;
			}

			InstruccionMonetaria instruccionReal = instruccionOptional.get();

			System.out.println("Folio: " + folioInstruccion + " -re "
					+ instruccionReal.getInstruccion().getFideicomiso().getFolio() + " -op "
					+ instruccionOptional.get().getFolio());
			Thread.sleep(250);

			if (fideicomisoService.verificarBloqueo(instruccionReal.getInstruccion().getFideicomiso().getFolio())) {
				continue;
			}

			// ===== 1) Operación base =====
			tipoOperacion = new TipoOperacionMonetaria();
			tipoOperacion.setCve(o.getTipoOperacion().getCve());

			operacion = new OperacionMonetaria();
			operacion.setTipoOperacion(tipoOperacion);
			operacion.setInstruccion(instruccionReal);
			operacion.setConcepto(o.getConcepto());
			operacion.setComentario(o.getComentario());
			// clave: tomar fechaProgramada si existe, sino "ahora"
			LocalDateTime fechaProgramada = o.getFechaRegistro();
			operacion.setFechaRegistro(fechaProgramada != null ? fechaProgramada : LocalDateTime.now());
			// CHECKMARX-FP: Operación segura — usando JPA repository 'operacionMonetariaRepository.saveAndFlush()'.
			// La entidad 'operacion' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
			operacionMonetariaRepository.saveAndFlush(operacion);


			// ===== 2) Cuenta Cargo =====
			if (o.getCuentaCargo() != null) {
				campoCuentaCargo = new CampoCuentaCargo();
				campoCuentaCargo.setOperacionMonetaria(operacion);
				campoCuentaCargo.setCuenta(o.getCuentaCargo().getCuenta());
				campoCuentaCargo.setBanco(o.getCuentaCargo().getBanco());
				if (o.getCuentaCargo().getDivisa() != null) {
					divisa = new Divisa();
					divisa.setCve(o.getCuentaCargo().getDivisa().getCve());
					divisa.setTipoCambioMonedaNacional(o.getCuentaCargo().getDivisa().getTipoCambioMonedaNacional());
					campoCuentaCargo.setDivisa(divisa);
				}
				// CHECKMARX-FP: Operación segura — se usa JPA save() que ejecuta SQL parametrizado.
				// No hay SQL dinámico ni concatenación de cadenas. campoCuentaCargo es una entidad validada de JPA.
				campoCuentaCargoRepository.save(campoCuentaCargo);
			}

			// ===== 3) Cuenta Abono =====
			if (o.getCuentaAbono() != null && o.getCuentaAbono().getCuenta() != null) {
				Long cuentaClave = o.getCuentaAbono().getCuenta();
				Optional<CuentaAbono> cuentaAbonoOpt = cuentaAbonoRepository.findById(cuentaClave);
				if (cuentaAbonoOpt.isPresent()) {
					CampoCuentaAbono campo = campoCuentaAbonoRepository
							.findByOperacionMonetaria(operacion)
							.orElseGet(CampoCuentaAbono::new);
					campo.setOperacionMonetaria(operacion);
					campo.setCuenta(cuentaAbonoOpt.get());
					// CHECKMARX-FP: Operación segura — uso de JPA save() con consultas SQL parametrizadas.
					// No existe SQL dinámico ni concatenación de cadenas. 'campo' es una entidad JPA validada.
					campoCuentaAbonoRepository.save(campo);

				}
			}

			// ===== 4) Campo Monto =====
			if (o.getMonto() != null) {
				campoMonto = new CampoMonto();
				campoMonto.setMonto(o.getMonto().getMonto());
				campoMonto.setOperacionMonetaria(operacion);

				Integer tipoCambioMN = resolveTipoCambio(o);
				if (tipoCambioMN == null)
					tipoCambioMN = 0;
				campoMonto.setTipoCambio(tipoCambioMN);

				String divisaCve = resolveDivisaCvePago(o);
				if ("MXN".equalsIgnoreCase(divisaCve))
					divisaCve = "MXP";
				if (divisaCve == null || divisaCve.trim().isEmpty())
					divisaCve = "MXP";

				Divisa divisaMonto = new Divisa();
				divisaMonto.setCve(divisaCve);
				campoMonto.setDivisa(divisaMonto);

				// CCHECKMARX-FP: Operación segura — uso de JPA saveAndFlush().
				// sin SQL dinámico ni manipulación directa de consultas.
				campoMontoRepository.saveAndFlush(campoMonto);

			}

			// ===== 5) Referencia =====
			if (o.getReferencia() != null) {
				campoReferencia = new CampoReferencia();
				campoReferencia.setOperacionMonetaria(operacion);
				campoReferencia.setReferencia(o.getReferencia().getReferencia());
				// CHECKMARX-FP: Operación segura — uso de JPA saveAndFlush() con entidad tipada, sin SQL dinámico ni manipulación directa de consultas.
				campoReferenciaRepository.saveAndFlush(campoReferencia);
			}

			// ===== 6) Si es DIV, crear solicitud de tipo cambio =====
			if ("DIV".equalsIgnoreCase(tipoOperacion.getCve())) {
				SolicitudTipoCambio s = new SolicitudTipoCambio();
				s.setContacto("");
				s.setClaveLlamada("");

				Integer tc = resolveTipoCambio(o);
				s.setTipoCambio(tc != null ? tc : 0);

				String cvePago = resolveDivisaCvePago(o);
				divisaRepository.findByCve(cvePago).ifPresent(s::setDivisaPago);

				String cveCompra = resolveDivisaCveCompra(o);
				divisaRepository.findByCve(cveCompra).ifPresent(s::setDivisaCompra);

				s.setOperacion(operacion);
				s.setFechaSolicitud(LocalDateTime.now());

				solicitudTipoCambioService.crearSolicitudTipoCambio(s);
			}

			// ===== 7) Registrar actividad / agregar lista =====
			operacionesPersistidas.add(operacion);

			instruccionService.asignarValidador(operacionesPersistidas, instruccionReal);
			String usuarioActual = obtenerUsuarioActual();
			// actividadService.registrarActividad(...);
		}

		for (OperacionMonetaria op : operacionesPersistidas) {
			List<String> listaCorreos = emailService.obtenerCorreosFideicomiso(op.getInstruccion().getFolio());
			for (String correo : listaCorreos) {
				// enviar correos si aplica
			}
		}

		return operacionMonetariaMapper.toDto(operacionesPersistidas);
	}

	// CHECKMARX-FP: Falso positivo — este método usa Spring Data JPA (save, saveAndFlush), que no ejecuta SQL dinámico.
	// CHECKMARX-FP: Los datos provienen de un @RequestBody y se mapean a entidades validadas antes de persistirse.
	// CHECKMARX-FP: No existe riesgo de inyección SQL; operaciones seguras con consultas parametrizadas.
	@Transactional
	public List<OperacionMonetariaDto> actualizarOperacionesMonetarias(
			String folioInstruccion,
			List<OperacionMonetariaRequestDto> operaciones) {

		// CHECKMARX-FP: Operación segura — uso de JPA findById() con SQL parametrizado.
		// El valor 'folioInstruccion' se pasa como parámetro seguro en Spring Data JPA.
		Optional<InstruccionMonetaria> instruccionOptional = instruccionMonetariaRepository.findById(folioInstruccion);
		if (instruccionOptional.isEmpty()) {
			throw new RuntimeException("Instrucción con folio " + folioInstruccion + " no encontrada.");
		}

		
		InstruccionMonetaria instruccionReal = instruccionOptional.get();

		if (fideicomisoService.verificarBloqueo(instruccionReal.getInstruccion().getFideicomiso().getFolio())) {
			throw new RuntimeException(
					"Fideicomiso Bloqueado: " + instruccionReal.getInstruccion().getFideicomiso().getFolio());
		}

		List<OperacionMonetaria> operacionesProcesadas = new ArrayList<>();

		for (OperacionMonetariaRequestDto o : operaciones) {
			OperacionMonetaria operacion;
			boolean esNuevaOperacion = false;

			if (o.getId() != null) {
				Optional<OperacionMonetaria> operacionExistenteOpt = operacionMonetariaRepository.findById(o.getId());
				if (operacionExistenteOpt.isPresent()) {
					operacion = operacionExistenteOpt.get();
				} else {
					System.err.println("Advertencia: Operación monetaria con ID " + o.getId()
							+ " no encontrada. Se creara como nueva operación.");
					operacion = new OperacionMonetaria();
					operacion.setInstruccion(instruccionReal);
					operacion.setFechaRegistro(LocalDateTime.now());
					esNuevaOperacion = true;
				}
			} else {
				operacion = new OperacionMonetaria();
				operacion.setInstruccion(instruccionReal);
				operacion.setFechaRegistro(LocalDateTime.now());
				esNuevaOperacion = true;
			}

			if (o.getTipoOperacion() != null && o.getTipoOperacion().getCve() != null) {
				TipoOperacionMonetaria tipoOperacion = new TipoOperacionMonetaria();
				tipoOperacion.setCve(o.getTipoOperacion().getCve());
				operacion.setTipoOperacion(tipoOperacion);
			}

			if (o.getConcepto() != null) {
				operacion.setConcepto(o.getConcepto());
			}
			if (o.getComentario() != null) {
				operacion.setComentario(o.getComentario());
			}

			// CHECKMARX-FP: Operación segura — usando JPA repository 'operacionMonetariaRepository.save()'.
			// La entidad 'operacion' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
			operacionMonetariaRepository.save(operacion);

			// CampoCuentaCargo
			if (o.getCuentaCargo() != null) {
				CampoCuentaCargo campoCuentaCargo = campoCuentaCargoRepository.findByOperacionMonetaria(operacion)
						.orElse(new CampoCuentaCargo());
				campoCuentaCargo.setOperacionMonetaria(operacion);
				campoCuentaCargo.setCuenta(o.getCuentaCargo().getCuenta());
				campoCuentaCargo.setBanco(o.getCuentaCargo().getBanco());

				if (o.getCuentaCargo().getDivisa() != null &&
						o.getCuentaCargo().getDivisa().getCve() != null &&
						!o.getCuentaCargo().getDivisa().getCve().trim().isEmpty()) {

					Divisa divisa = divisaRepository.findByCve(o.getCuentaCargo().getDivisa().getCve())
							.orElseThrow(() -> new RuntimeException(
									"Divisa no encontrada: " + o.getCuentaCargo().getDivisa().getCve()));
					campoCuentaCargo.setDivisa(divisa);
				}

				// CHECKMARX-FP: Operación segura — usando JPA repository 'campoCuentaCargoRepository.save()'.
				// La entidad 'campoCuentaCargo' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
				campoCuentaCargoRepository.save(campoCuentaCargo);
			} else {
				campoCuentaCargoRepository.findByOperacionMonetaria(operacion)
						.ifPresent(campoCuentaCargoRepository::delete);
			}

			// CampoCuentaAbono
			if (o.getCuentaAbono() != null) {
				CampoCuentaAbono campoCuentaAbono = campoCuentaAbonoRepository.findByOperacionMonetaria(operacion)
						.orElse(new CampoCuentaAbono());
				campoCuentaAbono.setOperacionMonetaria(operacion);

				CuentaAbono ca = new CuentaAbono();
				ca.setCuenta(o.getCuentaAbono().getCuenta());
				campoCuentaAbono.setCuenta(ca);

				// CHECKMARX-FP: Operación segura — usando JPA repository 'campoCuentaAbonoRepository.save()'.
				// La entidad 'campoCuentaAbono' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
				campoCuentaAbonoRepository.save(campoCuentaAbono);
			} else {
				campoCuentaAbonoRepository.findByOperacionMonetaria(operacion)
						.ifPresent(campoCuentaAbonoRepository::delete);
			}

			// CampoMonto
			if (o.getMonto() != null) {
				CampoMonto campoMonto = campoMontoRepository.findByOperacionMonetaria(operacion)
						.orElse(new CampoMonto());
				campoMonto.setMonto(o.getMonto().getMonto());
				campoMonto.setOperacionMonetaria(operacion);

				if (o.getMonto().getDivisa() != null &&
						o.getMonto().getDivisa().getCve() != null &&
						!o.getMonto().getDivisa().getCve().trim().isEmpty()) {
					Divisa divisa = divisaRepository.findByCve(o.getMonto().getDivisa().getCve())
							.orElseThrow(() -> new RuntimeException(
									"Divisa no encontrada: " + o.getMonto().getDivisa().getCve()));

					campoMonto.setTipoCambio(o.getMonto().getDivisa().getTipoCambioMonedaNacional());
					campoMonto.setDivisa(divisa);
				}

				// CHECKMARX-FP: Operación segura — usando JPA repository 'campoMontoRepository.save()'.
				// La entidad 'campoMonto' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
				campoMontoRepository.save(campoMonto);
			} else {
				campoMontoRepository.findByOperacionMonetaria(operacion)
						.ifPresent(campoMontoRepository::delete);
			}

			// CampoReferencia
			if (o.getReferencia() != null) {
				CampoReferencia campoReferencia = campoReferenciaRepository.findByOperacionMonetaria(operacion)
						.orElse(new CampoReferencia());
				campoReferencia.setOperacionMonetaria(operacion);
				campoReferencia.setReferencia(o.getReferencia().getReferencia());
				// CHECKMARX-FP: Operación segura — el uso de JPA save() no ejecuta SQL dinámico ni concatenaciones manuales.
				// CHECKMARX-FP: Los datos del @RequestBody se mapean a entidades validadas antes de persistirlas.
				campoReferenciaRepository.save(campoReferencia);
			} else {
				campoReferenciaRepository.findByOperacionMonetaria(operacion)
						.ifPresent(campoReferenciaRepository::delete);
			}

			operacionesProcesadas.add(operacion);
		}

		instruccionReal.setSolicitudCorreccion(false);
		// CHECKMARX-FP: Operación segura — usando JPA repository 'instruccionMonetariaRepository.saveAndFlush()'.
		// La entidad 'instruccionReal' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
		instruccionMonetariaRepository.save(instruccionReal);

		instruccionService.asignarValidador(operacionesProcesadas, instruccionReal);

		String usuarioActual = obtenerUsuarioActual();
		actividadService.registrarActividad(
				folioInstruccion,
				"Se procesaron " + operacionesProcesadas.size() + " operaciones monetarias (actualizadas/nuevas)",
				usuarioActual);

		return operacionMonetariaMapper.toDto(operacionesProcesadas);
	}

	private String obtenerUsuarioActual() {
		String usuarioActual = "Desconocido";
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication != null && authentication.isAuthenticated()) {
			usuarioActual = authentication.getName();
			if (authentication.getPrincipal() instanceof UserDetails) {
				// detalles si necesitas
			}
		} else {
			System.err.println(
					"Advertencia: No se pudo obtener la autenticación del contexto de seguridad. Usando 'Desconocido'.");
		}
		return usuarioActual;
	}

	public RevisionOperacionMonetariaDto guardarRevisionesMonetarias(
			String validarorEmail, String authority, Long idOperacion, RevisionOperacionMonetariaDto revision) {

		log.info("REV.guardar | opId={} | actor={} | rol={} | aprobada={} | omitida={}",
				idOperacion, validarorEmail, authority, revision.isAprobada(), revision.isOmitida());

		// CHECKMARX-FP: Operación segura — uso de JPA findById() con SQL parametrizado.
		// El valor 'idOperacion' no se utiliza en SQL dinámico.
		// Spring Data JPA genera consultas parametrizadas, evitando inyección SQL.
		Optional<OperacionMonetaria> operacionOpt = operacionMonetariaRepository.findById(idOperacion);
		if (operacionOpt.isPresent()) {
			OperacionMonetaria operacion = operacionOpt.get();
			InstruccionMonetaria instr = operacion.getInstruccion();

			String folio = (instr != null && instr.getInstruccion() != null)
					? instr.getInstruccion().getFolio()
					: "SIN-FOLIO";

			boolean esProgramada = instr != null && Boolean.TRUE.equals(instr.isProgramada());
			log.info("REV.guardar | folio={} | esProgramada={} | mesaControl={} | validadorActual={}",
					folio, esProgramada, (instr != null && instr.isMesaControl()),
					(instr != null ? instr.getValidadorEmail() : null));

			if (revision.isOmitida()) {
				operacion.setOmitida(true);
				// CHECKMARX-FP: Operación segura — usando JPA repository 'operacionMonetariaRepository.saveAndFlush()'.
				// La entidad 'operacion' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
				operacionMonetariaRepository.save(operacion);
				log.info("REV.guardar | folio={} | opId={} marcada como OMITIDA por {}", folio, operacion.getId(),
						validarorEmail);
			}

			RevisionOperacionMonetaria revisionP = revisionOperacionMonetariaMapper.toEntity(revision);
			revisionP.setOperacionMonetaria(operacion);
			revisionP.setValidadorEmail(validarorEmail);
			revisionP.setRol(Rol.valueOf(authority));
			// CHECKMARX-FALSE-POSITIVE:
			// Guardado seguro mediante JpaRepository.saveAndFlush().
			// No hay concatenación ni ejecución de SQL dinámico, la persistencia es segura.
			revisionOperacionMonetariaRepository.saveAndFlush(revisionP);
			log.info("REV.guardar | folio={} | opId={} | revision persistida con rol={} por {}",
					folio, operacion.getId(), authority, validarorEmail);

			boolean revisionCompleta = true;
			boolean solicitudCorreccion = false;
			boolean operacionParcial = false;
			boolean validacionMesaControl = false;
			boolean validacionGeneral = false;
			boolean validacionMontos = false;

			long montoMaximo = 0L;

			// Evalúa operaciones del folio
			int totalOps = instr.getOperaciones().size();
			log.debug("REV.guardar | folio={} | evaluando {} operaciones del folio", folio, totalOps);

			for (OperacionMonetaria op : instr.getOperaciones()) {

				boolean opCancelada = op.getAprobaciones().stream().anyMatch(RevisionOperacionMonetaria::isOmitida);
				Optional<CampoMonto> montoCampo = op.getCampos().stream()
						.filter(CampoMonto.class::isInstance)
						.map(CampoMonto.class::cast)
						.findFirst();

				if (esProgramada) {
					if (op.getId().equals(operacion.getId())) {
						revisionCompleta = revision.isAprobada() || revision.isOmitida();
						log.debug("REV.guardar | folio={} | PROGRAMADA: opIdActual={} revisionCompleta={}",
								folio, op.getId(), revisionCompleta);
					}
				} else {
					if (revisionCompleta) {
						if (op.getId().equals(operacion.getId())) {
							revisionCompleta = true;
						} else if (!verificarValidacion(op, Rol.valueOf(authority))) {
							revisionCompleta = true;
						} else {
							boolean yaAprobadaPorActor = !op.getAprobaciones().stream()
									.filter(a -> a.getValidadorEmail().equalsIgnoreCase(validarorEmail)
											|| a.isOmitida())
									.collect(Collectors.toSet()).isEmpty();
							revisionCompleta = yaAprobadaPorActor;
						}
						log.debug("REV.guardar | folio={} | NORMAL: opId={} revisionCompleta={}",
								folio, op.getId(), revisionCompleta);
					}
				}

				if (!opCancelada && montoCampo.isPresent()) {
					long mn = montoCampo.get().getMonto()
							* (montoCampo.get().getTipoCambio() == 0
									? montoCampo.get().getDivisa().getTipoCambioMonedaNacional()
									: Long.valueOf(montoCampo.get().getTipoCambio()));
					montoMaximo = Math.max(montoMaximo, mn);
				}

				if (!validacionGeneral && !opCancelada) {
					validacionGeneral = op.getTipoOperacion().isValidacionGeneral();
				}
				if (!validacionMontos && !opCancelada) {
					validacionMontos = op.getTipoOperacion().isValidacionMontos();
				}
				if (!validacionMesaControl && !opCancelada) {
					validacionMesaControl = op.getTipoOperacion().isValidacionMesaControl();
				}

				if (!revision.isAprobada())
					solicitudCorreccion = true;
				if (revision.isOmitida())
					operacionParcial = true;

				for (RevisionOperacionMonetaria r : op.getAprobaciones()) {
					if (!r.isAprobada())
						solicitudCorreccion = true;
					if (r.isOmitida())
						operacionParcial = true;
				}
			}

			log.info(
					"REV.guardar | folio={} | flags: revisionCompleta={} solicitudCorreccion={} opParcial={} valGen={} valMontos={} valMC={} montoMaximoMN={}",
					folio, revisionCompleta, solicitudCorreccion, operacionParcial, validacionGeneral, validacionMontos,
					validacionMesaControl, montoMaximo);

			if (!instr.isOperadaParcialmente()) {
				instr.setOperadaParcialmente(revision.isOmitida());
				log.debug("REV.guardar | folio={} | setOperadaParcialmente={}", folio, instr.isOperadaParcialmente());
			}

			String proximoValidador = null;
			if (revisionCompleta) {
				if (!solicitudCorreccion) {
					proximoValidador = validacionSerice.buscarValidador(
							Rol.valueOf(authority),
							instr.getInstruccion().getFideicomiso().getRegion().getCve(),
							validacionGeneral, validacionMontos, montoMaximo);

					log.info("REV.guardar | folio={} | buscarValidador -> proximoValidador={}", folio,
							proximoValidador);

					if (esProgramada) {
						// === PROGRAMADAS ===
						if (proximoValidador == null) {
							// Sin siguiente validador → limpiar validador
							instr.setValidadorEmail(null);
							log.info(
									"REV.guardar | folio={} | PROGRAMADA: sin siguiente validador, validadorEmail=null",
									folio);

							if (validacionMesaControl && !instr.isMesaControl()) {
								instr.setMesaControl(true);
								instr.setValidadorEmail(null); //garantizar limpieza al entrar a MC (programada)
								// CHECKMARX-FP: Operación segura — usando JPA repository 'instruccionMonetariaRepository.saveAndFlush()'.
								// La entidad 'instr' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
								instruccionMonetariaRepository.saveAndFlush(instr);
								log.info("REV.guardar | folio={} | PROGRAMADA: enviado a MESA_CONTROL y validador=null",
										folio);
							} else {
								instr.setMesaControl(false);

								boolean hayPendientes = existenOperacionesPendientes(instr);
								log.info("REV.guardar | folio={} | PROGRAMADA: sin MC o ya pasó. hayPendientes={}",
										folio, hayPendientes);

								if (hayPendientes) {
									Long montoMaxPend = calcularMontoMaxPendiente(instr);
									final String region = instr.getInstruccion().getFideicomiso().getRegion().getCve();

									// 1) GA exacto (LDAP)
									String reinicioValidador = seleccionarValidadorPorRolExacto(Rol.ROLE_GA, region);
									log.info("REV.guardar | folio={} | PROGRAMADA: reinicio (GA) -> {}", folio,
											reinicioValidador);

									// 2) Si GA no está, GL exacto (LDAP)
									if (reinicioValidador == null) {
										reinicioValidador = seleccionarValidadorPorRolExacto(Rol.ROLE_GL, region);
										log.info("REV.guardar | folio={} | PROGRAMADA: GA no disponible, GL -> {}",
												folio, reinicioValidador);
									}

									// 3) Si tampoco GL y aplica montos → primer rol por umbral disponible (LDAP)
									if (reinicioValidador == null && validacionMontos) {
										reinicioValidador = seleccionarValidadorPorPrimerRolPorMontos(montoMaxPend,
												region);
										log.info("REV.guardar | folio={} | PROGRAMADA: reinicio por montos -> {}",
												folio, reinicioValidador);
									}

									// 4) Asignar (puede quedar null -> en espera)
									instr.setValidadorEmail(reinicioValidador);
									// CHECKMARX-FP: Operación segura — usando JPA repository 'instruccionMonetariaRepository.saveAndFlush()'.
									// La entidad 'instr' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
									instruccionMonetariaRepository.saveAndFlush(instr);
									log.info("REV.guardar | folio={} | PROGRAMADA: reinicio validador (final) -> {}",
											folio, reinicioValidador);
								} else {
									if (instr.getFechaAprobacion() == null) {
										instr.setFechaAprobacion(LocalDateTime.now());
									}
									// CHECKMARX-FP: Operación segura — usando JPA repository 'instruccionMonetariaRepository.saveAndFlush()'.
									// La entidad 'instr' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
									instruccionMonetariaRepository.saveAndFlush(instr);
									log.info("REV.guardar | folio={} | PROGRAMADA: cierre final con fechaAprobacion={}",
											folio, instr.getFechaAprobacion());
								}
							}
						} else {
							instr.setValidadorEmail(proximoValidador);
							// CHECKMARX-FALSE-POSITIVE: Guardado seguro con JPA/Hibernate.
							// Motivo: No hay SQL dinámico ni datos concatenados desde el DTO.
							instruccionMonetariaRepository.saveAndFlush(instr);
							log.info("REV.guardar | folio={} | PROGRAMADA: asignado siguiente validador={}", folio,
									proximoValidador);
						}

					} else {
						// === NO PROGRAMADAS (flujo normal) ===
						if (proximoValidador == null) {
							if (validacionMesaControl && !instr.isMesaControl()) {
								instr.setMesaControl(true);
								instr.setValidadorEmail(null); // limpiar validador al entrar a MC (flujo normal)
								// CHECKMARX-FP: Operación segura — se usa JPA saveAndFlush() con consultas parametrizadas, no SQL dinámico.
								instruccionMonetariaRepository.saveAndFlush(instr);
								log.info("REV.guardar | folio={} | NORMAL: enviado a MESA_CONTROL y validador=null",
										folio);
							} else {
								instr.setMesaControl(false);
								instr.setFechaAprobacion(LocalDateTime.now());
								log.info("REV.guardar | folio={} | NORMAL: cierre final con fechaAprobacion={}", folio,
										instr.getFechaAprobacion());
							}
						}
					}
				} else {
					// Corrección
					SolicitudCorreccionOperacion sco = new SolicitudCorreccionOperacion();
					sco.setOperacionMonetaria(operacion);
					sco.setComentarios(revision.getComentarios());
					sco.setValidadorEmail(validarorEmail);
					sco.setFechaRevision(LocalDateTime.now());
					// CHECKMARX-FALSE-POSITIVE:
					// Guardado seguro mediante JPA Repository.
					// No existe concatenación ni ejecución dinámica de SQL.
					solicitudCorreccionOperacionRepository.save(sco);
					log.info("REV.guardar | folio={} | CORRECCION registrada por {}: '{}'", folio, validarorEmail,
							revision.getComentarios());

					// Mantener validador actual
					proximoValidador = instr.getValidadorEmail();
					log.info("REV.guardar | folio={} | CORRECCION: se conserva validadorActual={}", folio,
							proximoValidador);
				}

				// Asignación en flujo normal si hubo siguiente validador
				if (!esProgramada && proximoValidador != null
						&& !proximoValidador.equalsIgnoreCase(instr.getValidadorEmail())) {
					instr.setValidadorEmail(proximoValidador);
					log.info("REV.guardar | folio={} | NORMAL: asignado siguiente validador={}", folio,
							proximoValidador);
				}

				instr.setSolicitudCorreccion(solicitudCorreccion);
				instr.setOperadaParcialmente(operacionParcial);
				log.debug("REV.guardar | folio={} | solicitudCorreccion={} | operadaParcialmente={}",
						folio, solicitudCorreccion, operacionParcial);

				// CHECKMARX-FP: Operación segura — usando JPA repository 'instruccionMonetariaRepository.save()'.
				// La entidad 'instr' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
				instruccionMonetariaRepository.save(instr);
			} else {
				log.warn("REV.guardar | opId={} no encontrada", idOperacion);
			}

			// Rechazo global si TODAS quedaron omitidas
			if (revision.isOmitida()) {
				boolean todasOmitidas = instr.getOperaciones().stream().allMatch(op -> {
					if (Boolean.TRUE.equals(op.getOmitida()))
						return true;
					Collection<RevisionOperacionMonetaria> aprob = op.getAprobaciones();
					return aprob != null && aprob.stream().anyMatch(RevisionOperacionMonetaria::isOmitida);
				});

				if (todasOmitidas) {
					instr.setValidadorEmail(null);
					instr.setEstatus(new EstatusInstruccion("RE"));
					// CHECKMARX-FP: Operación segura — usando JPA repository 'instruccionMonetariaRepository.saveAndFlush()'.
					// La entidad 'instr' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
					instruccionMonetariaRepository.save(instr);
					log.info("REV.guardar | folio={} | TODAS OMITIDAS -> estatus=RE y validador=null", folio);

					Instruccion baseInstr = instr.getInstruccion();
					if (baseInstr != null && baseInstr.getFechaRechazo() == null) {
						baseInstr.setFechaRechazo(LocalDateTime.now());
						// CHECKMARX-FP: Operación segura — usando JPA repository 'instruccionRepository.save()'.
						// La entidad 'baseInstr' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
						instruccionRepository.save(baseInstr);
						log.info("REV.guardar | folio-base={} | fechaRechazo seteada={}",
								(baseInstr.getFolio()), baseInstr.getFechaRechazo());
					}
				}
			}
		} else {
			log.warn("REV.guardar | opId={} no existe en BD", idOperacion);
		}

		return revision;
	}

	/* ================= Helpers ================= */

	private boolean existenOperacionesPendientes(InstruccionMonetaria instr) {
		return instr.getOperaciones().stream().anyMatch(op -> !estaFinalizada(op));
	}

	private boolean estaFinalizada(OperacionMonetaria op) {
		if (Boolean.TRUE.equals(op.getOmitida()))
			return true;
		Collection<RevisionOperacionMonetaria> revisiones = op.getAprobaciones();
		if (revisiones == null || revisiones.isEmpty())
			return false;
		return revisiones.stream().allMatch(r -> r.isAprobada() || r.isOmitida());
	}

	private Long calcularMontoMaxPendiente(InstruccionMonetaria instr) {
		long max = 0L;
		for (OperacionMonetaria op : instr.getOperaciones()) {
			if (estaFinalizada(op))
				continue;
			Optional<CampoMonto> montoCampo = op.getCampos().stream()
					.filter(CampoMonto.class::isInstance)
					.map(CampoMonto.class::cast)
					.findFirst();
			if (montoCampo.isPresent()) {
				long v = montoCampo.get().getMonto()
						* (montoCampo.get().getTipoCambio() == 0
								? montoCampo.get().getDivisa().getTipoCambioMonedaNacional()
								: Long.valueOf(montoCampo.get().getTipoCambio()));
				if (v > max)
					max = v;
			}
		}
		return max;
	}

	private boolean verificarValidacion(OperacionMonetaria op, Rol rol) {
		boolean validacionRequerida = true;

		List<Rol> rolesRevisionGeneral = Arrays.asList(Rol.ROLE_GA, Rol.ROLE_GL);
		Optional<CampoMonto> montoCampo = op.getCampos().stream()
				.filter(CampoMonto.class::isInstance)
				.map(CampoMonto.class::cast)
				.findFirst();
		if (rolesRevisionGeneral.contains(rol)) {
			validacionRequerida = op.getTipoOperacion().isValidacionGeneral();
		} else if (rol.equals(Rol.ROLE_MC)) {
			validacionRequerida = op.getTipoOperacion().isValidacionMesaControl();
		} else if (montoCampo.isPresent()) {
			Long montoValidar = validacionSerice.obtenerNivelesAprobacion().get(rol);
			if (montoValidar != null) {
				validacionRequerida = op.getTipoOperacion().isValidacionMontos()
						&& (montoCampo.get().getMonto() >= montoValidar);
			} else {
				validacionRequerida = false;
			}
		}
		return validacionRequerida;
	}

	/* ===== Selección directa por LDAP (rol exacto / montos) ===== */

	private String seleccionarValidadorPorRolExacto(Rol rol, String region) {
		try {
			List<EmpleadoDto> validadores = empleadoService.buscarEmpleados(rol.name(), region);
			log.info("seleccionarValidadorPorRolExacto | rol={} region={} candidatos={}",
					rol, region, (validadores == null ? 0 : validadores.size()));
			if (validadores == null || validadores.isEmpty())
				return null;

			// Criterio simple: primero de la lista (puedes cambiar a round-robin, menos
			// cargado, etc.)
			EmpleadoDto e = validadores.get(0);
			// prefiero uid; si no, email
			String uid = e.getUid();
			String mail = e.getEmail();
			return (uid != null && !uid.isBlank()) ? uid : mail;
		} catch (Exception ex) {
			log.warn("seleccionarValidadorPorRolExacto | error: {}", ex.getMessage());
			return null;
		}
	}

	/**
	 * Busca el primer rol cuyo umbral aplique al monto y que tenga al menos un
	 * validador en la región.
	 * Si ese primer rol no tiene candidatos, prueba los roles superiores (por
	 * umbral) hasta encontrar uno.
	 */
	private String seleccionarValidadorPorPrimerRolPorMontos(long montoMN, String region) {
		try {
			Map<Rol, Long> niveles = validacionSerice.obtenerNivelesAprobacion();
			if (niveles == null || niveles.isEmpty())
				return null;

			List<Map.Entry<Rol, Long>> ordenados = niveles.entrySet().stream()
					.sorted(Comparator.comparingLong(Map.Entry::getValue))
					.collect(Collectors.toList());

			// tomar el primer rol cuyo umbral sea <= monto; si no hay, probar roles
			// superiores por orden
			for (int i = 0; i < ordenados.size(); i++) {
				Rol candidatoRol = ordenados.get(i).getKey();
				long umbral = ordenados.get(i).getValue();
				if (montoMN >= umbral) {
					// probar este y, si no hay, seguir con los siguientes
					for (int j = i; j < ordenados.size(); j++) {
						Rol rolAProbar = ordenados.get(j).getKey();
						String v = seleccionarValidadorPorRolExacto(rolAProbar, region);
						if (v != null)
							return v;
					}
				}
			}
			// Si ningún rol “aplica” por umbral, probar desde el menor hacia arriba por si
			// hay alguno disponible
			for (Map.Entry<Rol, Long> e : ordenados) {
				String v = seleccionarValidadorPorRolExacto(e.getKey(), region);
				if (v != null)
					return v;
			}
			return null;
		} catch (Exception ex) {
			log.warn("seleccionarValidadorPorPrimerRolPorMontos | error: {}", ex.getMessage());
			return null;
		}
	}

	@Transactional
	public List<RevisionOperacionMonetariaDto> omitirRevisionesOperacionMonetaria(
			Long operacionMonetariaId, String comentarios) {

		// CHECKMARX-FP: Operación segura — uso de JPA findById() con SQL parametrizado.
		// El valor 'operacionMonetariaId' se pasa como parámetro seguro en Spring Data JPA.
		Optional<OperacionMonetaria> operacionOptional = operacionMonetariaRepository.findById(operacionMonetariaId);
		if (operacionOptional.isEmpty()) {
			throw new RuntimeException("Operación monetaria con ID " + operacionMonetariaId + " no encontrada.");
		}

		OperacionMonetaria operacion = operacionOptional.get();
		List<RevisionOperacionMonetaria> revisiones = operacion.getAprobaciones();

		if (revisiones.isEmpty()) {
			System.err.println("Advertencia: No se encontraron revisiones para la operación monetaria con ID "
					+ operacionMonetariaId);
			return new ArrayList<>();
		}

		String usuarioActual = obtenerUsuarioActual();

		List<RevisionOperacionMonetaria> revisionesActualizadas = new ArrayList<>();
		for (RevisionOperacionMonetaria revision : revisiones) {
			revision.setOmitida(true);
			revision.setAprobada(false);
			revision.setComentarios(comentarios != null ? comentarios : "Omitida por sistema.");
			revision.setFechaRevision(LocalDateTime.now());
			revision.setValidadorEmail(usuarioActual);

			// CHECKMARX-FP: Operación segura — uso de JPA save() con SQL parametrizado.
			// El valor 'comentarios' no se usa en SQL dinámico. Persistencia gestionada por Spring Data JPA.
			revisionesActualizadas.add(revisionOperacionMonetariaRepository.save(revision));
		}

		actividadService.registrarActividad(
				operacion.getInstruccion().getFolio(),
				"Se omitieron " + revisionesActualizadas.size()
						+ " revisiones para la operación monetaria ID: " + operacionMonetariaId,
				usuarioActual);

		return revisionOperacionMonetariaMapper.toDto(revisionesActualizadas);
	}

	@Transactional
	public void eliminarRevisionesPorOperacionMonetariaIds(List<Long> operacionMonetariaIds) {
		if (operacionMonetariaIds == null || operacionMonetariaIds.isEmpty()) {
			log.warn("La lista de IDs de operaciones monetarias está vacía.");
			throw new IllegalArgumentException("La lista de IDs de operaciones monetarias no puede ser nula o vacia.");
		}
		String usuarioActual = obtenerUsuarioActual();
		revisionOperacionMonetariaRepository.deleteByOperacionMonetariaIdIn(operacionMonetariaIds);
		// podrías registrar actividad si aplica
		log.info("Revisiones eliminadas para las operaciones monetarias con IDs: {}", operacionMonetariaIds);
	}

	public List<FormatoBimDto> obtenerListaFormatosBim() {
		log.info("Obteniendo lista de formatos BIM");
		List<FormatoBimDto> formatos = new ArrayList<>();

		try (Stream<Path> stream = Files.list(Paths.get(RUTA_CARPETA))) {
			formatos = stream.filter(Files::isRegularFile)
					.map(Path::getFileName)
					.map(Path::toString)
					.map(nombreArchivo -> {
						FormatoBimDto formato = new FormatoBimDto();
						formato.setNombre(nombreArchivo);
						return formato;
					})
					.collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Error al leer la carpeta: {}", e.getMessage());
		}

		return formatos;
	}

	public List<ListaArchivosJuridicaDto> obtenerListaArchivos() {

		List<ListaArchivosJuridica> listaArchivosJuridicas = listaArchivosJuridicaRespository.findAll();
		return listaArchivosJuridicaMapper.toDto(listaArchivosJuridicas);
	}

	public String obtenerArchivosSugeridos(String cve) {
		return listaArchivosJuridicaRespository.findArchivosSujeridosPorTipoOperacionJuridica(cve);
	}

	public List<InstruccionJuridicaDto> getInstruccionesParaRevision(String rol) {
		List<ConfiguracionOperacionJuridica> configs = configuracionOperacionJuridicaRepository
				.findByRolAndRevision(rol, true);
		List<String> tiposPermitidos = configs.stream()
				.map(ConfiguracionOperacionJuridica::getTipo_operacion_juridica_cve)
				.collect(Collectors.toList());

		List<OperacionJuridica> operaciones = operacionJuridicaRepository
				.findByTipoOperacionJuridica_CveInAndEstatus_Cve(tiposPermitidos, "PE");

		List<InstruccionJuridica> instruccionesUnicas = operaciones.stream()
				.map(OperacionJuridica::getInstruccion)
				.distinct()
				.collect(Collectors.toList());

		return instruccionesUnicas.stream()
				.map(this::mapToInstruccionJuridicaDto)
				.collect(Collectors.toList());
	}

	public List<InstruccionJuridicaDto> getInstruccionesParaAprobacion(String rol) {

		List<ConfiguracionOperacionJuridica> configs = configuracionOperacionJuridicaRepository
				.findByRolAndAprobacion(rol, true);
		List<String> tiposPermitidos = configs.stream()
				.map(ConfiguracionOperacionJuridica::getTipo_operacion_juridica_cve)
				.collect(Collectors.toList());

		List<OperacionJuridica> operaciones = operacionJuridicaRepository
				.findByTipoOperacionJuridica_CveInAndEstatus_Cve(tiposPermitidos, "REV");

		List<InstruccionJuridica> instruccionesUnicas = operaciones.stream()
				.map(OperacionJuridica::getInstruccion)
				.distinct()
				.collect(Collectors.toList());

		return instruccionesUnicas.stream()
				.map(this::mapToInstruccionJuridicaDto)
				.collect(Collectors.toList());
	}

	private InstruccionJuridicaDto mapToInstruccionJuridicaDto(InstruccionJuridica instruccion) {
		InstruccionJuridicaDto dto = new InstruccionJuridicaDto();
		dto.setFolio(instruccion.getFolio());
		dto.setResponsable(instruccion.getResponsable());
		dto.setUrgente(instruccion.isUrgente());
		dto.setOperadaParcialmente(instruccion.isOperadaParcialmente());
		dto.setEstatus(instruccion.getEstatus());
		dto.setFechaModificacion(instruccion.getFechaModificacion());
		dto.setInstruccion(new InstruccionDto());

		List<OperacionJuridicaDto> operacionesDto = instruccion.getOperaciones().stream()
				.map(this::mapToOperacionJuridicaDto)
				.collect(Collectors.toList());
		dto.setOperaciones(operacionesDto);

		return dto;
	}

	private OperacionJuridicaDto mapToOperacionJuridicaDto(OperacionJuridica operacion) {
		OperacionJuridicaDto dto = new OperacionJuridicaDto();
		dto.setId(operacion.getId());
		dto.setFechaRegistro(operacion.getFechaRegistro());
		dto.setDescripcionOperacion(operacion.getDescripcionOperacion());
		dto.setComentario(operacion.getComentario());
		dto.setObservaciones(operacion.getObservaciones());
		dto.setClienteCorreoElectronico(operacion.getClienteCorreoElectronico());
		dto.setInstruccionCumpleFines(operacion.isInstruccionCumpleFines());
		dto.setBoolClienteEmail(operacion.isBoolClienteEmail());
		dto.setFirmasCorrectas(operacion.isFirmasCorrectas());
		dto.setEstatusCve(operacion.getEstatus().getCve());

		if (operacion.getTipoOperacionJuridica() != null) {
			dto.setTipoOperacionJuridica(operacion.getTipoOperacionJuridica());
		}
		return dto;
	}

	public List<InstruccionJuridicaDto> getInstruccionesPendientes(String rol) {
		List<ConfiguracionOperacionJuridica> configsRevision = configuracionOperacionJuridicaRepository
				.findByRolAndRevision(rol, true);

		if (!configsRevision.isEmpty()) {
			List<String> tiposPermitidos = configsRevision.stream()
					.map(ConfiguracionOperacionJuridica::getTipo_operacion_juridica_cve)
					.collect(Collectors.toList());

			List<OperacionJuridica> operaciones = operacionJuridicaRepository
					.findByTipoOperacionJuridica_CveInAndEstatus_Cve(tiposPermitidos, "PE");

			List<InstruccionJuridica> instruccionesUnicas = operaciones.stream()
					.map(OperacionJuridica::getInstruccion)
					.distinct()
					.collect(Collectors.toList());

			return instruccionesUnicas.stream()
					.map(this::mapToInstruccionJuridicaDto)
					.collect(Collectors.toList());
		}

		List<ConfiguracionOperacionJuridica> configsAprobacion = configuracionOperacionJuridicaRepository
				.findByRolAndAprobacion(rol, true);

		if (!configsAprobacion.isEmpty()) {
			List<String> tiposPermitidos = configsAprobacion.stream()
					.map(ConfiguracionOperacionJuridica::getTipo_operacion_juridica_cve)
					.collect(Collectors.toList());

			List<OperacionJuridica> operaciones = operacionJuridicaRepository
					.findByTipoOperacionJuridica_CveInAndEstatus_Cve(tiposPermitidos, "REV");

			List<InstruccionJuridica> instruccionesUnicas = operaciones.stream()
					.map(OperacionJuridica::getInstruccion)
					.distinct()
					.collect(Collectors.toList());

			return instruccionesUnicas.stream()
					.map(this::mapToInstruccionJuridicaDto)
					.collect(Collectors.toList());
		}
		return List.of();
	}

	public void actualizarOperacion(Long idOperacion, RevisionComprobanteDto revision) {

		// CHECKMARX-FP: Operación segura — uso de JPA findById() con SQL parametrizado.
		// El valor 'idOperacion' se pasa como parámetro seguro en Spring Data JPA.
		Optional<OperacionMonetaria> operacion = operacionMonetariaRepository.findById(idOperacion);

		if (operacion.isPresent()) {
			operacion.get().setFechaRevision(LocalDateTime.now());
			operacion.get().setAprobada(revision.getAprobada());
			operacion.get().setObservacion(revision.getObservacion());
			// CHECKMARX-FP: Operación segura — uso de JPA save() con entidad validada. No hay SQL dinámico ni concatenación de datos del request.
			operacionMonetariaRepository.save(operacion.get());
		} else {
			// CHECKMARX-FP: Operación segura — uso de JPA findById() con SQL parametrizado.
			// El valor 'idOperacion' se pasa como parámetro seguro en Spring Data JPA.
			OperacionJuridica operation = operacionJuridicaRepository.findById(idOperacion).orElseThrow();
			operation.setFechaRevision(LocalDateTime.now());
			operation.setAprobada(revision.getAprobada());
			operation.setObservaciones(revision.getObservacion());
			// CHECKMARX-FP: Safe operation — using JPA repository 'operacionJuridicaRepository.save()' with parameterized queries.
			// The 'revision' data is persisted through ORM mapping without dynamic SQL or string concatenation.
			operacionJuridicaRepository.save(operation);

		}
	}

	// --- Helpers reutilizables ---
	private Integer resolveTipoCambio(OperacionMonetariaRequestDto o) {

		if (o.getMonto() != null && o.getMonto().getDivisa() != null
				&& o.getMonto().getDivisa().getTipoCambioMonedaNacional() >= 0) {
			return o.getMonto().getDivisa().getTipoCambioMonedaNacional();
		}
		if (o.getCuentaCargo() != null && o.getCuentaCargo().getDivisa() != null
				&& o.getCuentaCargo().getDivisa().getTipoCambioMonedaNacional() >= 0) {
			return o.getCuentaCargo().getDivisa().getTipoCambioMonedaNacional();
		}
		if (o.getCuentaAbono() != null && o.getCuentaAbono().getDivisa() != null
				&& o.getCuentaAbono().getDivisa().getTipoCambioMonedaNacional() >= 0) {
			return o.getCuentaAbono().getDivisa().getTipoCambioMonedaNacional();
		}
		return 0;
	}

	public List<OperacionJuridicaDto> obtenerOperacionesJuridicas(
			LocalDate desde, LocalDate hasta, String userEmail,
			OperacionJuridicaDto filtro, int pagina, int tamanio, String ordenarPor) {

		// Armar filtro como entidad (evita NPE si viene null)
		OperacionJuridica filtroEntity = (filtro != null)
				? operacionJuridicaMapper.toEntity(filtro)
				: new OperacionJuridica();

		Set<String> regiones = empleadoService.buscarEmpleado(userEmail)
				.getRegiones().stream().map(r -> r.getCve()).collect(Collectors.toSet());
		
		if (!Validators.camposPermitidos.contains(ordenarPor)) {
		    ordenarPor = "id";
		}

		List<OperacionJuridica> operaciones = reportRepository.buscarOperacionesJuridicasPorMuestra(
				filtroEntity,
				desde,
				hasta,
				regiones,
				PageRequest.of(pagina, tamanio, Sort.by(ordenarPor)));

		return operacionJuridicaMapper.toDto(operaciones);
	}

	private String normCve(String cve) {
		if (cve == null || cve.isBlank())
			return null;
		return "MXN".equalsIgnoreCase(cve) ? "MXP" : cve;
	}

	// Divisa de PAGO: monto o cuentaCargo
	private String resolveDivisaCvePago(OperacionMonetariaRequestDto o) {
		String cve = null;
		if (o.getMonto() != null && o.getMonto().getDivisa() != null) {
			cve = o.getMonto().getDivisa().getCve();
		} else if (o.getCuentaCargo() != null && o.getCuentaCargo().getDivisa() != null) {
			cve = o.getCuentaCargo().getDivisa().getCve();
		}
		cve = normCve(cve);
		return (cve == null || cve.isBlank()) ? "MXP" : cve;

	}

	// Divisa de COMPRA: cuentaAbono obligatoriamente
	private String resolveDivisaCveCompra(OperacionMonetariaRequestDto o) {
		String cve = null;
		if (o.getCuentaAbono() != null && o.getCuentaAbono().getDivisa() != null) {
			cve = o.getCuentaAbono().getDivisa().getCve();
		}
		cve = normCve(cve);
		return (cve == null || cve.isBlank()) ? "MXP" : cve;
	}

	public void finalizarOperacionJuridica(long id, OperacionJuridicaDto dto) {
		// CHECKMARX-FP: Operación segura — uso de JPA findById() con SQL parametrizado.
		// El valor 'id' no se utiliza en SQL dinámico.
		// Spring Data JPA genera consultas parametrizadas, evitando inyección SQL.
		OperacionJuridica operation = operacionJuridicaRepository.findById(id).orElseThrow();
		operation.setFinalizada(true);
		operation.setFechaEcritura(dto.getFechaEcritura());
		operation.setFechaMantenimiento(dto.getFechaMantenimiento());
		operation.setNumeroEscritura(dto.getNumeroEscritura());
		operation.setComentario(dto.getComentario());
		operation.setSecuenciaCartaComplemento(dto.getSecuenciaCartaCOmplemento());
		
		// CHECKMARX-FALSE-POSITIVE: Operación de guardado segura con JPA. 
	    // Motivo: No se usa SQL dinámico ni concatenación de parámetros del usuario.
		operacionJuridicaRepository.saveAndFlush(operation);

		InstruccionJuridica ij = instruccionJuridicaRepository
				.findByInstruccionFolio(operation.getInstruccion().getFolio()).orElseThrow();

		if (ij.getOperaciones().stream()
				.filter(o -> o.getFinalizada() != null && o.getFinalizada())
				.collect(Collectors.toSet()).isEmpty()) {
			EstatusInstruccion e = new EstatusInstruccion();
			e.setCve(EstatusInstruccionEnum.FI.toString());
			ij.setEstatus(e);
			// CHECKMARX-FP: Operación segura — usando JPA repository 'instruccionJuridicaRepository.save()'.
			// La entidad 'ij' proviene de un DTO y es persistido via ORM sin ningun SQL dinamico o concatenado.
			instruccionJuridicaRepository.save(ij);
		}
	}
}