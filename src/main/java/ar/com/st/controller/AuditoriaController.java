package ar.com.st.controller;

import ar.com.st.entity.Auditoria;
import ar.com.st.entity.Solicitud;
import ar.com.st.service.AuditoriaService;
import ar.com.st.service.SolicitudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controlador REST para manejo de auditoría
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auditoría", description = "API para consultar registros de auditoría")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;
    private final SolicitudService solicitudService;

    @GetMapping("/solicitud/{solicitudId}")
    @Operation(summary = "Obtener auditorías por solicitud", 
               description = "Obtiene todas las auditorías relacionadas con una solicitud específica")
    public ResponseEntity<List<Auditoria>> obtenerPorSolicitud(
            @Parameter(description = "ID de la solicitud") @PathVariable Long solicitudId) {
        
        log.info("Obteniendo auditorías para solicitud con ID: {}", solicitudId);
        
        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Solicitud solicitud = solicitudOpt.get();
        List<Auditoria> auditorias = auditoriaService.obtenerPorSolicitud(solicitud);
        
        return ResponseEntity.ok(auditorias);
    }

    @GetMapping("/tipo-accion/{tipoAccion}")
    @Operation(summary = "Obtener auditorías por tipo de acción", 
               description = "Obtiene todas las auditorías de un tipo de acción específico")
    public ResponseEntity<List<Auditoria>> obtenerPorTipoAccion(
            @Parameter(description = "Tipo de acción") @PathVariable String tipoAccion) {
        
        log.info("Obteniendo auditorías para tipo de acción: {}", tipoAccion);
        
        try {
            Auditoria.TipoAccion tipo = Auditoria.TipoAccion.valueOf(tipoAccion.toUpperCase());
            List<Auditoria> auditorias = auditoriaService.obtenerPorTipoAccion(tipo);
            return ResponseEntity.ok(auditorias);
        } catch (IllegalArgumentException e) {
            log.warn("Tipo de acción inválido: {}", tipoAccion);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/todas")
    @Operation(summary = "Obtener todas las auditorías", 
               description = "Obtiene todas las auditorías del sistema")
    public ResponseEntity<List<Auditoria>> obtenerTodas() {
        
        log.info("Obteniendo todas las auditorías");
        
        List<Auditoria> auditorias = auditoriaService.obtenerTodas();
        return ResponseEntity.ok(auditorias);
    }
}

