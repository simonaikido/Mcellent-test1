package com.bim.seif.controllers;

import com.bim.seif.models.ComprobanteOperacion;
import com.bim.seif.services.ComprobanteOperacionService;
import com.bim.seif.services.FileService;
import com.bim.seif.services.FileServiceSSH;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/comprobantes")
@RequiredArgsConstructor
public class ComprobantesController {

    private final ComprobanteOperacionService service;
    private final FileService fileService;

    @Autowired
    private FileServiceSSH fileServiceSSH;


    private String buildStoredName(Long operacionId, String tipoOperacion, String original) {
        String cleanTipo = (tipoOperacion == null ? "" : tipoOperacion.trim().toUpperCase());
        String ext = "";
        int dot = (original == null) ? -1 : original.lastIndexOf('.');
        if (dot >= 0)
            ext = original.substring(dot); // incluye el punto

        long ts = System.currentTimeMillis();
        // Ej: OP_123_MONETARIA_1692578890000.pdf
        return "OP_" + operacionId + "_" + cleanTipo + "_" + ts + ext;
    }
    /**
     * Descarga física desde el FTP, recibiendo el nombre con el que se guardó.
     * /comprobantes/descargar?nombreArchivo=OP_123_MONETARIA_...pdf
     * 
     * @throws SftpException
     * @throws JSchException
     */
    @GetMapping("/descargar")
    public void descargar(@RequestParam String nombreArchivo, HttpServletResponse response)
            throws IOException, JSchException, SftpException {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + nombreArchivo);
        boolean ok = fileService.getFile(nombreArchivo, response.getOutputStream());
        if (!ok)
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ComprobanteOperacion> uploadMultiple(
            @RequestParam("accion") String accion,
            @RequestParam("file") MultipartFile file,
            @RequestParam("tipoOperacion") String tipoOperacion,
            @RequestParam("operacionIds") List<Long> operacionIds) throws Exception {
     
        String storedName = buildStoredName(operacionIds.get(0), tipoOperacion, file.getOriginalFilename());
        String carpeta = "comprobantes";
        fileServiceSSH.sendSFTP(storedName, carpeta, new ByteArrayInputStream(file.getBytes()));

        String rutaRelativa = carpeta + "/" + storedName;
        ComprobanteOperacion co = service.crearYAsignar(accion, rutaRelativa, tipoOperacion, operacionIds);
        return ResponseEntity.ok(co);
    }

    @PostMapping(value = "/finalizarInstruccion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ComprobanteOperacion> finalizarInstruccion(
            @RequestParam("operacionIds") List<Long> operacionIds,
             @RequestParam("folioInstruccion") String folioInstruccion) throws Exception {

        service.verificarYFinalizarInstruccion(operacionIds);
        return ResponseEntity.ok(new ComprobanteOperacion());
    }
}