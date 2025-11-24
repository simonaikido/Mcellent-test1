package com.bim.seif.services;

import com.bim.seif.models.dto.DeletionGuardResponse;
import com.bim.seif.repositories.InstruccionJuridicaRepository;
import com.bim.seif.repositories.InstruccionMonetariaRepository;
import com.bim.seif.repositories.InstruccionProgramadaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDeletionGuardService {

    private final InstruccionMonetariaRepository monetariaRepo;
    private final InstruccionJuridicaRepository juridicaRepo;
    private final InstruccionProgramadaRepository programadaRepo;

    @Transactional(readOnly = true)
    public DeletionGuardResponse evaluate(String correo) {
        String email = correo == null ? "" : correo.trim();
        long m = monetariaRepo.countPendientesOEnProcesoByEmail(email);
        long j = juridicaRepo.countPendientesOEnProcesoByEmail(email);
        long p = programadaRepo.countPendientesOEnProcesoByEmail(email);

        boolean ok = (m + j + p) == 0;

        DeletionGuardResponse.DeletionGuardResponseBuilder builder = DeletionGuardResponse.builder()
                .deletable(ok)
                .monetarias(m)
                .juridicas(j)
                .programadas(p);

        if (m > 0)
            builder.reason("Tiene instrucciones monetarias en estatus PR/PE asignadas.");
        if (j > 0)
            builder.reason("Tiene instrucciones no monetarias (jurídicas) en estatus PR/PE asignadas.");
        if (p > 0)
            builder.reason("Tiene instrucciones programadas en estatus PR/PE asignadas.");

        return builder.build();
    }
}