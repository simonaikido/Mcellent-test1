package com.bim.seif.services;

import com.jcraft.jsch.*;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class FileService {

    @Value("${ftp.host}")
    private String FTP_HOST;
    @Value("${ftp.port}")
    private int FTP_PORT;
    @Value("${ftp.user}")
    private String FTP_USER;
    @Value("${ftp.password}")
    private String FTP_PASSWORD;
    @Value("${ftp.path}")
    private String FTP_PATH;

    // Reconectar
    private ByteArrayInputStream inputStreamRedirect;
    private String nameFileMedia;

    public void sendFTP(String filename, ByteArrayInputStream inputStream) throws IOException {
        FTPClient ftpClient = new FTPClient();
        inputStreamRedirect = inputStream;
        nameFileMedia = filename;
        // FTPSClient ftpClient = new FTPSClient();
        try {

            ftpClient.setDefaultTimeout(30000);
            ftpClient.setConnectTimeout(30000);
            ftpClient.connect(FTP_HOST, FTP_PORT);
            ftpClient.login(FTP_USER, FTP_PASSWORD);
            ftpClient.enterLocalActiveMode();// erLocalPassiveMode();

            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                throw new IOException("Exception in connecting to FTP Server");
            }

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.setControlKeepAliveTimeout(300);

            // Verificar/Cambiar directorio
            if (!ftpClient.changeWorkingDirectory(FTP_PATH)) {
                log.info("No se pudo acceder al directorio remoto");
            } else {
                log.info("Directorio remoto OK");
            }

            try {
                // Forzar reutilización de sesión TLS
                System.setProperty("jdk.tls.useExtendedMasterSecret", "true");
                reply = ftpClient.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    ftpClient.disconnect();
                    throw new IOException("Exception in connecting to FTP Server");
                }

                if (!ftpClient.storeFile(filename, inputStream)) {
                    System.err.println("Error al subir archivo: " + ftpClient.getReplyString());
                }

            } catch (IOException e) {
                System.err.println("Excepcion al subir archivo: " + e.getMessage());
                // Revisar estado de la conexión
                if (!ftpClient.isConnected()) {
                    System.err.println("Conexion perdida, intentando reconectar...");
                    this.reconnect(ftpClient);
                    // Forzar reutilización de sesión TLS
                    System.setProperty("jdk.tls.useExtendedMasterSecret", "true");
                    ftpClient.storeFile(filename, inputStream);
                }
                throw e;
            }

        } finally {
            ftpClient.logout();
            ftpClient.disconnect();
        }
    }

    public String senArchivoCancelacionInstruccionProgramada(String filename, ByteArrayInputStream inputStream,
            String nuevoDirectorioParam) throws IOException {

        FTPClient ftpClient = new FTPClient();
        inputStreamRedirect = inputStream;
        nameFileMedia = filename;
        // FTPSClient ftpClient = new FTPSClient();
        String nuevoDirectorio;
        try {

            ftpClient.setDefaultTimeout(60000);
            ftpClient.setConnectTimeout(60000);
            ftpClient.setDataTimeout(60000);
            ftpClient.connect(FTP_HOST, FTP_PORT);
            ftpClient.login(FTP_USER, FTP_PASSWORD);
            ftpClient.enterLocalActiveMode();// erLocalPassiveMode();

            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                throw new IOException("Exception in connecting to FTP Server");
            }

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.setControlKeepAliveTimeout(300);

            // Crear nuevo directorio si no existe
            nuevoDirectorio = FTP_PATH + "/" + nuevoDirectorioParam;
            if (!ftpClient.changeWorkingDirectory(nuevoDirectorio)) {
                // Si no puede cambiar al directorio, intentar crearlo
                if (ftpClient.makeDirectory(nuevoDirectorio)) {
                } else {
                    throw new IOException("No se pudo crear el directorio en el servidor FTP");
                }
            }

            // Verificar/Cambiar directorio
            if (!ftpClient.changeWorkingDirectory(nuevoDirectorio)) {
                log.info("No se pudo acceder al directorio remoto");
            } else {
                log.info("Directorio remoto OK");
            }

            try {
                // Forzar reutilización de sesión TLS
                System.setProperty("jdk.tls.useExtendedMasterSecret", "true");
                reply = ftpClient.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    ftpClient.disconnect();
                    throw new IOException("Exception in connecting to FTP Server");
                }

                if (!ftpClient.storeFile(filename, inputStream)) {
                    log.error("Error al subir archivo: {}", ftpClient.getReplyString());
                }

                nuevoDirectorio += "\\" + filename;

            } catch (IOException e) {
                log.error("Excepción al subir archivo: {}", e.getMessage());
                // Revisar estado de la conexión
                if (!ftpClient.isConnected()) {
                    log.error("Conexión perdida, intentando reconectar...");
                    this.reconnect(ftpClient);
                    // Forzar reutilización de sesión TLS
                    System.setProperty("jdk.tls.useExtendedMasterSecret", "true");
                    ftpClient.storeFile(filename, inputStream);
                }
                throw e;
            }

        } finally {
            ftpClient.logout();
            ftpClient.disconnect();
        }
        return nuevoDirectorio;
    }

    public Mono<Void> sendFile(String filename, String directorioUsuario, byte[] fileContent) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent)) {
            sendSFTP(filename, directorioUsuario, inputStream);
        } catch (IOException | JSchException | SftpException e) {
            throw new RuntimeException("Error al enviar archivo por SFTP", e);
        }
        return Mono.empty();
    }

    public boolean getFile(String nombreArchivo, OutputStream outputStream)
            throws IOException, JSchException, SftpException {

        boolean success = false;
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

            channelSftp.get(FTP_PATH + "/" + nombreArchivo, outputStream);

            // si llegamos aquí, la transferencia fue correcta
            success = true;

        } catch (JSchException | SftpException e) {
            throw e;
        } finally {
            if (channelSftp != null)
                channelSftp.disconnect();
            if (session != null)
                session.disconnect();
        }
        return success;
    }

    public void sendSFTP(String filename, String directorioUsuario, ByteArrayInputStream inputStream)
            throws IOException, JSchException, SftpException {

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
            crearDirectoriosSFTP(FTP_PATH, directorioUsuario, channelSftp);

            // Cambiar al directorio destino
            String fullPath = FTP_PATH + "/" + directorioUsuario;
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

    private void reconnect(FTPClient ftpClient) throws IOException {
        log.info("Reconectando al servidor FTP...");
        ftpClient.disconnect();
        sendFTP(nameFileMedia, inputStreamRedirect); // Debes tener estos valores almacenados
    }

    private void crearDirectoriosSFTP(String pathRaiz, String ruta, ChannelSftp channelSftp)
            throws SftpException {

        String[] directorios = ruta.split("/");
        String path = pathRaiz + "/";

        log.info("Creando directorios SFTP en la ruta: {}", path);
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

}
