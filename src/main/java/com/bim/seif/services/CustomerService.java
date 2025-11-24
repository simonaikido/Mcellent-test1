package com.bim.seif.services;

import com.bim.seif.clients.FideicomisoClient;
import com.bim.seif.models.Cliente;
import com.bim.seif.models.Contrato;
import com.bim.seif.models.Fideicomiso;
import com.bim.seif.models.dto.*;
import com.bim.seif.models.mappers.ClienteMapper;
import com.bim.seif.repositories.ContratoRepository;
import com.bim.seif.repositories.CustomerRespository;
import com.bim.seif.repositories.FideicomisoRepository;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CustomerService {

    private final CustomerRespository customerRespository;
    private final ContratoRepository contratoRepository;
    private final FideicomisoClient fideicomisoClient;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ClienteMapper clienteMapper;
    private final FideicomisoRepository fideicomisoRepository;

    private static final String CARACTERES = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*_-+=!?";
    private static final int LONGITUD = 8;
    private static final SecureRandom random = new SecureRandom();

    public CustomerService(CustomerRespository customerRespository, ContratoRepository contratoRepository,
            FideicomisoClient fideicomisoClient, EmailService emailService, PasswordEncoder passwordEncoder,
            ClienteMapper clienteMapper,
            FideicomisoRepository fideicomisoRepository) {

        this.customerRespository = customerRespository;
        this.contratoRepository = contratoRepository;
        this.fideicomisoClient = fideicomisoClient;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.clienteMapper = clienteMapper;
        this.fideicomisoRepository = fideicomisoRepository;
    }

    public Cliente obtenerClientePorEmail(String email) {
        return customerRespository.findByEmail(email);
    }

    public ClienteDto registrarCliente(ClienteRequestDto clienteDto) throws Exception {
        List<FideicomisoDto> fideicomisos = this.fideicomisoClient.obtenerFideicomisos(clienteDto.getEmail());
        ClienteDto dto = new ClienteDto();
        Cliente cliente = new Cliente();
        if (!fideicomisos.isEmpty()) {
            cliente.setEmail(clienteDto.getEmail());
            cliente.setNombre(clienteDto.getNombre());
            String facultad = clienteDto.getFacultad();
            // Comprobamos si 'facultad' NO es null y si contiene "instruir"
            boolean esPropietario = facultad != null && facultad.contains("instruir");
            cliente.setPropietario(esPropietario); // Se usa el valor booleano
                                                   // validadocliente.setIdioma(clienteDto.getIdioma());
            String codigo = generarCodigo();
            cliente.setPassword(passwordEncoder.encode(digest(codigo)));
            String nombreCompleto = clienteDto.getNombre();
            if (nombreCompleto != null && !nombreCompleto.trim().isEmpty()) {
                // Dividimos por espacios
                String[] partes = nombreCompleto.trim().split("\\s+");

                if (partes.length >= 3) {
                    // Primer elemento = apellido paterno
                    cliente.setApellidoPaterno(partes[0]);

                    // Segundo elemento = apellido materno
                    cliente.setApellidoMaterno(partes[1]);

                    // El resto = nombres
                    StringBuilder nombres = new StringBuilder();
                    for (int i = 2; i < partes.length; i++) {
                        nombres.append(partes[i]).append(" ");
                    }
                    cliente.setNombre(nombres.toString().trim());
                } else {
                    // Caso fallback: si no cumple la estructura esperada
                    cliente.setApellidoPaterno(partes[0]);
                    if (partes.length == 2) {
                        cliente.setNombre(partes[1]);
                    }
                }
            }

            this.customerRespository.save(cliente);

            Contrato contrato = new Contrato();
            Contrato.ContratoId id = new Contrato.ContratoId();
            id.setClienteId(cliente.getId());
            id.setFideicomisoFolio(clienteDto.getFideicomisoFolio());
            contrato.setId(id);
            contrato.setCliente(cliente);
            Fideicomiso f = new Fideicomiso();
            f.setFolio(clienteDto.getFideicomisoFolio());
            contrato.setFideicomiso(f);
            contrato.setParticipacion(clienteDto.getFacultad());
            this.contratoRepository.save(contrato);

            if (clienteDto.getIdioma().contains("es")) {
                // emailService.sendEmail(clienteDto.getEmail(),
                emailService.sendEmail("gvazquezh@mcllent.com",
                        "Clave acceso seif",
                        "email bienvenida en espaniol, contrasenia: \n " + codigo);
            } else if (clienteDto.getIdioma().contains("en")) {
                emailService.sendEmail("gvazquezh@mcllent.com",
                        "Clave acceso seif",
                        "email welcome in ingles password: \n " + codigo);
            } else {
                emailService.sendEmail("gvazquezh@mcllent.com",
                        "Clave acceso seif",
                        "email welcome en espaniol e ingles password: \n " + codigo);
            }

        } else {
            log.warn("No se encontro registro del cliente con email: {}", clienteDto.getEmail());
            throw new Exception("No se encontro registro del cliente");
        }

        return dto;
    }

    public List<ClienteDto> obtenerClientes(LocalDate desde, LocalDate hasta, ClienteDto cliente, int page, int size,
            Sort by) {

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withIgnorePaths("password", "email", "fechaBaja", "fechaAlta", "id", "nombre", "apellidoPaterno",
                        "apellidoMaterno", "propietario");

        return clienteMapper.toDto(customerRespository
                .findAll(Example.of(clienteMapper.toEntity(cliente), matcher), PageRequest.of(page, size, by))
                .getContent()
                .stream().filter(s -> {
                    LocalDateTime a = s.getFechaAlta();
                    return (a != null && !a.isBefore(desde.atStartOfDay()) && !a.isAfter(hasta.atTime(23, 59)));
                }).collect(Collectors.toList()));
    }

    private String digest(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(texto.getBytes("UTF-8"));

            // Convertir el hash a hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar hash SHA-256", e);
        }
    }

    public void desactivarCliente(String email, boolean disabled) {
        Cliente cliente = this.customerRespository.findByEmail(email);

        if (cliente != null) {
            if (disabled) {
                cliente.setFechaBaja(LocalDateTime.now());
            } else {
                cliente.setFechaBaja(null);
            }
        }
        this.customerRespository.save(cliente);
    }

    public FideicomisoDto obtenerClientesFideicomiso(String folio) {
        FideicomisoDto f = fideicomisoClient.obtenerFideicomisoClientes(folio);

        for (FideicomisoContactoDto c : f.getClientes()) {
            // normaliza email
            String emailLimpio = c.getEmail().startsWith("Email: ")
                    ? c.getEmail().substring("Email: ".length())
                    : c.getEmail();

            Cliente cliente = customerRespository.findByEmail(emailLimpio);

            // inicializa campos de comparación
            c.setDesincronizado(false);
            c.setCambios(new java.util.ArrayList<>());
            c.setFacultadMicro(c.getFacultad()); // lo que viene del micro

            if (cliente != null) {
                c.setActivo(true);
                c.setIdioma(cliente.getIdioma());
                c.setFechaBaja(cliente.getFechaBaja());

                // idioma BD/micro (por ahora el micro no manda idioma => igualamos)
                c.setIdiomaDb(cliente.getIdioma());
                c.setIdiomaMicro(cliente.getIdioma());

                // buscar participación en BD (Contrato)
                contratoRepository.findByClienteAndFolio(cliente.getId(), folio).ifPresentOrElse(contrato -> {
                    String facultadDb = contrato.getParticipacion();
                    c.setFacultadDb(facultadDb);

                    String micro = (c.getFacultadMicro() == null) ? "" : c.getFacultadMicro();
                    String bd = (facultadDb == null) ? "" : facultadDb;

                    if (!bd.equalsIgnoreCase(micro)) {
                        c.getCambios().add(
                                "Facultad diferente: BD='" + facultadDb + "', Micro='" + c.getFacultadMicro() + "'");
                        c.setDesincronizado(true);
                    }
                }, () -> {
                    c.setFacultadDb(null);
                    if (c.getFacultadMicro() != null && !c.getFacultadMicro().isBlank()) {
                        c.getCambios().add("Facultad existe en Micro pero no en BD: '" + c.getFacultadMicro() + "'");
                        c.setDesincronizado(true);
                    }
                });

            } else {
                // no existe en BD
                c.setActivo(false);
                c.setFacultadDb(null);
                c.setIdiomaDb(null);
                c.setIdiomaMicro(c.getIdioma()); // o null si prefieres
                c.getCambios().add("Cliente no existe en BD (sólo en Micro).");
                c.setDesincronizado(true);
            }
        }

        // mantén tu lógica de instrucciones
        fideicomisoRepository.findById(folio).ifPresent(fe -> {
            f.setInstruccionesMonetarias(fe.getInstruccionesMonetarias());
            f.setInstruccionesJuridicas(fe.getInstruccionesJuridicas());
        });

        return f;
    }

    private String generarCodigo() {
        StringBuilder codigo = new StringBuilder(LONGITUD);
        for (int i = 0; i < LONGITUD; i++) {
            int index = random.nextInt(CARACTERES.length());
            codigo.append(CARACTERES.charAt(index));
        }
        return codigo.toString();
    }

}
