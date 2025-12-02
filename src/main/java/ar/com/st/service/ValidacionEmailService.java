package ar.com.st.service;

import ar.com.st.entity.ValidacionEmail;
import ar.com.st.repository.ValidacionEmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

/**
 * Servicio para manejo de validación de email
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidacionEmailService {
    
    private final ValidacionEmailRepository validacionEmailRepository;
    private final EmailService emailService;
    
    /**
     * Genera y envía un código de verificación por email
     */
    @Transactional
    public ValidacionEmail generarYEnviarCodigo(String email) {
        log.info("Generando código de verificación para: {}", email);
        
        // Verificar límite de intentos (máximo 5 por día)
        LocalDateTime hace24Horas = LocalDateTime.now().minusHours(24);
        Long intentosRecientes = validacionEmailRepository.countActiveByEmailSince(email, hace24Horas);
        
        if (intentosRecientes >= 5) {
            throw new RuntimeException("Se ha excedido el límite de códigos de verificación. Intente nuevamente mañana.");
        }
        
        // Desactivar validaciones anteriores del mismo email
        validacionEmailRepository.desactivarTodasPorEmail(email);
        
        // Generar código de 6 dígitos
        String codigo = generarCodigoVerificacion();
        
        // Crear nueva validación
        ValidacionEmail validacion = new ValidacionEmail(email, codigo);
        validacion = validacionEmailRepository.save(validacion);
        
        // Enviar email
        emailService.enviarCodigoVerificacion(email, codigo);
        
        log.info("Código de verificación generado para: {}", email);
        return validacion;
    }
    
    /**
     * Valida un código de verificación
     */
    @Transactional
    public boolean validarCodigo(String email, String codigo) {
        log.info("Validando código para: {}", email);
        
        Optional<ValidacionEmail> validacionOpt = validacionEmailRepository
                .findActiveByEmailAndCode(email, codigo, LocalDateTime.now());
        
        if (validacionOpt.isEmpty()) {
            log.warn("Código inválido para: {}", email);
            return false;
        }
        
        ValidacionEmail validacion = validacionOpt.get();
        
        // Verificar si está expirado
        if (validacion.isExpirado()) {
            log.warn("Código expirado para: {}", email);
            validacion.desactivar();
            validacionEmailRepository.save(validacion);
            return false;
        }
        
        
        // Código válido - desactivar la validación
        validacion.desactivar();
        validacionEmailRepository.save(validacion);
        
        log.info("Código validado exitosamente para: {}", email);
        return true;
    }
    
    
    /**
     * Obtiene la última validación activa de un email
     */
    public Optional<ValidacionEmail> obtenerUltimaValidacion(String email) {
        return validacionEmailRepository.findLastActiveByEmail(email);
    }
    
    /**
     * Limpia validaciones expiradas
     */
    @Transactional
    public void limpiarValidacionesExpiradas() {
        validacionEmailRepository.eliminarExpiradas(LocalDateTime.now());
        log.info("Validaciones expiradas eliminadas");
    }
    
    /**
     * Genera un código de verificación de 6 dígitos
     */
    private String generarCodigoVerificacion() {
        Random random = new Random();
        int codigo = 100000 + random.nextInt(900000); // Genera número entre 100000 y 999999
        return String.valueOf(codigo);
    }
}
