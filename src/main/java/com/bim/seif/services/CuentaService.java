package com.bim.seif.services;

import com.bim.seif.clients.FideicomisoClient;
import com.bim.seif.models.*;
import com.bim.seif.models.dto.CuentaAbonoDto;
import com.bim.seif.models.dto.CuentaCargoDto;
import com.bim.seif.models.mappers.CuentaMapper;
import com.bim.seif.repositories.CuentaRepository;
import com.bim.seif.repositories.DivisaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final DivisaRepository divisaRepository;
    private final FideicomisoClient fideicomisoClient;
    private final EmailService emailService;
    private final EmpleadoService empleadoService;

    public List<CuentaAbonoDto> obtenerCuentas(String fideicomisoFolio) {
        List<CuentaAbono> cuentas = cuentaRepository.findByFideicomisoFolioAndFechaBajaIsNull(fideicomisoFolio);
        return CuentaMapper.INSTANCE.toCuentaAbonoDtoList(cuentas);
    }

    public List<CuentaAbonoDto> obtenerCuentasAbonoByCuenta(Long cuenta){
        List<CuentaAbono> cuentas = cuentaRepository.findByCuenta(cuenta);
        return CuentaMapper.INSTANCE.toCuentaAbonoDtoList(cuentas);
    }

    public List<CuentaAbonoDto> obtenerCuentasMantenimiento(String fideicomisoFolio) {
        List<CuentaAbono> cuentas = cuentaRepository.findByFideicomisoFolio(fideicomisoFolio);
        return CuentaMapper.INSTANCE.toCuentaAbonoDtoList(cuentas);
    }

    public CuentaAbono registrarCuenta(CuentaAbono cuenta, String folioFideicomiso) {
        // Validar si ya existe
        if (cuentaRepository.existsById(cuenta.getCuenta())) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese número.");
        }

        if (cuenta.getDivisa() != null && cuenta.getDivisa().getCve() != null) {
            Divisa divisa = divisaRepository.findById(cuenta.getDivisa().getCve())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Divisa no encontrada: " + cuenta.getDivisa().getCve()));
            cuenta.setDivisa(divisa);
        }

        Fideicomiso fideicomiso = new Fideicomiso();
        fideicomiso.setFolio(folioFideicomiso);
        cuenta.setFideicomiso(fideicomiso);
        CuentaAbono c = cuentaRepository.save(cuenta);

        Map<Propiedad, String> variables = new HashMap<>();
        variables.put(Propiedad.cuenta_numero, cuenta.getCuenta().toString());


        emailService.enviarCorreoAsociados(folioFideicomiso, TipoEvento.cuenta_alta, variables);

        log.info("Cuenta registrada con exito: {}", c);

        return c;
    }

    public void desactivarCuenta(Long cuenta, boolean desactiva){
        CuentaAbono c = cuentaRepository.findById(cuenta).orElseThrow();
        if(desactiva){
            c.setFechaBaja(LocalDateTime.now());
        } else {
            c.setFechaBaja(null);
        }

        cuentaRepository.save(c);
    }


    public List<CuentaCargoDto> obtenerCuentasCargo(String fideicomisoFolio) {
        List<Divisa> divisas = divisaRepository.findAll();
        Divisa divisaDefault = new Divisa();
        divisaDefault.setTipoCambioMonedaNacional(1);
        return fideicomisoClient.obtenerCuentasPorFideicomiso(fideicomisoFolio).stream()
                .map(c -> {
                    c.getDivisa().setCve(c.getDivisa().getAbreviatura());
                    c.getDivisa().setTipoCambioMonedaNacional(divisas.stream().filter(d -> d.getCve()
                            .equalsIgnoreCase(c.getDivisa().getAbreviatura())).findFirst()
                            .orElse(divisaDefault).getTipoCambioMonedaNacional());
                    return c;
                }).collect(Collectors.toList());
    }

    public void actualizarCuenta(CuentaAbono cuenta){
        log.info("Actualizando cuenta con ID: {}", cuenta.getCuenta());
        CuentaAbono c = cuentaRepository.findById(cuenta.getCuenta()).orElseThrow();
//        c.setBanco(cuenta.getBanco());
//        c.setBeneficiario(cuenta.getBeneficiario());
        c.setDireccion(cuenta.getDireccion());
        c.setRfc(cuenta.getRfc());
        c.setRegimen(cuenta.getRegimen());
        cuentaRepository.save(c);
    }

    public List<CuentaAbonoDto> obtenerCuentasPorEjemplo(CuentaAbonoDto filtro, Pageable pageable) {
        CuentaAbono probe = new CuentaAbono();
        probe.setBanco(filtro.getBanco());
        probe.setRegimen(filtro.getRegimen());
        probe.setSolicitudCuenta(filtro.getSolicitudCuenta());

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withIgnorePaths("cuenta","direccion","rfc", "rutaArchivo")
                .withMatcher("banco", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase())
                .withMatcher("regimen", ExampleMatcher.GenericPropertyMatchers.exact());

        Example<CuentaAbono> example = Example.of(probe, matcher);

        return CuentaMapper.INSTANCE.toCuentaAbonoDtoList(cuentaRepository.findAll(example, pageable).getContent());
    }

    public List<CuentaAbonoDto> buscarCuentas(String userEmail, CuentaAbonoDto filtro, LocalDate desde, LocalDate hasta, Pageable pageable) {
        log.info("Buscando cuentas para el usuario: {} con filtro: {}", userEmail, filtro);
        CuentaAbono probe = new CuentaAbono();
        probe.setBanco(filtro.getBanco());
        probe.setRegimen(filtro.getRegimen());
        if(filtro.getDivisa() != null){
        Divisa divisa = new Divisa();
        divisa.setCve(filtro.getDivisa().getCve());
        probe.setDivisa(divisa);
        }
        if(filtro.getSolicitudCuenta() != null){
        probe.setSolicitudCuenta(filtro.getSolicitudCuenta());
        }
        log.info("Regiones del usuario {}: {}", userEmail, empleadoService.buscarEmpleado(userEmail).getRegiones()
                .stream().map(r -> r.getCve()).collect(Collectors.toSet()));
        return CuentaMapper.INSTANCE.toCuentaAbonoDtoList(cuentaRepository.findAll(filtroCuenta(probe, empleadoService.buscarEmpleado(userEmail).getRegiones()
                .stream().map(r -> r.getCve()).collect(Collectors.toSet()), desde, hasta), pageable).getContent());
    }

    private static Specification<CuentaAbono> filtroCuenta(CuentaAbono filtro, Set<String> regiones, LocalDate desde, LocalDate hasta) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if(!regiones.isEmpty()) {
                Join<CuentaAbono, Fideicomiso> joinFideicomiso = root.join("fideicomiso", JoinType.LEFT);
                Join<Fideicomiso, Region> regionJoin = joinFideicomiso.join("region", JoinType.LEFT);
                predicates.add(regionJoin.get("cve").in(regiones));
            }

            if (desde != null && hasta != null) {
                predicates.add(cb.between(root.get("fechaAlta"), desde, hasta));
            }

            if (filtro.getBanco() != null) {
                predicates.add(cb.like(cb.lower(root.get("banco")), "%" + filtro.getBanco().toLowerCase() + "%"));
            }

            if (filtro.getDivisa()!= null) {
                Join<CuentaAbono, Divisa> joinDivisa = root.join("divisa");
                predicates.add(cb.equal(joinDivisa.get("cve"), filtro.getDivisa().getCve()));
            }

            if(filtro.getRegimen() != null){
                predicates.add(cb.equal(root.get("regimen"), filtro.getRegimen()));
            }

            if (filtro.getSolicitudCuenta() != null) {
                predicates.add(cb.equal(root.get("solicitudCuenta"), filtro.getSolicitudCuenta()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }



    public void registrarComprobante(Long cuenta, String rutaComprobante){

        CuentaAbono c = cuentaRepository.findById(cuenta).orElseThrow();
        c.setRutaArchivo(rutaComprobante);
        cuentaRepository.save(c);
    }


}
