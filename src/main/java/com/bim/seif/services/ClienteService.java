package com.bim.seif.services;

import com.bim.seif.models.Cliente;
import com.bim.seif.repositories.ClienteRespository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ClienteService {

    ClienteRespository clienteRespository;

    public ClienteService(ClienteRespository clienteRespository) {
        this.clienteRespository = clienteRespository;
    }

    public Cliente obtenerClientePorEmail(String email){
       return clienteRespository.findByEmail(email);
    }
}
