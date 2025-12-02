package ar.com.st.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Tarea programada para limpiar validaciones expiradas
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LimpiarValidacionesTask {
    
    private final ValidacionEmailService validacionEmailService;
    
    /**
     * Limpia validaciones expiradas cada hora
     */
    @Scheduled(fixedRate = 3600000) // 1 hora en milisegundos
    public void limpiarValidacionesExpiradas() {
        try {
            validacionEmailService.limpiarValidacionesExpiradas();
        } catch (Exception e) {
            log.error("Error limpiando validaciones expiradas: {}", e.getMessage());
        }
    }
}
