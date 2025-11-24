package com.bim.seif.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bim.seif.models.dto.ForgotPasswordDTO;
import com.bim.seif.services.ForgotPasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/forgotPass")
@RequiredArgsConstructor
public class forgotPasswordController {

    private final ForgotPasswordService fpwdService;
    
    @PostMapping("/validateUsr")
    public ResponseEntity<?> validateUsr(
            @RequestBody ForgotPasswordDTO request) {
        
        
        ResponseEntity<?> response = fpwdService.validateUsr(request);
        
        log.info("Validacion de usuario completada con estado HTTP: {}", response.getStatusCode());
        return response;
    }

    @PostMapping("/updatePass")
    public ResponseEntity<?> updatePass(
            @RequestBody ForgotPasswordDTO request) {

        
        ResponseEntity<?> response = fpwdService.updatePass(request);
        
        log.info("Actualizacion de contrasena completada con estado HTTP: {}", response.getStatusCode());
        return response;
    }

    @PostMapping("/otpSend")
    public ResponseEntity<?> otpSend(
            @RequestBody ForgotPasswordDTO request) {
        
        
        ResponseEntity<?> response = fpwdService.otpSend(request);
        
        log.info("Envio de OTP completado con estado HTTP: {}", response.getStatusCode());
        return response;
    }

    @PostMapping("/otpValidate")
    public ResponseEntity<?> otpValidate(
            @RequestBody ForgotPasswordDTO request) {
        
        ResponseEntity<?> response = fpwdService.otpValidate(request);
        
        log.info("Validacion de OTP completada con estado HTTP: {}", response.getStatusCode());
        return response;
    }
}