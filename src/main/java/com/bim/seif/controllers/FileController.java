package com.bim.seif.controllers;

import com.bim.seif.models.DocumentoFideicomiso;
import com.bim.seif.models.DocumentoInstruccion;
import com.bim.seif.models.SolicitudArchivoJuridica;
import com.bim.seif.repositories.SolicitudArchivoJuridicaRepository;
import com.bim.seif.services.DocumentoFideicomisoService;
import com.bim.seif.services.DocumentoInstruccionService;
import com.bim.seif.services.FileService;

import javax.servlet.http.HttpServletResponse;

import com.bim.seif.services.FileServiceSSH;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/archivos")
@RequiredArgsConstructor
public class FileController {

    @Autowired
    FileService fileService;
    @Autowired
    DocumentoInstruccionService documentoInstruccionService;
    @Autowired
    DocumentoFideicomisoService documentoFideicomisoService;
    @Autowired
    FileServiceSSH fileServiceSSH;
    @Autowired
    private SolicitudArchivoJuridicaRepository solicitudArchivoJuridicaRepository;

    @GetMapping
    public void descargarArchivo(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String nombreArchivo,
            HttpServletResponse response) {

        if (jwt == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            log.warn("Intento de descarga no autorizado. Archivo: {}", nombreArchivo);
            return;
        }

        log.info("Solicitud de descarga de archivo (FTP) por usuario {}: {}", jwt.getSubject(), nombreArchivo);

        try {
            //nombre limpio + MIME + Content-Disposition correcto
            String onlyName = java.nio.file.Paths.get(nombreArchivo.replace('\\', '/'))
                    .getFileName().toString();

            String lower = onlyName.toLowerCase();
            String contentType = lower.endsWith(".pdf") ? "application/pdf" : "application/octet-stream";
            response.setContentType(contentType);

            String cd = "attachment; filename=\"" + onlyName.replace("\"", "") + "\"; filename*=UTF-8''"
                    + java.net.URLEncoder.encode(onlyName, java.nio.charset.StandardCharsets.UTF_8)
                            .replace("+", "%20");
            response.setHeader("Content-Disposition", cd);
            // === FIN NUEVO ===

            boolean success = fileService.getFile(nombreArchivo, response.getOutputStream());
            response.flushBuffer();

            if (!success) {
                log.error("Fallo al descargar el archivo: {} - Archivo no encontrado o error de servicio.",
                        nombreArchivo);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } else {
                log.info("Archivo descargado con éxito: {}", onlyName);
            }
        } catch (Exception e) {
            log.error("Error crítico al descargar archivo (FTP): {}", nombreArchivo, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/cargar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Void> upload(@RequestPart(name = "file") FilePart filePart) {
        log.info("Solicitud de carga reactiva para archivo: {}", filePart.filename());
        log.info(filePart.filename());
        return DataBufferUtils.join(filePart.content())
                .flatMap(dataBuffer -> {
                    byte[] fileBytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(fileBytes);
                    DataBufferUtils.release(dataBuffer);
                    log.info(fileBytes.toString());
                    // return Mono.empty();

                    Mono<Void> result = enviarAServicioExterno(filePart.filename(), fileBytes);
                    log.info("Archivo reactivo {} enviado al servicio externo.", filePart.filename());
                    return result;
                });
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadFile(@RequestParam(name = "file") MultipartFile file,
            @RequestParam(value = "metadata", required = false) String path) {

        log.info("Solicitud de carga de archivo generico. Nombre original: {}. Path: {}", file.getOriginalFilename(),
                path);
        log.info(file.getName());

        String rutaAdicionales = path + "/" + "adicionales" + "/";
        String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String nombreArchivo = LocalDateTime.now().format(dtf) + "." + extension;

        try {
            // LocalDateTime.now().toString() + "_" +
            fileServiceSSH.enviarPorSFTP(nombreArchivo, rutaAdicionales, file.getBytes());
            log.info("Archivo generico subido por SFTP. Ruta: {}", rutaAdicionales + "/" + nombreArchivo);
        } catch (IOException e) {
            log.error("Error al subir archivo generico por SFTP.", e);
            throw new RuntimeException(e);
        }
        return rutaAdicionales + "/" + nombreArchivo;
    }

    private Mono<Void> enviarAServicioExterno(String filename, byte[] fileContent) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent)) {
            fileService.sendFTP(filename, inputStream);
        } catch (IOException e) {
            log.error("Error en enviarAServicioExterno (FTP): {}", filename, e);
            throw new RuntimeException(e);
        }
        return Mono.empty();
    }

    // Nuevo Endpoint para Documentos de Instrucción

    @GetMapping("/adicionales/{folio}") // Nuevo endpoint
    public ResponseEntity<List<DocumentoInstruccion>> getDocumentosAdicionalesPorFolio(@PathVariable String folio) {

        List<DocumentoInstruccion> documentos = documentoInstruccionService.getDocumentosByFolioInstruccion(folio);

        if (documentos.isEmpty()) {
            log.info("No se encontraron documentos adicionales para el folio: {}", folio);
            return ResponseEntity.noContent().build();
        }
        log.info("Se encontraron {} documentos adicionales para el folio: {}", documentos.size(), folio);

        return ResponseEntity.ok(documentos);
    }

    // Nuevo Endpoint para Documentos de Fideicomiso
    @GetMapping("/fideicomiso/{folio}")
    public ResponseEntity<List<DocumentoFideicomiso>> getDocumentosFideicomisoPorFolio(@PathVariable String folio) {

        List<DocumentoFideicomiso> documentos = documentoFideicomisoService.getDocumentosByFolioFideicomiso(folio);

        if (documentos.isEmpty()) {
            log.info("No se encontraron documentos para el folio: {}", folio);
            return ResponseEntity.noContent().build();
        }
        log.info("Se encontraron {} documentos para el folio: {}", documentos.size(), folio);
        return ResponseEntity.ok(documentos);
    }

    @GetMapping("/solicitados")
    public void descargarArchivoExt(@RequestParam String nombreArchivo, HttpServletResponse response) {
        try {
            // Configurar headers primero
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"");

            // Usar el método que escribe directamente al OutputStream
            boolean success = fileServiceSSH.getFile(nombreArchivo, response.getOutputStream());

            if (!success) {
                log.error("Fallo al descargar el archivo: {} - Archivo no encontrado o error de servicio.",
                        nombreArchivo);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } else {
                log.info("Archivo  descargado con exito: {}", nombreArchivo);
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.error("Error al descargar archivo: " + nombreArchivo, e);
        }
    }

    @PostMapping(value = "/internos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Void> cargarDocumentoAdicionalOperacionJuridica(
            @RequestParam(name = "file") MultipartFile file,
            @RequestParam(name = "noFideicomiso") String noFideicomiso,
            @RequestParam(name = "noInstruccion") String noInstruccion,
            @RequestParam(name = "idOperacion") String idOperacion,
            @RequestParam(name = "DescripcionDelActo") String descripcionDelActo) {

        // Construir ruta completa incluyendo el nombre del archivo
        String rutaAdicionales = String.format(
                "/juridicas/%s/%s/%s/",
                noFideicomiso,
                noInstruccion,
                idOperacion);

        String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String nombreArchivo = LocalDateTime.now().format(dtf) + "." + extension;

        return Mono.fromRunnable(() -> {
            // Guardar en base de datos
            SolicitudArchivoJuridica solicitud = new SolicitudArchivoJuridica();

            solicitud.setIdOperacion(Long.parseLong(idOperacion));

            solicitud.setFechaSolicitud(new Date());
            solicitud.setFechaCarga(new Date());
            solicitud.setNombreArchivo(file.getOriginalFilename());
            solicitud.setRutaArchivo(rutaAdicionales + nombreArchivo);
            solicitud.setDescripcionDelActo(descripcionDelActo);
            solicitud.setNombreFormato(null);

            solicitud.setRutaFormato(null);
            solicitud.setNota(null);

            solicitudArchivoJuridicaRepository.save(solicitud);
            log.info("Documento Juridico interno cargado y registrado con exito. Ruta: {}", solicitud.getRutaArchivo());
        });
    }

    @PostMapping(value = "/carga_cliente", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Void> cargarDocumentoCargaCliente(
            @RequestParam(name = "file") MultipartFile file,
            @RequestParam(name = "noFideicomiso") String noFideicomiso,
            @RequestParam(name = "noInstruccion") String noInstruccion,
            @RequestParam(name = "idOperacion") String idOperacion) {

        String nombreOriginal = file.getOriginalFilename();
        if (nombreOriginal == null || nombreOriginal.isEmpty()) {
            log.error("Fallo en carga de cliente - El archivo no tiene nombre valido.");
            throw new IllegalArgumentException("El archivo no tiene nombre valido.");
        }

        // Construir ruta completa incluyendo el nombre del archivo
        String rutaBase = String.format(
                "/%s/%s/%s/",
                noFideicomiso,
                noInstruccion,
                idOperacion);

        String rutaAdicionales = rutaBase + nombreOriginal;

        return Mono.fromRunnable(() -> {
            try {

                fileServiceSSH.enviarPorSFTP(nombreOriginal, rutaBase, file.getBytes());

                // Guardar en base de datos
                SolicitudArchivoJuridica solicitud = new SolicitudArchivoJuridica();
                solicitud.setIdOperacion(Long.parseLong(idOperacion)); // Debe existir en operacion_juridica
                solicitud.setFechaSolicitud(new Date());
                solicitud.setFechaCarga(new Date());
                solicitud.setNombreArchivo(nombreOriginal);
                solicitud.setRutaArchivo(rutaAdicionales); // Usando ruta completa
                solicitud.setDescripcionDelActo("Carga Cliente");
                solicitud.setNombreFormato(null);
                solicitud.setRutaFormato(null);
                solicitud.setNota(null);

                solicitudArchivoJuridicaRepository.save(solicitud);
                log.info("Documento por Cliente cargado y registrado con exito. Ruta: {}", solicitud.getRutaArchivo());

            } catch (IOException e) {
                log.error("Error al enviar archivo por Cliente (SFTP). Fideicomiso: {}, Operacion: {}", noFideicomiso,
                        idOperacion, e);
                throw new RuntimeException("Error al enviar archivo por SFTP", e);
            }
        });
    }

    @GetMapping("/listar/formatosBIM")
    public ResponseEntity<?> listarFormatosBIM() {
        log.info("Recibida solicitud para listar formatos BIM.");
        try {
            List<FileServiceSSH.ArchivoInfo> formatos = fileServiceSSH.listarArchivosSFTP("formatosBIM");

            if (formatos.isEmpty()) {
                log.info("No se encontraron formatos BIM.");
                return ResponseEntity.noContent().build();
            }

            log.info("Listado exitoso de {} formatos BIM.", formatos.size());
            return ResponseEntity.ok(formatos);

        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                // Manejo específico si el directorio no existe
                log.error("Directorio de formatos BIM no encontrado.", e);
                return ResponseEntity.status(HttpServletResponse.SC_NOT_FOUND)
                        .body("Error: El directorio de formatos no existe en el servidor.");
            }
            log.error("Error SFTP al listar formatos BIM.", e);
            return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                    .body("Error SFTP al obtener la lista de formatos.");
        } catch (JSchException e) {
            log.error("Error de conexión JSch/SFTP al listar formatos BIM.", e);
            return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                    .body("Error de conexión al servidor SFTP.");
        } catch (Exception e) {
            log.error("Error inesperado al listar formatos BIM.", e);
            return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                    .body("Error interno del servidor al listar formatos.");
        }
    }
}
