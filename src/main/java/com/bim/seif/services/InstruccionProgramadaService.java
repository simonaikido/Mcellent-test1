package com.bim.seif.services;

import com.bim.seif.models.*;
import com.bim.seif.models.dto.CampoCuentaCargoDto;
import com.bim.seif.models.dto.CampoCuentaDto;
import com.bim.seif.models.dto.CampoMontoDto;
import com.bim.seif.models.dto.CampoReferenciaDto;
import com.bim.seif.models.dto.InstruccionProgramadaDto;
import com.bim.seif.models.dto.InstruccionProgramadaResponseDto;
import com.bim.seif.models.dto.OperacionMonetariaRequestDto;
import com.bim.seif.models.dto.TipoOperacionMonetariaDto;
import com.bim.seif.models.dto.TipoOperacionMonetariaProgramadaDto;
import com.bim.seif.repositories.*;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class InstruccionProgramadaService {

    private final InstruccionProgramadaRepository instruccionProgramadaRepository;
    private final TipoOperacionMonetariaRepository tipoOperacionMonetariaRepository;
    private final DivisaRepository divisaRepository;
    private final CuentaRepository cuentaAbonoRepository;
    private final ProgramacionRepository programacionRepository;
    private final InstruccionRepository instruccionRepository;
    private final InstruccionMonetariaRepository monetariaRepository;
    private final DocumentoInstruccionRepository documentoInstruccionRepository;
    private final OperacionService operacionService;
    private final CampoCuentaCargoRepository campoCuentaCargoRepo;

    @Autowired
    FileServiceSSH fileServiceSSH;

    @Autowired
    InstruccionService instruccionService;

    @Value("${ruta.guardar.adicional}")
    private String RUTA_GUARDAR_ADICIONALES;

    public InstruccionProgramadaService(DocumentoInstruccionRepository documentoInstruccionRepository,
            InstruccionMonetariaRepository monetariaRepository, InstruccionRepository instruccionRepository,
            InstruccionProgramadaRepository instruccionProgramadaRepository,
            TipoOperacionMonetariaRepository tipoOperacionMonetariaRepository, DivisaRepository divisaRepository,
            CuentaRepository cuentaAbonoRepository, ProgramacionRepository programacionRepository,
            OperacionService operacionService, CampoCuentaCargoRepository campoCuentaCargoRepo) {

        log.info("InstruccionProgramadaService initialized with repositories");
        this.instruccionProgramadaRepository = instruccionProgramadaRepository;
        this.tipoOperacionMonetariaRepository = tipoOperacionMonetariaRepository;
        this.divisaRepository = divisaRepository;
        this.cuentaAbonoRepository = cuentaAbonoRepository;
        this.programacionRepository = programacionRepository;
        this.instruccionRepository = instruccionRepository;
        this.monetariaRepository = monetariaRepository;
        this.documentoInstruccionRepository = documentoInstruccionRepository;
        this.operacionService = operacionService;
        this.campoCuentaCargoRepo = campoCuentaCargoRepo;
    }

    public List<InstruccionProgramadaResponseDto> obtenerInstruccionesProgramadas(String responsable) {

        List<Object[]> results = instruccionProgramadaRepository.findByResponsableEmail(responsable);

        return results.stream().map(this::mapToProgramadasResponse).toList();
    }

    @Transactional
    public void guardarInstruccionProgramada(String folioIstruccion, String responsableMonetaria,
            String responsableJuridica) {

        // instruccionService.calsificarInstruccion(folioIstruccion,responsableMonetaria,"");

        Optional<Instruccion> i = instruccionRepository.findById(folioIstruccion);
        if (i.isPresent()) {
            if (responsableMonetaria != null) {
                InstruccionMonetaria im = new InstruccionMonetaria();
                im.setInstruccion(i.get());
                im.setResponsable(responsableMonetaria);
                im.setEstatus(new EstatusInstruccion(EstatusInstruccionEnum.PR.name()));
                im.setProgramada(true);
                monetariaRepository.save(im);

                InstruccionProgramadaDto instruccionProgramadaDto = new InstruccionProgramadaDto();
                instruccionProgramadaDto.setFolio(folioIstruccion);
                instruccionProgramadaDto.setMonto(0);
                instruccionProgramadaDto.setFechaClasificacion(LocalDateTime.now());
                instruccionProgramadaDto.setResponsableEmail(responsableMonetaria);
                instruccionProgramadaRepository.save(dtoToModel(instruccionProgramadaDto));
            }
        }

    }

    public void cancelarInstruccionProgramada(String folio, String comentario, MultipartFile file) {
        Optional<Instruccion> i = instruccionRepository.findById(folio);
        String rutaArchivoCancelacion;
        if (i.isPresent()) {
            // Envia archivo al ftp
            String fileName = "ArchivoCancelacion_" + LocalDateTime.now();
            try {
                byte[] fileBytes = file.getBytes();
                // Crear un ByteArrayInputStream a partir de los bytes
                ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
                rutaArchivoCancelacion = fileServiceSSH.senArchivoCancelacionInstruccionProgramada(
                        fileName.split("T")[0], inputStream, RUTA_GUARDAR_ADICIONALES);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (JSchException e) {
                throw new RuntimeException(e);
            } catch (SftpException e) {
                throw new RuntimeException(e);
            }

            // Guarda archivo de cancelacion en la base
            Instruccion instruccion = i.get();
            DocumentoInstruccion documentoInstruccion = new DocumentoInstruccion();
            documentoInstruccion.setInstruccion(instruccion);
            documentoInstruccion.setRuta(rutaArchivoCancelacion);
            documentoInstruccion.setFechaCarga(LocalDateTime.now());
            documentoInstruccionRepository.save(documentoInstruccion);

            // Cancela instruccion en la base
            Date date = new Date();
            Timestamp hoy = new Timestamp(date.getTime());
            instruccion.setFechaCancelacion(hoy.toLocalDateTime());
            instruccionRepository.save(instruccion);
        } else {
            throw new RuntimeException("Instruccion no encontrada");
        }

    }

    private InstruccionProgramadaResponseDto mapToProgramadasResponse(Object[] result) {
        InstruccionProgramadaResponseDto dto = new InstruccionProgramadaResponseDto();
        dto.setFolio((String) result[0]);
        dto.setFechaHoraAlta(timestampToLocalDateTime((Timestamp) result[1]));
        dto.setAliasFideicomiso((String) result[2]);
        dto.setRegionFideicomiso((String) result[3]);
        dto.setRutaArchivo((String) result[4]);
        dto.setComentario((String) result[5]);
        dto.setTipoInstruccion("Monetaria");

        dto.setProgramada(Integer.valueOf(String.valueOf(result[6])) != 0);
        dto.setMonto(formatoMoneda(Integer.valueOf(String.valueOf(result[6]))));

        if (dto.getProgramada() == true) {
            dto.setConcepto((String) result[7]);
            // ntes convertías a BigInteger; ahora viene como STRING "cuenta - banco -
            // divisa"
            dto.setCuentaCargo((String) result[8]);
            dto.setCuentaAbono(
                    result[10] != null && !String.valueOf(result[9]).trim().isEmpty()
                            ? String.valueOf(result[9])
                            : null);

            dto.setTipoOperacion((String) result[10]);

            List<Object[]> resultsFechas = instruccionProgramadaRepository.findByProgramacionEmail(dto.getFolio());
            List<Programacion> fechas = resultsFechas.stream().map(this::mapToFechas).toList();
            dto.setFechas(fechas);
        }
        return dto;
    }

    private static Timestamp toTimestamp(Object raw) {
        if (raw == null)
            return null;
        if (raw instanceof Timestamp ts)
            return ts;
        if (raw instanceof java.sql.Date d)
            return new Timestamp(d.getTime());
        if (raw instanceof java.util.Date d)
            return new Timestamp(d.getTime());
        throw new IllegalArgumentException("Tipo insperado para fecha_ejecucion: " + raw.getClass());
    }

    private Programacion mapToFechas(Object[] result) {
        Programacion programacion = new Programacion();
        BigInteger id = (BigInteger) result[0];
        programacion.setId(Long.valueOf(id.longValue()));
        programacion.setFechaEjecucion(toTimestamp(result[1]));
        programacion.setInstruccionProgramada((String) result[2]);
        return programacion;
    }

    public String formatoMoneda(Number cantidad) {
        // Usamos el locale de EE.UU. para el formato $ y comas como separadores de
        // miles
        NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.US);
        return formato.format(cantidad);
    }

    public LocalDateTime strToLocalDateTime(String fechaString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return LocalDateTime.parse(fechaString, formatter);
    }

    public LocalDateTime timestampToLocalDateTime(Timestamp timestamp) {
        return timestamp.toLocalDateTime();
    }

    private InstruccionProgramada dtoToModel(InstruccionProgramadaDto dto) {
        InstruccionProgramada ip = new InstruccionProgramada();

        Instruccion instruccion = instruccionService.obtenerInstruccionPorFolio(dto.getFolio());
        ip.setInstruccion(instruccion);
        ip.setComentario(dto.getComentario());
        ip.setConcepto(dto.getConcepto());
        if (dto.getCuentaCargo() != null) {
            String cuentaInput = String.valueOf(dto.getCuentaCargo());
            String soloDigitos = cuentaInput.replaceAll("\\D", ""); // quita todo excepto dígitos

            CampoCuentaCargo ccc = campoCuentaCargoRepo.findByCuenta(cuentaInput)
                    .or(() -> campoCuentaCargoRepo.findByCuentaNormalizada(soloDigitos))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "CuentaCargo no existe en CampoCuentaCargo.cuenta = " + cuentaInput));

            ip.setCuentaCargo(ccc); // ← relación a la entidad
        }
        ip.setFechaCancelacion(dto.getFechaCancelacion());
        ip.setFechaClasificacion(dto.getFechaClasificacion());
        ip.setMonto(dto.getMonto() * 100);
        ip.setReferencia(dto.getReferencia());
        ip.setResponsable_email(dto.getResponsableEmail());
        return ip;
    }

    @Transactional
    public void guardarInstruccionProgramadaProg(InstruccionProgramadaDto req) throws InterruptedException {

        log.info("Guardando instrucción programada para folio: {}", req.getFolio());

        // Obtener la instrucción programada
        InstruccionProgramada ip = instruccionProgramadaRepository.findById(req.getFolio())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró InstruccionProgramada con folio " + req.getFolio()));

        // Actualizar datos base
        ip.setConcepto(req.getConcepto());
        ip.setComentario(req.getComentario());
        ip.setMonto(req.getMonto() * 100);
        ip.setReferencia(req.getReferencia());

        TipoOperacionMonetaria tipo = tipoOperacionMonetariaRepository.findById(req.getTipoOperacionMonetariaCve())
                .orElseThrow(() -> new IllegalArgumentException(
                        "TipoOperacionMonetaria no encontrado: " + req.getTipoOperacionMonetariaCve()));
        ip.setTipoOperacionMonetaria(tipo);

        Divisa divisa = divisaRepository.findById(req.getDivisaCve().substring(0, 3))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Divisa no encontrada: " + req.getDivisaCve()));
        ip.setDivisa(divisa);

        // Validar cuenta abono
        if (req.getCuentaAbonoCuenta() != null) {
            cuentaAbonoRepository.findById(req.getCuentaAbonoCuenta())
                    .ifPresent(ip::setCuenta);
        }

        // Validar y setear cuenta cargo (id → entidad existente)
        if (req.getCuentaCargo() != null) {
            String cuentaInput = String.valueOf(req.getCuentaCargo());
            String cuentaNormalizada = cuentaInput.replaceAll("\\D", ""); // elimina todo excepto dígitos

            CampoCuentaCargo ccc = campoCuentaCargoRepo.findByCuenta(cuentaInput)
                    .or(() -> campoCuentaCargoRepo.findByCuentaNormalizada(cuentaNormalizada))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "CuentaCargo no existe en CampoCuentaCargo.cuenta = " + cuentaInput));

            ip.setCuentaCargo(ccc); // ← referencia a entidad ya persistida
        }

        // Evita flush prematuro
        instruccionProgramadaRepository.save(ip);

        // Crear programaciones
        List<Programacion> programaciones = new ArrayList<>();
        for (Date fecha : req.getFechas()) {
            Programacion prog = new Programacion();
            prog.setInstruccionProgramada(ip.getFolio());
            prog.setFechaEjecucion(new java.sql.Timestamp(fecha.getTime()));
            programaciones.add(prog);
        }
        programacionRepository.saveAll(programaciones);

        // Crear operación por cada programación (usando la fecha programada)
        for (Programacion prog : programaciones) {
            try {
                dispararOperacionProgramada(ip, prog.getFechaEjecucion().toLocalDateTime());
            } catch (Exception e) {
                log.error("No se pudo crear OperacionMonetaria (IP {}, prog {}): {}",
                        ip.getFolio(), prog.getId(), e.getMessage(), e);
            }
        }

        log.info("Instrucción programada {} procesada con {} programaciones y operaciones creadas.",
                ip.getFolio(), programaciones.size());
    }

    private void dispararOperacionProgramada(InstruccionProgramada ip, @Nullable LocalDateTime fechaProgComentario)
            throws InterruptedException {
        OperacionMonetariaRequestDto dto = buildOperacionDesdeIP(ip, fechaProgComentario);
        operacionService.guardarOperacionesMonetarias("programadas", Collections.singletonList(dto));
    }

    private OperacionMonetariaRequestDto buildOperacionDesdeIP(InstruccionProgramada ip,
            @Nullable LocalDateTime fechaProgComentario) {

        OperacionMonetariaRequestDto dto = new OperacionMonetariaRequestDto();

        // Tipo de operación (builder de tu DTO inmutable)
        TipoOperacionMonetariaDto tipoDto = TipoOperacionMonetariaDto.builder()
                .cve(ip.getTipoOperacionMonetaria().getCve())
                .descripcion(ip.getTipoOperacionMonetaria().getDescripcion())
                .validacionGeneral(false)
                .validacionMontos(false)
                .validacionMesaControl(false)
                .build();
        dto.setTipoOperacion(tipoDto);

        dto.setInstruccionFolio(ip.getFolio());
        dto.setConcepto(ip.getConcepto());

        //registrar la fecha programada en el DTO para que la operación se
        // cree con esa fecha
        dto.setFechaRegistro(fechaProgComentario);

        String comentario = ip.getComentario() == null ? "" : ip.getComentario();
        if (fechaProgComentario != null)
            comentario += " (Programada: " + fechaProgComentario + ")";
        dto.setComentario(comentario.trim());

        if (ip.getMonto() != 0) {
            CampoMontoDto monto = new CampoMontoDto();
            monto.setMonto((long) ip.getMonto());
            dto.setMonto(monto);
        }

        if (ip.getReferencia() != null && !ip.getReferencia().isBlank()) {
            CampoReferenciaDto ref = new CampoReferenciaDto();
            ref.setReferencia(ip.getReferencia());
            dto.setReferencia(ref);
        }

        if (ip.getCuentaCargo() != null) {
            CampoCuentaCargoDto cargo = new CampoCuentaCargoDto();
            cargo.setCuenta(ip.getCuentaCargo().getCuenta());
            dto.setCuentaCargo(cargo);
        }

        if (ip.getCuenta() != null) {
            CampoCuentaDto abono = new CampoCuentaDto();
            abono.setCuenta(ip.getCuenta().getCuenta());
            dto.setCuentaAbono(abono);
        }

        return dto;
    }

    public static Timestamp stringToTimestamp(String dateString) {
        // Define el formato de la fecha que esperas en el String

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            // Convierte el String a un objeto Date
            java.util.Date parsedDate = format.parse(dateString);
            // Convierte el objeto Date a Timestamp
            return new Timestamp(parsedDate.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return null; // O maneja la excepción de otra manera
        }
    }

    public void reclasificarInstruccionProgramada(String instruccionFolio) {
        Optional<InstruccionProgramada> instruccionProgramadaFind = instruccionProgramadaRepository
                .findById(instruccionFolio);
        instruccionProgramadaRepository.delete(instruccionProgramadaFind.get());
        instruccionService.reclasificarInstruccionMonetaria(instruccionFolio);
    }

    public List<TipoOperacionMonetariaProgramadaDto> obtenerTiposOperacionProgramada() {
        return instruccionProgramadaRepository.findTiposOperacionProgramada();
    }

    public List<DiasFestivos> obtenerDiasFestivos() {
        return instruccionProgramadaRepository.findDiasFestivos();
    }

    public List<String> obtenerCorreosFideicomiso(String folio) {
        List<String> destinatarios = new ArrayList<>(); // instruccionProgramadaRepository.obtenerCorreosFideicomiso(folio);
        destinatarios.add("gvazquezh@mcllent.com");
        return destinatarios;
    }

}
