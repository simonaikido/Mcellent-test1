package com.bim.seif.services;

import com.bim.seif.models.*;
import com.bim.seif.models.dto.*;
import com.bim.seif.models.mappers.InstruccionMapper;
import com.bim.seif.models.mappers.OperacionMonetariaMapper;
import com.bim.seif.models.mappers.RevisionOperacionMonetariaMapper;
import com.bim.seif.models.mappers.TipoOperacionMonetariaMapper;
import com.bim.seif.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.InvalidNameException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstruccionService {

    private final InstruccionRepository instruccionRepository;
    private final InstruccionMonetariaRepository monetariaRepository;
    private final InstruccionJuridicaRepository juridicaRepository;
    private final EmpleadoService empleadoService;
    private final TipoOperacionMonetariaRepository tipoOperacionMonetariaRepository;
    private final RevisionOperacionMonetariaRepository revisionOperacionMonetariaRepository;
    private final ValidacionSerice validacionSerice;
    private final EstatusInstruccionRepository estatusInstruccionRepository;
    private final OperacionMonetariaRepository operacionMonetariaRepository;

    // Mappers inyectados (MapStruct con componentModel="spring")
    private final InstruccionMapper instruccionMapper;
    private final OperacionMonetariaMapper operacionMonetariaMapper;
    private final RevisionOperacionMonetariaMapper revisionOperacionMonetariaMapper;
    private final TipoOperacionMonetariaMapper tipoOperacionMonetariaMapper;

    private final OperacionJuridicaService operacionJuridicaService;
    private final OperacionJuridicaRepository operacionJuridicaRepository;
    private final EmailService emailService;
    private final ClienteRespository clienteRespository;

    private final CuentaService cuentaService;

    public List<InstruccionDto> obtenerInstruccionesSinClasificar(String username)
            throws UsernameNotFoundException, InvalidNameException {

        log.info("Obtener instrucciones sin clasificar para usuario: {}", username);

        EmpleadoDto e = empleadoService.loadUserByUsername(username);
        Set<String> regiones = e.getRegiones().stream().map(RegionDto::getCve).collect(Collectors.toSet());

        List<Instruccion> instrucciones = instruccionRepository.findInstruccionesWithoutSubtipe(regiones);

        log.info("Se recuperaron {} instrucciones sin clasificar.", instrucciones.size());
        return instruccionMapper.instruccionToInstruccionDTOList(instrucciones);
    }

    public void calsificarInstruccion(String folioIstruccion, String responsableMonetaria, String responsableJuridica) {

        log.info("Clasificación de instrucción {}. R.Mon: {}, R.Jur: {}", folioIstruccion, responsableMonetaria,
                responsableJuridica);

        Optional<Instruccion> i = instruccionRepository.findById(folioIstruccion);

        if (i.isPresent()) {
            if (responsableMonetaria != null) {
                InstruccionMonetaria im = new InstruccionMonetaria();
                im.setInstruccion(i.get());
                im.setResponsable(responsableMonetaria);
                im.setProgramada(false);
                im.setEstatus(new EstatusInstruccion(EstatusInstruccionEnum.PR.name()));
                monetariaRepository.save(im);
                log.debug("Clasificada como Monetaria y asignada a responsable: {}", responsableMonetaria);
            }

            if (responsableJuridica != null) {
                InstruccionJuridica juridica = new InstruccionJuridica();
                juridica.setInstruccion(i.get());
                juridica.setResponsable(responsableJuridica);
                juridica.setSolicitudCorreccion(false);
                juridica.setEstatus(new EstatusInstruccion(EstatusInstruccionEnum.PR.name()));
                juridicaRepository.save(juridica);
                log.debug("Clasificada como Jurídica y asignada a responsable: {}", responsableJuridica);
            }
        } else {
            log.warn("No se encontró la instrucción con folio {} para clasificar.", folioIstruccion);
        }

        log.info("Clasificación de instrucción {} completada.", folioIstruccion);
    }

    public List<InstruccionMonetariaDto> obtenerInstruccionesMonetariasPorValidador(String validadorEmail) {

        log.info("Obtener instrucciones monetarias pendientes de validación para: {}", validadorEmail);

        List<InstruccionMonetaria> monetarias = monetariaRepository
                .findByValidadorEmailAndFechaAprobacionIsNull(validadorEmail);

        log.info("Se recuperaron {} instrucciones pendientes de validar.", monetarias.size());
        return instruccionMapper.instruccionMonetariaToInstruccionMonetariaDTOList(monetarias);
    }

    public List<InstruccionMonetariaDto> obtenerInstruccionesMonetariasMesaControl() {

        log.info("Obtener instrucciones monetarias para Mesa de Control.");

        List<InstruccionMonetaria> monetarias = monetariaRepository.findByMesaControlTrue();

        log.info("Se recuperaron {} instrucciones para Mesa de Control.", monetarias.size());
        return instruccionMapper.instruccionMonetariaToInstruccionMonetariaDTOList(monetarias);
    }

    @Transactional(readOnly = true)
    public List<InstruccionMonetariaDto> obtenerInstruccionesMonetarias(String email) {

        log.info("Obtener instrucciones monetarias activas para empleado: {}", email);

        // 1) Trae las instrucciones
        List<InstruccionMonetaria> monetarias = monetariaRepository
                .findMonetariasByEmpleadoEmailAndEstatusCveAndNoOperations(email);

        // 2) Mapea a DTO
        List<InstruccionMonetariaDto> salida = instruccionMapper
                .instruccionMonetariaToInstruccionMonetariaDTOList(monetarias);

        log.debug("Instrucciones monetarias mapeadas: {}", salida.size());

        // 3) Enriquecer solicitudCuenta usando el service de cuentas (folio corto)
        for (InstruccionMonetariaDto imDto : salida) {
            String folio = imDto.getFolio();
            List<OperacionMonetariaDto> ops = imDto.getOperaciones();
            if (folio == null || ops == null || ops.isEmpty())
                continue;

            String folioFideicomiso = folio.length() > 8 ? folio.substring(0, 8) : folio;

            // Service que ya devuelve bien el boolean
            List<CuentaAbonoDto> cuentas = cuentaService.obtenerCuentas(folioFideicomiso);

            // Index por cuenta
            Map<Long, Boolean> porCuenta = new HashMap<>();
            if (cuentas != null) {
                for (CuentaAbonoDto c : cuentas) {
                    if (c != null && c.getCuenta() != null) {
                        porCuenta.put(c.getCuenta(), Boolean.TRUE.equals(c.getSolicitudCuenta()));
                    }
                }
            }
            // Inyecta el valor en cada operación, pero sin filtrar ni alterar nada más
            for (OperacionMonetariaDto opDto : ops) {
                if (opDto == null || opDto.getCuentaAbono() == null || opDto.getCuentaAbono().getCuentaAbono() == null)
                    continue;

                Long cuentaOp = opDto.getCuentaAbono().getCuentaAbono().getCuenta();
                Boolean val = (cuentaOp != null) ? porCuenta.get(cuentaOp) : null;

                if (Boolean.TRUE.equals(val)) {
                    // Solo si realmente está en true, lo seteamos
                    opDto.getCuentaAbono().getCuentaAbono().setSolicitudCuenta(true);
                    log.trace("SERVICE: Inyectado solicitudCuenta=true para operación: {}", opDto.getId());
                }
                // Si es null o false, dejamos lo que ya estaba en el DTO (normalmente false)
            }
        }

        log.info("Obtencion y enriquecimiento de instrucciones monetarias completado. Total: {}", salida.size());
        return salida;
    }

    public List<InstruccionJuridicaDto> obtenerInstruccionesJuridicas(String email) throws InterruptedException {

        log.info("Obtener instrucciones jurídicas activas para empleado: {}", email);

        // Ya devuelve DTOs
        List<InstruccionJuridicaDto> juridicas = juridicaRepository.findInstruccionesJuridicas(email);

        // Agregar operaciones/archivos como ya lo haces
        for (InstruccionJuridicaDto instruccion : juridicas) {
            List<OperacionJuridicaDto> operaciones = operacionJuridicaService
                    .obtenerOperacionesJuridicas(instruccion.getFolio());
            for (OperacionJuridicaDto ojd : operaciones) {
                List<SolicitudArchivoJuridicaDto> solicitudesArchivos = operacionJuridicaService
                        .obtenerArchivosSolicitadosPorOperacion(ojd.getId());
                ojd.setSolicitudArchivoJuridica(solicitudesArchivos);
            }
            instruccion.setOperaciones(operaciones);
        }

        log.info("Obtención y enriquecimiento de instrucciones jurídicas completado. Total: {}", juridicas.size());
        // Devolvemos directo la lista de DTOs
        return juridicas;
    }

    public List<InstruccionJuridicaDto> obtenerInstruccionesJuridicas(String email, boolean proceso)
            throws InterruptedException {

        log.info("Obtener instrucciones jurídicas en proceso (Estatus != PR) para empleado: {}", email);

        // ¡Cambiado para usar el nuevo método del repositorio!
        List<InstruccionJuridica> juridicas = juridicaRepository.findByResponsableAndEstatusCveNotLike(email, "PR");

        log.info("Se recuperaron {} instrucciones jurídicas en proceso.", juridicas.size());
        // (comentado en tu código original)
        return instruccionMapper.instruccionJuridicaToInstruccionJuridicaDTOListTwo(juridicas);
    }

    public Instruccion obtenerInstruccionPorFolio(String folio) {
        log.info("Buscando Instrucción por folio: {}", folio);
        return instruccionRepository.findById(folio).get();
    }

    // Método para consulta-instruccion (Otros estatus O PR con operaciones)
    public List<InstruccionMonetariaDto> obtenerInstruccionesMonetariasConOtrosEstatus(String email) {

        log.info("Obtener instrucciones monetarias para consulta (Estatus != PR o PR con Ops) para: {}", email);

        // Base: las que sí tienen responsable (tu query actual)
        List<InstruccionMonetaria> base = monetariaRepository.findMonetariasForConsulta(email,
                EstatusInstruccionEnum.PR.name());

        // Extra: rechazadas sin responsable
        List<InstruccionMonetaria> rechazadasSinResp = monetariaRepository
                .findRechazadasSinResponsable(EstatusInstruccionEnum.RE.name());

        // Merge sin duplicados, usando folio como clave
        Map<String, InstruccionMonetaria> merged = new LinkedHashMap<>();
        for (InstruccionMonetaria im : base) {
            merged.put(im.getFolio(), im);
        }
        for (InstruccionMonetaria im : rechazadasSinResp) {
            merged.putIfAbsent(im.getFolio(), im);
        }

        log.info("Se recuperaron {} instrucciones monetarias para consulta (después de merge).", merged.size());
        return instruccionMapper.instruccionMonetariaToInstruccionMonetariaDTOList(
                new ArrayList<>(merged.values()));
    }

    public List<InstruccionMonetariaDto> obtenerInstrucciones(String email) {

        log.info("Obtener todas las instrucciones monetarias de las regiones del ejecutivo: {}", email);

        EmpleadoDto empleado = empleadoService.buscarEmpleado(email);

        Set<String> regiones = empleado.getRegiones().stream().map(RegionDto::getCve).collect(Collectors.toSet());

        List<InstruccionMonetaria> monetarias = monetariaRepository.findByInstruccionFideicomisoRegionCveIn(regiones);

        log.info("Se recuperaron {} instrucciones monetarias por región.", monetarias.size());
        return instruccionMapper.instruccionMonetariaToInstruccionMonetariaDTOList(monetarias);
    }

    // Nuevo metodo para actualizar el estatus de InstruccionMonetaria
    @Transactional
    public void actualizarEstatusInstruccionMonetaria(String folioInstruccion, String nuevoEstatus) {

        log.info("Actualizar estatus de InstruccionMonetaria {}. Nuevo Estatus: {}", folioInstruccion, nuevoEstatus);

        final String est = Objects.requireNonNull(nuevoEstatus, "nuevoEstatus").trim().toUpperCase();

        Instruccion instruccion = instruccionRepository.findById(folioInstruccion)
                .orElseThrow(
                        () -> new IllegalArgumentException("No existe Instrucción con folio: " + folioInstruccion));

        LocalDateTime ahora = LocalDateTime.now();
        switch (est) {
            case "RE":
                instruccion.setFechaRechazo(ahora);
                log.debug("Actualizando fechaRechazo para Instruccion: {}", folioInstruccion);
                break;
            case "CA":
            case "PE":
                break;
            case "FI": //Finalizada
                try {
                    instruccion.setFechaAprobacion(ahora);
                    log.debug("Actualizando fechaAprobacion para Instruccion: {}", folioInstruccion);
                } catch (Throwable ignore) {
                    // si el campo no existe en algunos entornos, no romper
                    log.warn("No se pudo setear fechaAprobacion en Instruccion {} (campo no existe/ignore).",
                            folioInstruccion);
                }
                break;
            default:
                break;
        }
        instruccionRepository.save(instruccion);

        InstruccionMonetaria im = monetariaRepository.findById(folioInstruccion)
                .orElseGet(() -> {
                    log.debug("InstruccionMonetaria {} no existía, creando nueva instancia.", folioInstruccion);
                    var n = new InstruccionMonetaria();
                    n.setInstruccion(instruccion);
                    return n;
                });

        EstatusInstruccion estatus = estatusInstruccionRepository.findByCve(est);
        if (estatus == null) {
            log.error(" Estatus no válido: {}", est);
            throw new IllegalArgumentException("Estatus no válido: " + est);
        }

        im.setValidadorEmail("");
        im.setEstatus(estatus);

        if ("RE".equals(est)) {
            log.debug("Marcando operaciones y revisiones como omitidas por rechazo (RE) de {}", folioInstruccion);
            // (si tu IM tiene fechaCancelacion)
            try {
                im.setFechaCancelacion(ahora);
            } catch (Throwable ignore) {
            }

            // 1) Trae TODAS las operaciones de la instrucción
            List<OperacionMonetaria> ops = operacionMonetariaRepository.findAllByInstruccion_Folio(folioInstruccion);

            // 2) Marca todas las operaciones como omitidas (si existe el boolean en tu
            // OperacionMonetaria)
            if (!ops.isEmpty()) {
                for (OperacionMonetaria op : ops)
                    op.setOmitida(true);
                operacionMonetariaRepository.saveAll(ops);

                // 3) Omitir TODAS las revisiones de esas operaciones (masivo y sin tronar si no
                // hay)
                List<Long> ids = ops.stream().map(OperacionMonetaria::getId).toList();
                if (!ids.isEmpty()) {
                    revisionOperacionMonetariaRepository.marcarOmitidasPorOperacionIds(ids);
                }
                log.debug("{} operaciones y sus revisiones marcadas como omitidas.", ops.size());
            }
            notificacionEmailRechazo(instruccion, "Monetaria");
        }

        monetariaRepository.save(im);
        log.info("Estatus de InstruccionMonetaria {} actualizado a {}.", folioInstruccion, est);        
    }

    public List<InstruccionMonetariaDto> obtenerInstruccionesConComprobates() {

        log.info("Obtener instrucciones aprobadas con comprobantes pendientes de revisión.");

        List<InstruccionMonetaria> monetarias = monetariaRepository.findByFechaAprobacionIsNotNull();

        log.debug("Instrucciones aprobadas encontradas: {}", monetarias.size());

        for (InstruccionMonetaria i : monetarias) {
            i.setOperaciones(i.getOperaciones().stream()
                    .filter(o -> o.getFechaRevision() == null && o.getOmitida() == null)
                    .collect(Collectors.toList()));
        }

        List<InstruccionMonetaria> filtradas = monetarias.stream()
                .filter(m -> (m.getOperaciones().stream().allMatch(o -> o.getComprobante() != null))
                        && (m.getOperaciones().stream().anyMatch(o -> o.getFechaRevision() == null)))
                .collect(Collectors.toList());

        log.info("Se recuperaron {} instrucciones con comprobantes para revisión.", filtradas.size());
        return instruccionMapper.instruccionMonetariaToInstruccionMonetariaDTOList(filtradas);
    }

    public void actualizarEstatusInstruccionJuridica(String folioInstruccion, EstatusInstruccionEnum nuevoEstatus) {

        log.info("Actualizar estatus de InstruccionJuridica {}. Nuevo Estatus: {}", folioInstruccion,
                nuevoEstatus.name());

        InstruccionJuridica instruccionJuridica = juridicaRepository.findById(folioInstruccion)
                .orElseThrow(
                        () -> new IllegalArgumentException("No existe InstruccionJuridica con folio: " + folioInstruccion));

        EstatusInstruccion estatus = new EstatusInstruccion();
        estatus.setCve(nuevoEstatus.name());
        instruccionJuridica.setEstatus(estatus);
        juridicaRepository.save(instruccionJuridica);

        switch (nuevoEstatus.name()) {
            case "RE":
                Instruccion instruccion = instruccionRepository.findInstruccionesByFolio(folioInstruccion)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "No existe Instruccion con folio: " + folioInstruccion));
                instruccion.setFechaRechazo(LocalDateTime.now());
                instruccionRepository.save(instruccion);

                List<OperacionJuridica> operaciones = operacionJuridicaRepository
                        .findByInstruccionFolio(instruccion.getFolio());

                for (OperacionJuridica operacion : operaciones) {
                    operacion.setEstatus(new EstatusOperacion("RE"));
                    operacionJuridicaRepository.save(operacion);
                }
                notificacionEmailRechazo(instruccion, "No Monetaria");
                break;
        }

        log.info("Estatus de InstruccionJuridica {} actualizado a {}.", folioInstruccion, nuevoEstatus.name());
    }

    private void notificacionEmailRechazo(Instruccion instruccion, String instruccionTipo){
        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                Cliente client = clienteRespository.findByEmail(instruccion.getClienteCarga());
                emailService.enviarCorreo(instruccion.getClienteCarga(),
                TipoEvento.instruccion_rechazo,
                Map.of(Propiedad.instruccion_folio, instruccion.getFolio(),
                Propiedad.instruccion_cliente_email, client.getNombre() + " " + client.getApellidoPaterno() + " " + client.getApellidoMaterno(),
                Propiedad.instruccion_fecha_recepcion, instruccion.getFechaAlta().format(formatterDate),
                Propiedad.fideicomiso_folio, instruccion.getFideicomiso().getFolio(),
                Propiedad.fideicomiso_alias, instruccion.getFideicomiso().getAlias(),
                Propiedad.instruccion_tipo, instruccionTipo));
    }

    public void actualizarInstruccionJuridica(String folioInstruccion, LocalDateTime fechaEcritura,
            Long numeroEscritura, LocalDateTime fechaMantenimiento,
            Long secuenciaCartaCOmplemento) {

        log.info("Actualizar metadatos de InstruccionJuridica {}.", folioInstruccion);

        InstruccionJuridica instruccion = juridicaRepository.findByInstruccionFolio(folioInstruccion)
                .orElseThrow(
                        () -> new IllegalArgumentException("No existe Instrucción con folio: " + folioInstruccion));

        // Note: Logic to update the fields (fechaEcritura, numeroEscritura, etc.) is
        // missing here in the original code.

        juridicaRepository.save(instruccion);
        log.info("Metadatos de InstruccionJuridica {} actualizados.", folioInstruccion);
    }

    /**
     * Reclasifica una Instruccion monetaria, eliminándola de la tabla
     * INSTRUCION_MONETARIA.
     * Si no tiene otra clasificación (ej. jurídica), volverá a aparecer en
     * Instrucciones Sin Clasificar.
     *
     * @param folioInstruccion El folio de la instrucción a reclasificar.
     */
    @Transactional
    public void reclasificarInstruccionMonetaria(String folioInstruccion) {

        log.info("Reclasificar/eliminar InstruccionMonetaria con folio: {}", folioInstruccion);

        Optional<InstruccionMonetaria> instruccionMonetariaOptional = monetariaRepository
                .findByInstruccionFolio(folioInstruccion);

        if (instruccionMonetariaOptional.isPresent()) {
            monetariaRepository.borrar(folioInstruccion);
            log.debug("InstruccionMonetaria {} eliminada de la tabla INSTRUCION_MONETARIA.", folioInstruccion);

            // Intenta buscarla inmediatamente después de la eliminación en la misma sesión
            Optional<InstruccionMonetaria> instruccionMonetariaAfterDelete = monetariaRepository
                    .findByInstruccionFolio(folioInstruccion);

            if (!instruccionMonetariaAfterDelete.isPresent()) {
                log.debug("InstruccionMonetaria {} no encontrada en la sesión después de delete (OK).",
                        folioInstruccion);
                // OK, ya no está en sesión
            } else {
                System.err.println("InstruccionMonetaria con folio " + folioInstruccion
                        + " AÚN ENCONTRADA en la sesión después de delete (FALLÓ a nivel de sesión).");
                log.error("InstruccionMonetaria con folio {} AÚN ENCONTRADA después de delete.", folioInstruccion);
            }

        } else {
            System.out.println("No se encontró InstruccionMonetaria para el folio: "
                    + folioInstruccion + " para reclasificar.");
            log.warn("No se encontró InstruccionMonetaria para el folio: {} para reclasificar.", folioInstruccion);
        }

        log.info("Reclasificación de InstruccionMonetaria {} completada.", folioInstruccion);
    }

    /************/

    public void asignarValidador(List<OperacionMonetaria> operacionesPersistidas, InstruccionMonetaria im) {

        log.info("Asignación de validador para InstruccionMonetaria: {}", im.getFolio());

        // operacion terminal y de cortocircuito
        boolean validacionGeneral = obtenerTiposOperacionMonetaria().stream()
                .filter(TipoOperacionMonetariaDto::isValidacionGeneral)
                .map(TipoOperacionMonetariaDto::getCve)
                .anyMatch(operacionesPersistidas.stream()
                        .map(om -> om.getTipoOperacion().getCve())
                        .collect(Collectors.toList())::contains);

        if (validacionGeneral) {
            String validadorEmail = validacionSerice
                    .buscarValidadorGeneral(im.getInstruccion().getFideicomiso().getRegion().getCve(), Rol.ROLE_EA);
            im.setValidadorEmail(validadorEmail);
            monetariaRepository.save(im);
            log.info("Validador GENERAL asignado: {}", validadorEmail);

        } else {
            boolean validacionMontos = obtenerTiposOperacionMonetaria().stream()
                    .filter(TipoOperacionMonetariaDto::isValidacionMontos)
                    .map(TipoOperacionMonetariaDto::getCve)
                    .anyMatch(operacionesPersistidas.stream()
                            .map(om -> om.getTipoOperacion().getCve())
                            .collect(Collectors.toList())::contains);

            List<OperacionMonetariaDto> op = operacionMonetariaMapper.toDto(operacionesPersistidas);

            OptionalLong montoMaximo = op.stream()
                    .mapToLong(o -> {
                        System.out.println(">> MONTO : " + o.getMonto());
                        if (o.getMonto() == null) {
                            return 0L;
                        }
                        return o.getMonto().getMonto();
                    }).max();

            log.debug("Monto máximo encontrado para validación: {}", montoMaximo.orElse(0L));

            if (validacionMontos) {
                String validadorEmail = validacionSerice.buscarValidadorMontos(
                        im.getInstruccion().getFideicomiso().getRegion().getCve(), Rol.ROLE_EA, montoMaximo.orElse(0L));
                im.setValidadorEmail(validadorEmail);
                monetariaRepository.save(im);
                log.info("Validador por MONTOS asignado: {}", validadorEmail);
            } else {
                boolean validacionMesaControl = obtenerTiposOperacionMonetaria().stream()
                        .filter(TipoOperacionMonetariaDto::isValidacionMesaControl)
                        .map(TipoOperacionMonetariaDto::getCve)
                        .anyMatch(operacionesPersistidas.stream()
                                .map(om -> om.getTipoOperacion().getCve())
                                .collect(Collectors.toList())::contains);
                System.out.println(validacionMesaControl);

                if (validacionMesaControl) {
                    im.setMesaControl(true);
                    monetariaRepository.save(im);
                    log.info("Asignado a MESA DE CONTROL.");
                } else {
                    // no requiere aprobaciones
                    im.setFechaAprobacion(LocalDateTime.now());
                    monetariaRepository.save(im);
                    log.info("No requiere aprobaciones. Aprobación automática a: {}", im.getFechaAprobacion());
                }
            }
        }
    }

    private List<TipoOperacionMonetariaDto> obtenerTiposOperacionMonetaria() {
        log.debug("Buscando todos los TiposOperacionMonetaria.");
        List<TipoOperacionMonetaria> tipos = tipoOperacionMonetariaRepository.findAll();
        return tipoOperacionMonetariaMapper.toDto(tipos);
    }

    public void cancelarInstruccion(Instruccion instruccion) {
        log.info("Guardando instrucción cancelada: {}", instruccion.getFolio());
        instruccionRepository.save(instruccion);
    }

    public List<InstruccionJuridicaDto> obtenerInstruccionesJuridicas(InstruccionJuridicaDto filtro,
            int pagina, int tamanio, String ordenarPor) {

        log.info("Búsqueda paginada de Instrucciones Jurídicas. Pág: {}, Tamaño: {}, Ordenar: {}", pagina, tamanio,
                ordenarPor);
        log.debug("Filtro de búsqueda (Instruccion): {}", filtro.getInstruccion().getFolio());

        Pageable pageable = PageRequest.of(pagina, tamanio, Sort.by(ordenarPor));

        InstruccionJuridica probe = new InstruccionJuridica();
        probe.setInstruccion(instruccionMapper.toEntity(filtro.getInstruccion()));

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withIgnorePaths("fechaModificacion", "solicitudCorreccion", "operaciones");
        Example<InstruccionJuridica> example = Example.of(probe, matcher);

        List<InstruccionJuridica> results = juridicaRepository.findAll(example, pageable).getContent();

        log.info("Se recuperaron {} instrucciones jurídicas paginadas.", results.size());
        return instruccionMapper.instruccionJuridicaToInstruccionJuridicaDTOListTwo(results);
    }

    public void guardarRevisionesMonetarias(String authority, InstruccionMonetariaDto instruccion) {

        log.info("Guardar revisiones monetarias para folio: {}. Autoridad: {}", instruccion.getFolio(), authority);

        List<RevisionOperacionMonetaria> revisiones = new ArrayList<>();

        for (OperacionMonetariaDto om : instruccion.getOperaciones()) {
            List<RevisionOperacionMonetaria> revOperaciones = revisionOperacionMonetariaMapper
                    .toEntity(om.getAprobaciones());

            revOperaciones.forEach(r -> r.setOperacionMonetaria(operacionMonetariaMapper.toEntity(om)));

            revisiones.addAll(revOperaciones);
        }
        revisionOperacionMonetariaRepository.saveAll(revisiones);
        log.debug("Se guardaron {} revisiones de operaciones.", revisiones.size());

        Optional<InstruccionMonetaria> im = monetariaRepository.findByInstruccionFolio(instruccion.getFolio());

        if (im.isPresent()) {
            im.get().setFechaAprobacion(LocalDateTime.now());
            monetariaRepository.save(im.get());
            log.debug("InstruccionMonetaria {} marcada como aprobada/finalizada.", instruccion.getFolio());
        }

        if (authority.equalsIgnoreCase(Rol.ROLE_GA.name())) {
            // asignar validacion por montos (si aplica)
            log.debug("La autoridad es GA. Lógica pendiente .");
        }

        log.info("Revisiones monetarias guardadas para folio: {}.", instruccion.getFolio());
    }

    @Transactional
    public void reclasificarInstruccionJuridica(String folioInstruccion) {

        log.info(" Reclasificar/eliminar InstruccionJuridica con folio: {}", folioInstruccion);

        Optional<InstruccionJuridica> instruccionJuridicaOptional = juridicaRepository
                .findByInstruccionFolio(folioInstruccion);
        if (instruccionJuridicaOptional.isPresent()) {
            juridicaRepository.borrar(folioInstruccion);
            log.debug("InstruccionJuridica {} eliminada de la tabla INSTRUCION_JURIDICA.", folioInstruccion);

            // Intenta buscarla inmediatamente después de la eliminación en la misma sesión
            Optional<InstruccionJuridica> instruccionJuridicaAfterDelete = juridicaRepository
                    .findByInstruccionFolio(folioInstruccion);

            if (!instruccionJuridicaAfterDelete.isPresent()) {
                log.debug("InstruccionJuridica {} no encontrada en la sesión después de delete (OK).",
                        folioInstruccion);
                // OK
            } else {
                System.err.println("InstruccionJuridica con folio " + folioInstruccion
                        + " AÚN ENCONTRADA en la sesión después de delete (FALLÓ a nivel de sesión).");
                log.error("InstruccionJuridica con folio {} AÚN ENCONTRADA después de delete.", folioInstruccion);
            }
        } else {
            System.out.println("No se encontró Instruccionjuridica para el folio: "
                    + folioInstruccion + " para reclasificar.");
            log.warn("No se encontró InstruccionJuridica para el folio: {} para reclasificar.", folioInstruccion);
        }

        log.info(" Reclasificación de InstruccionJuridica {} completada.", folioInstruccion);
    }

    public InstruccionJuridicaDto obtenerInstruccionJuridicaPorFolio(String folio) {

        log.info("Obtener InstruccionJuridica por folio: {}", folio);

        Optional<InstruccionJuridica> instruccionJuridica = juridicaRepository.findByInstruccionFolio(folio);

        log.info("InstruccionJuridica {} recuperada.", folio);
        return instruccionMapper.instruccionJuridicaToInstruccionJuridicaDTO(instruccionJuridica.get());
    }

    public List<InstruccionMonetariaDto> obtenerTodasInstruccionesMonetarias() {

        log.info("Obtener todas las Instrucciones Monetarias (findAll).");

        List<InstruccionMonetaria> monetarias = monetariaRepository.findAll();

        log.info("Se recuperaron {} Instrucciones Monetarias.", monetarias.size());
        return instruccionMapper.instruccionMonetariaToInstruccionMonetariaDTOList(monetarias);
    }
}