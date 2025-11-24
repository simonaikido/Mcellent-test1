package com.bim.seif.clients;

import com.bim.seif.config.ClientConfig;
import com.bim.seif.models.dto.CuentaCargoDto;
import com.bim.seif.models.dto.FideicomisoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "FIDEICOMISOS-API", url = "${gestor.api.url}", configuration = ClientConfig.class)
public interface FideicomisoClient {
    @GetMapping(value = "/{fideicomisoFolio}/cuentas")
    List<CuentaCargoDto> obtenerCuentasPorFideicomiso(@PathVariable String fideicomisoFolio);

    @GetMapping(value = "/{clienteEmail}")
    List<FideicomisoDto> obtenerFideicomisos(@PathVariable String clienteEmail);

    @GetMapping(value = "/bloqueados/{fideicomiso}")
    boolean verificarBloqueo(@PathVariable String fideicomiso);

    @GetMapping
    FideicomisoDto obtenerFideicomisoClientes(@RequestParam(name = "id", required = true) String fideicomisoFolio);



}
