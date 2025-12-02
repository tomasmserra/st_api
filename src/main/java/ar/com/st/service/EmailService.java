package ar.com.st.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio para envío de emails
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Service
@Slf4j
public class EmailService {
   
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.from:registro@dealfs.com.ar}")
    private String fromEmail;
    
    @Value("${spring.mail.enabled:false}")
    private boolean emailEnabled;
    
    @Autowired(required = false)
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    /**
     * Envía un código de verificación por email
     */
    public void enviarCodigoVerificacion(String email, String codigo) {
        log.info("Intentando enviar código a: {}, emailEnabled: {}, mailSender: {}", 
                email, emailEnabled, mailSender != null ? "OK" : "NULL");
        
        if (!emailEnabled || mailSender == null) {
            log.warn("Envío de email deshabilitado. emailEnabled: {}, mailSender: {}. Código para {}: {}", 
                    emailEnabled, mailSender != null ? "OK" : "NULL", email, codigo);
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setFrom(fromEmail);
            message.setSubject("Código de verificación - ST Securities");
            message.setText(crearMensajeCodigoVerificacion(codigo));
            
            mailSender.send(message);
            log.info("Código de verificación {} enviado a: {}", email, codigo);
            
        } catch (Exception e) {
            log.error("Error enviando código de verificación a {}: {}", email, e.getMessage());
            throw new RuntimeException("Error enviando email de verificación", e);
        }
    }
    
    /**
     * Envía un email HTML a múltiples destinatarios con adjuntos opcionales
     * 
     * @param to Lista de destinatarios
     * @param subject Asunto del email
     * @param htmlBody Cuerpo del email en HTML
     * @param attachments Lista de adjuntos (puede ser null o vacía)
     */
    public void enviarEmailHtml(List<String> to, String subject, String htmlBody, List<AdjuntoEmail> attachments) {
        if (!emailEnabled || mailSender == null) {
            log.warn("Envío de email deshabilitado. emailEnabled: {}, mailSender: {}", 
                    emailEnabled, mailSender != null ? "OK" : "NULL");
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true indica que es HTML
            
            // Agregar adjuntos si existen
            if (attachments != null && !attachments.isEmpty()) {
                for (AdjuntoEmail adjunto : attachments) {
                    helper.addAttachment(adjunto.getNombreArchivo(), adjunto.getInputStreamSource());
                }
            }
            
            mailSender.send(message);
            log.info("Email HTML enviado a: {} con asunto: {}", to, subject);
            
        } catch (MessagingException e) {
            log.error("Error enviando email HTML a {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Error enviando email HTML", e);
        }
    }
    
    /**
     * Crea el mensaje del email con el código de verificación
     */
    private String crearMensajeCodigoVerificacion(String codigo) {
        return String.format("""
            Estimado cliente,
            
            Su código de verificación para acceder a ST Securities es:
            
            %s
            
            Este código es válido por 15 minutos.
            
            Si no solicitó este código, por favor ignore este email.
            
            Atentamente,
            Equipo ST Securities
            """, codigo);
    }
    
    /**
     * Clase auxiliar para representar un adjunto de email
     */
    public static class AdjuntoEmail {
        private final String nombreArchivo;
        private final InputStreamSource inputStreamSource;
        
        public AdjuntoEmail(String nombreArchivo, InputStreamSource inputStreamSource) {
            this.nombreArchivo = nombreArchivo;
            this.inputStreamSource = inputStreamSource;
        }
        
        public String getNombreArchivo() {
            return nombreArchivo;
        }
        
        public InputStreamSource getInputStreamSource() {
            return inputStreamSource;
        }
    }
}
