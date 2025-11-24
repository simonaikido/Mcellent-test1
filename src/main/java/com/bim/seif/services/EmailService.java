package com.bim.seif.services;

import com.bim.seif.models.Contrato;
import com.bim.seif.models.Propiedad;
import com.bim.seif.models.TipoEvento;
import com.bim.seif.models.dto.EventoDto;
import com.bim.seif.repositories.ContratoRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final EventoService eventoService;
    private final ContratoRepository contratoRepository;

    @Value("${mail.from:Branchbit.1@bim.mx}")
    private String remitente;

    // Inyección por constructor: asegura que NADA quede en null.
    public EmailService(JavaMailSender mailSender,
                        EventoService eventoService,
                        ContratoRepository contratoRepository) {
        this.mailSender = mailSender;
        this.eventoService = eventoService;
        this.contratoRepository = contratoRepository;
    }

    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(remitente);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public void enviarCorreo(String destinatario, TipoEvento te, Map<Propiedad, String> data) {
        EventoDto evento = eventoService.obtenerEvento(te);
        String template = evento.getCuerpoCorreo();
        String cuerpoCorreo = renderTemplate(template, data);
        sendMimeEmail(destinatario, evento.getAsunto(), cuerpoCorreo);
    }

    public void enviarCorreoAsociados(String fideicomisoFolio, TipoEvento te, Map<Propiedad, String> data) {
        EventoDto evento = eventoService.obtenerEvento(te);
        String template = evento.getCuerpoCorreo();

        List<Contrato> contratos = contratoRepository.findByFideicomisoFolio(fideicomisoFolio);
        for (Contrato c : contratos) {
            // partir SIEMPRE del template original
            String cuerpo = renderTemplate(template, data);
            // reemplazo adicional por destinatario
            cuerpo = cuerpo.replace(Propiedad.cliente_nombre.toString(), c.getCliente().getNombre());
            sendMimeEmail(c.getCliente().getEmail(), evento.getAsunto(), cuerpo);
        }
    }

    private void sendMimeEmail(String to, String subject, String body) {
        MimeMessage mensaje = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setTo(to);
            helper.setCc(new String[] {"j.mendez@bim.mx","fpedraza@mcllent.com","aalcalar@mcllent.com","e.andrade@bim.mx"});
            helper.setSubject(subject);
            helper.setFrom(remitente);
            helper.setText(body, true); // true = HTML
            mailSender.send(mensaje);
        } catch (MessagingException e) {
            throw new RuntimeException("Error enviando correo MIME", e);
        }
    }

    /**
     * Reemplaza las variables del template a partir del mapa de Propiedad -> valor.
     * aquí asumo que en el template las llaves son exactamente Propiedad.toString().
     * Si usas otra convención (p.ej. {{otp}}), ajusta placeHolder(...) abajo.
     */
    private String renderTemplate(String template, Map<Propiedad, String> data) {
        log.info("Renderizando template de correo.");
        String out = template;
        if (data != null) {
            for (Map.Entry<Propiedad, String> e : data.entrySet()) {
                String token = placeHolder(e.getKey());   // Por si luego quieres envolver con {{...}}
                String value = e.getValue() == null ? "" : e.getValue();
                out = out.replace(token, value);          // IMPORTANTE: asignar el resultado
            }
        }
        return out;
    }

    /**
     * Si tus plantillas usan el nombre “crudo” (p.ej. OTP) entonces deja toString().
     * Si usas {{OTP}} o ${OTP}, cambia a:
     *   return "{{" + prop.toString() + "}}";
     */
    private String placeHolder(Propiedad prop) {

        return prop.toString();
    }

    public List<String> obtenerCorreosFideicomiso(String folio) {
        List<String> destinatarios = new ArrayList<>();
        // instruccionProgramadaRepository.obtenerCorreosFideicomiso(folio);
        destinatarios.add("gvazquezh@mcllent.com");

        return destinatarios;
    }
}