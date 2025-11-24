package com.bim.seif.services;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletOutputStream;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


@Slf4j
@Service
public class FileServiceSSH {

    @Value("${ftp.path}")
    private String FTP_REMOTE_PATH;

    @Value("${ftp.host}")
    private String FTP_HOST;
    @Value("${ftp.port}")
    private int FTP_PORT;
    @Value("${ftp.user}")
    private String FTP_USER;
    @Value("${ftp.password}")
    private String FTP_PASSWORD;

    public void sendSFTP(String filename, String directorioUsuario, ByteArrayInputStream inputStream)
            throws IOException, JSchException, SftpException {

                log.info("Iniciando envío de archivo SFTP: {} al directorio: {}", filename, directorioUsuario);

        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(FTP_USER, FTP_HOST, FTP_PORT);
            session.setPassword(FTP_PASSWORD);

            // Configuración para evitar problemas de autenticación
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();
            log.info("Conexión SFTP establecida");

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            log.info("Canal SFTP abierto");

            // Crear directorios si no existen
            crearDirectoriosSFTP(FTP_REMOTE_PATH, directorioUsuario, channelSftp);

            // Cambiar al directorio destino
            String fullPath = FTP_REMOTE_PATH + "/" + directorioUsuario;
            try {
                channelSftp.cd(fullPath);
            } catch (SftpException e) {
                log.error("No se pudo acceder al directorio: {}", fullPath, e);
                throw e;
            }

            // Subir el archivo
            try {
                channelSftp.put(inputStream, filename);
                log.info("Archivo {} subido exitosamente", filename);
            } catch (SftpException e) {
                log.error("Error al subir el archivo: {}", e.getMessage());
                throw e;
            }

        } finally {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    public Mono<Void> enviarPorSFTP(String filename, String directorioUsuario, byte[] fileContent) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent)) {
            sendSFTP(filename, directorioUsuario, inputStream);
        } catch (IOException | JSchException | SftpException e) {
            throw new RuntimeException("Error al enviar archivo por SFTP", e);
        }
        return Mono.empty();
    }

    private void crearDirectoriosSFTP(String pathRaiz, String ruta, ChannelSftp channelSftp)
            throws SftpException {

        String[] directorios = ruta.split("/");
        String path = pathRaiz + "/";

        for (String directorio : directorios) {
            path += directorio;
            try {
                channelSftp.mkdir(path);
                log.info("Directorio creado: {}", path);
            } catch (SftpException e) {
                if (e.id != ChannelSftp.SSH_FX_FAILURE) {
                    throw e;
                }
                // El directorio ya existe, continuar
                log.debug("El directorio {} ya existe", path);
            }
            path += "/";
        }
    }

    public boolean  getFile(String nombreArchivo, ServletOutputStream nombreArchivo2) throws JSchException, SftpException {
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(FTP_USER, FTP_HOST, FTP_PORT);
            session.setPassword(FTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            System.out.println(FTP_REMOTE_PATH+"/" + nombreArchivo);
            channelSftp.get(FTP_REMOTE_PATH+"/" + nombreArchivo.trim(), nombreArchivo2);
            return true;

        } finally {
            if (channelSftp != null) channelSftp.disconnect();
            if (session != null) session.disconnect();
        }
    }

    public String senArchivoCancelacionInstruccionProgramada(String filename, ByteArrayInputStream inputStream, String nuevoDirectorioParam) throws IOException, JSchException, SftpException {
        sendSFTP(filename,nuevoDirectorioParam,inputStream);
        return FTP_REMOTE_PATH + "/" + nuevoDirectorioParam + "/" + filename;
    }

    public List<String> listFilesSFTP(String pathDirectorio) throws JSchException, SftpException, IOException {
        Session session = null;
        ChannelSftp channelSftp = null;
        List<String> fileNames = new java.util.ArrayList<>();
        String fullPath = FTP_REMOTE_PATH + pathDirectorio;
        log.info("Listando archivos en directorio SFTP: {}", fullPath);

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(FTP_USER, FTP_HOST, FTP_PORT);
            session.setPassword(FTP_PASSWORD);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            channelSftp.cd(fullPath);
            java.util.Vector<ChannelSftp.LsEntry> entries = channelSftp.ls(".");

            for (ChannelSftp.LsEntry entry : entries) {
                if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..") && !entry.getAttrs().isDir()) {
                    fileNames.add(entry.getFilename());
                }
            }

        } catch (SftpException e) {
            log.error("Error SFTP al listar archivos en {}: {}", fullPath, e.getMessage());
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                log.warn("El directorio no existe: {}", fullPath);
                return fileNames;
            }
            throw e;
        } finally {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
        return fileNames;
    }

    public static class ArchivoInfo {
        private String nombre;
        private String rutaCompleta;
        public ArchivoInfo(String nombre, String rutaCompleta) {
            this.nombre = nombre;
            this.rutaCompleta = rutaCompleta;
        }
        public String getNombre() {
            return nombre;
        }
        public void setNombre(String nombre) {
            this.nombre = nombre;
        }
        public String getRutaCompleta() {
            return rutaCompleta;
        }
        public void setRutaCompleta(String rutaCompleta) {
            this.rutaCompleta = rutaCompleta;
        }
    }

    public List<ArchivoInfo> listarArchivosSFTP(String directorioRemoto) throws JSchException, SftpException {
        Session session = null;
        ChannelSftp channelSftp = null;
        List<ArchivoInfo> archivos = new ArrayList<>();

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(FTP_USER, FTP_HOST, FTP_PORT);
            session.setPassword(FTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            log.info("Conexión SFTP establecida para listar archivos.");

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            log.info("Canal SFTP abierto para listar archivos.");

            // Directorio base para los formatos BIM (asumiendo que están en la raíz del FTP + un subdirectorio "formatosBIM")
            String rutaBase = FTP_REMOTE_PATH + "/formatosBIM";

            // Navegar al directorio remoto
            try {
                channelSftp.cd(rutaBase);
            } catch (SftpException e) {
                log.error("El directorio de formatos BIM no existe o no se puede acceder: {}", rutaBase, e);
                throw new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE, "Directorio no encontrado: " + rutaBase);
            }

            // Listar archivos
            Vector<ChannelSftp.LsEntry> fileList = channelSftp.ls(".");
            for (ChannelSftp.LsEntry entry : fileList) {
                // Excluir directorios y las entradas especiales "." y ".."
                if (!entry.getAttrs().isDir() && !entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
                    String nombreArchivo = entry.getFilename();

                    // --- 🔑 CLAVE: URL ENCODING PARA EL NOMBRE DEL ARCHIVO ---
                    String nombreArchivoCodificado;
                    try {
                        // 1. Codificar el nombre del archivo usando UTF-8.
                        nombreArchivoCodificado = URLEncoder.encode(nombreArchivo, StandardCharsets.UTF_8.toString())
                                // 2. Reemplazar '+' por '%20', ya que '+' se usa típicamente para espacios en query params,
                                //    pero en la ruta de acceso (path segment) se prefiere el %20.
                                .replace("+", "%20");
                    } catch (Exception e) {
                        log.error("Error al codificar el nombre del archivo: {}", nombreArchivo, e);
                        // Si falla la codificación, usar el nombre original (no codificado)
                        nombreArchivoCodificado = nombreArchivo;
                    }

                    String rutaCompletaCodificada = rutaBase + "/" + nombreArchivoCodificado;

                    // Usar el nombre original (sin codificar) para el campo 'nombre' y la ruta codificada para 'rutaCompleta'
                    archivos.add(new ArchivoInfo(nombreArchivo, rutaCompletaCodificada));
                }
            }

            log.info("Se encontraron {} archivos en el directorio de formatos BIM.", archivos.size());

        } finally {
            if (channelSftp != null) channelSftp.disconnect();
            if (session != null) session.disconnect();
        }

        return archivos;
    }

}