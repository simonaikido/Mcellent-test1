package com.bim.seif.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.bim.seif.models.dto.FoliosAsignadosResponse;
import com.bim.seif.repositories.InstruccionJuridicaRepository;
import com.bim.seif.repositories.InstruccionMonetariaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoliosAsignadosService {

    private final InstruccionMonetariaRepository monetariaRepo;
    private final InstruccionJuridicaRepository juridicaRepo;

    public FoliosAsignadosResponse listarFolios(String email) {
        List<String> monetarias = monetariaRepo.foliosMonetariasNoProgramadas(email);
        List<String> programadas = monetariaRepo.foliosMonetariasProgramadas(email);
        List<String> juridicas  = juridicaRepo.foliosJuridicas(email);

        return FoliosAsignadosResponse.builder()
                .email(email)
                .monetarias(monetarias)
                .programadas(programadas)
                .juridicas(juridicas)
                .build();
    }
}