package com.bim.seif.controllers;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class SpaErrorController implements ErrorController{

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object uriObj = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String uri = uriObj != null ? uriObj.toString() : "";
        log.info("URI de la solicitud que causó el error: {}", uri);

        // Si es 404 y NO es archivo estático y NO es endpoint de backend → reenviar SPA
        if (status != null && Integer.parseInt(status.toString()) == HttpStatus.NOT_FOUND.value()) {
            // 1) Si pide archivo (tiene punto), no tocar
            if (uri.contains(".")) {
                return "forward:/";
            }
            // 2) Evitar prefijos de backend (ajusta los que uses)
            if (uri.startsWith("/seif/api")
                    || uri.startsWith("/seif/docs")
                    || uri.startsWith("/seif/swagger-ui")
                    || uri.startsWith("/seif/auth")
                    || uri.startsWith("/seif/archivos")) {
                log.info("Error 404 en ruta de backend o archivo estático: {}", uri);
                return "forward:/";
            }
            // 3) Es ruta de Angular → devolver index.html
            return "forward:/int/index.html";
        }

        // Para otros códigos, deja el comportamiento por defecto (o manda index)
        return "forward:/int/index.html";
    }
}
