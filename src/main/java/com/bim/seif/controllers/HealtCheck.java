package com.bim.seif.controllers;

import com.bim.seif.services.HealtCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@RestController
@RequestMapping("/health")
public class HealtCheck {

    HealtCheckService healtCheckService;

    @Autowired
    private DataSource dataSource;


    public HealtCheck(HealtCheckService healtCheckService ) {
        this.healtCheckService = healtCheckService;
    }

    @GetMapping("/test/{var}")
    @Operation(summary = "Regresa la salud del servicio", description = "Metodo para verificar que el servicio esta funcionando")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval of users"),
            @ApiResponse(responseCode = "500", description = "Internal server error")   })
    public String test(@PathVariable(value = "var", required = true) String id) {
        return healtCheckService.methodX(id);
    }

    @GetMapping("/db")
    public ResponseEntity<String> checkDb() {
        try (Connection conn = dataSource.getConnection()) {
            boolean isValid = conn.isValid(2);
            return isValid
                    ? ResponseEntity.ok("DB OK")
                    : ResponseEntity.status(500).body("DB FAIL");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("DB ERROR: " + e.getMessage());
        }
    }

}
