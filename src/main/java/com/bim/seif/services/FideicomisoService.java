package com.bim.seif.services;

import com.bim.seif.clients.FideicomisoClient;
import com.bim.seif.models.Fideicomiso;
import com.bim.seif.models.Region;
import com.bim.seif.models.dto.RegionDto;
import com.bim.seif.models.mappers.RegionMapper;
import com.bim.seif.repositories.FideicomisoRepository;
import com.bim.seif.repositories.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FideicomisoService {

    private final RegionRepository regionRepository;
    private final FideicomisoRepository fideicomisoRepository;
    private final FideicomisoClient fideicomisoClient;


    public List<RegionDto> obtenerRegiones(){
        List<Region> regiones = regionRepository.findAll();
        return RegionMapper.INSTANCE.toDto(regiones);
    }
    
    public void actualizarFideicomiso(String fideicomisoFolio,
                                      String responsableEmail,
                                      String regionCve,
                                      boolean instruccionesMonetarias,
                                      boolean instruccionesJuridicas){

        Fideicomiso fideicomiso = fideicomisoRepository.findById(fideicomisoFolio).orElseThrow();
        fideicomiso.setInstruccionesJuridicas(instruccionesMonetarias);
        fideicomiso.setInstruccionesMonetarias(instruccionesJuridicas);
        Region region = regionRepository.findById(regionCve).orElseThrow();
        fideicomiso.setRegion(region);
        if(responsableEmail != null){
            fideicomiso.setEncargadoEmail(responsableEmail);
        }
        fideicomisoRepository.save(fideicomiso);
        
    }

    public boolean verificarBloqueo(String fideicomisoFolio) {
       return  fideicomisoClient.verificarBloqueo(fideicomisoFolio);
    }



}
