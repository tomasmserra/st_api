package ar.com.st.controller;

import ar.com.st.dto.firmaDigital.EstadoDocumentoDTO;
import ar.com.st.entity.Persona;
import ar.com.st.entity.Solicitud;
import ar.com.st.entity.Usuario;
import ar.com.st.service.AuthService;
import ar.com.st.service.FirmaDigitalService;
import ar.com.st.service.SolicitudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador REST para manejo de firma digital de solicitudes
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@RestController
@RequestMapping("/api/firma-digital")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Firma Digital", description = "API para manejo de firma digital de solicitudes")
public class FirmaDigitalController {

    private final FirmaDigitalService firmaDigitalService;
    private final SolicitudService solicitudService;
    private final AuthService authService;

    /**
     * Valida que el usuario autenticado NO tenga rol CLIENTE.
     * Si tiene rol CLIENTE, retorna ResponseEntity con 403 Forbidden.
     * Si no es CLIENTE o no está autenticado, retorna null (acceso permitido).
     */
    private ResponseEntity<?> validarNoCliente() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Usuario usuario = authService.obtenerUsuarioPorUsername(authentication.getName())
                        .orElse(null);
                
                if (usuario != null) {
                    // Verificar si el usuario tiene rol CLIENTE
                    boolean esCliente = usuario.getRoles().stream()
                            .anyMatch(rol -> rol.getNombre() == ar.com.st.entity.Rol.NombreRol.CLIENTE);
                    
                    if (esCliente) {
                        log.warn("Intento de acceso no autorizado: Usuario {} (CLIENTE) intentó acceder a un método no permitido", 
                                usuario.getId());
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    }
                }
            }
            return null; // Acceso permitido
        } catch (Exception e) {
            log.error("Error validando acceso de cliente: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/{solicitudId}/enviar")
    @Operation(summary = "Enviar solicitud para firma digital", 
               description = "Envía el PDF de la solicitud para firma digital a los emails especificados o a los emails del titular y firmantes")
    public ResponseEntity<Void> enviarFirmaDigital(
            @Parameter(description = "ID de la solicitud") @PathVariable Long solicitudId,
            @Parameter(description = "Lista de emails adicionales para enviar la firma (opcional). Si no se proporciona, se usan los emails del titular y firmantes") 
            @RequestParam(required = false) List<String> emails) {
        
        log.info("Enviando firma digital para solicitud con ID: {}", solicitudId);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<Void>) validacion;
        }
        
        Solicitud solicitud = solicitudService.obtenerPorId(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + solicitudId));

        // Si no se proporcionan emails, obtener los emails del titular y firmantes
        final List<String> emailsFinales;
        if (emails == null || emails.isEmpty()) {
            List<String> emailsRecolectados = new ArrayList<>();
            
            // Agregar email del titular si es Persona
            if (solicitud.getTitular() instanceof Persona persona && persona.getCorreoElectronico() != null) {
                emailsRecolectados.add(persona.getCorreoElectronico());
            }
            
            // Agregar emails de los firmantes
            if (solicitud.getFirmantes() != null) {
                solicitud.getFirmantes().forEach(firmante -> {
                    if (firmante.getCorreoElectronico() != null) {
                        emailsRecolectados.add(firmante.getCorreoElectronico());
                    }
                });
            }
            
            emailsFinales = emailsRecolectados;
        } else {
            emailsFinales = emails;
        }

        if (emailsFinales.isEmpty()) {
            log.warn("No se encontraron emails para enviar la firma digital de la solicitud {}", solicitudId);
            return ResponseEntity.badRequest().build();
        }

        boolean exito = firmaDigitalService.enviarFirmaDigital(solicitud, emailsFinales);
        
        if (exito) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{solicitudId}/estado")
    @Operation(summary = "Obtener estado del documento de firma digital", 
               description = "Obtiene el estado actual del documento de firma digital de una solicitud")
    public ResponseEntity<EstadoDocumentoDTO> obtenerEstadoDocumento(
            @Parameter(description = "ID de la solicitud") @PathVariable Long solicitudId) {
        
        log.info("Obteniendo estado de firma digital para solicitud con ID: {}", solicitudId);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<EstadoDocumentoDTO>) validacion;
        }
        
        Solicitud solicitud = solicitudService.obtenerPorId(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + solicitudId));

        if (solicitud.getIdFirmaDigital() == null || solicitud.getIdFirmaDigital().isEmpty()) {
            log.warn("La solicitud {} no tiene ID de firma digital", solicitudId);
            return ResponseEntity.notFound().build();
        }

        EstadoDocumentoDTO estado = firmaDigitalService.obtenerEstadoDocumento(solicitud.getIdFirmaDigital());
        
        if (estado != null) {
            return ResponseEntity.ok(estado);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{solicitudId}/cancelar")
    @Operation(summary = "Cancelar documento de firma digital", 
               description = "Cancela el documento de firma digital de una solicitud")
    public ResponseEntity<Void> cancelarDocumento(
            @Parameter(description = "ID de la solicitud") @PathVariable Long solicitudId,
            @Parameter(description = "Razón de cancelación") @RequestParam String razon) {
        
        log.info("Cancelando documento de firma digital para solicitud con ID: {}", solicitudId);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<Void>) validacion;
        }
        
        Solicitud solicitud = solicitudService.obtenerPorId(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + solicitudId));

        if (solicitud.getIdFirmaDigital() == null || solicitud.getIdFirmaDigital().isEmpty()) {
            log.warn("La solicitud {} no tiene ID de firma digital", solicitudId);
            return ResponseEntity.notFound().build();
        }

        boolean exito = firmaDigitalService.cancelarDocumento(solicitud.getIdFirmaDigital(), razon);
        
        if (exito) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/firma/{idFirma}/reenviar-email")
    @Operation(summary = "Reenviar email de invitación para firmar", 
               description = "Reenvía el email de invitación para firmar a un firmante específico")
    public ResponseEntity<Void> reenviarEmailFirmaDigital(
            @Parameter(description = "ID de la firma") @PathVariable String idFirma) {
        
        log.info("Reenviando email de firma digital para firma con ID: {}", idFirma);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<Void>) validacion;
        }

        boolean exito = firmaDigitalService.reenviarEmailFirmaDigital(idFirma);
        
        if (exito) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{solicitudId}/pdf-firmado")
    @Operation(summary = "Obtener PDF firmado", 
               description = "Descarga el PDF firmado del documento de firma digital de una solicitud")
    public ResponseEntity<byte[]> obtenerDocumentoCertificado(
            @Parameter(description = "ID de la solicitud") @PathVariable Long solicitudId) {
        
        log.info("Obteniendo PDF firmado para solicitud con ID: {}", solicitudId);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<byte[]>) validacion;
        }
        
        Solicitud solicitud = solicitudService.obtenerPorId(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + solicitudId));

        if (solicitud.getIdFirmaDigital() == null || solicitud.getIdFirmaDigital().isEmpty()) {
            log.warn("La solicitud {} no tiene ID de firma digital", solicitudId);
            return ResponseEntity.notFound().build();
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            firmaDigitalService.obtenerDocumentoCertificado(solicitud.getIdFirmaDigital(), out);
            byte[] pdf = out.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "solicitud-" + solicitudId + "-firmado.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdf);

        } catch (Exception ex) {
            log.error("Error al obtener PDF firmado para la solicitud {}: {}", solicitudId, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{solicitudId}/certificado-firma")
    @Operation(summary = "Obtener certificado de firma", 
               description = "Descarga el certificado de firma del documento de firma digital de una solicitud")
    public ResponseEntity<byte[]> obtenerCertificadoFirma(
            @Parameter(description = "ID de la solicitud") @PathVariable Long solicitudId) {
        
        log.info("Obteniendo certificado de firma para solicitud con ID: {}", solicitudId);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<byte[]>) validacion;
        }
        
        Solicitud solicitud = solicitudService.obtenerPorId(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + solicitudId));

        if (solicitud.getIdFirmaDigital() == null || solicitud.getIdFirmaDigital().isEmpty()) {
            log.warn("La solicitud {} no tiene ID de firma digital", solicitudId);
            return ResponseEntity.notFound().build();
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            firmaDigitalService.obtenerCertificadoFirma(solicitud.getIdFirmaDigital(), out);
            byte[] certificado = out.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "certificado-firma-" + solicitudId + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(certificado);

        } catch (Exception ex) {
            log.error("Error al obtener certificado de firma para la solicitud {}: {}", solicitudId, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

