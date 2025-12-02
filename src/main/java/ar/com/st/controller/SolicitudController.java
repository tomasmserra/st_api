package ar.com.st.controller;

import ar.com.st.dto.solicitud.SolicitudResumenDTO;
import ar.com.st.entity.Solicitud;
import ar.com.st.entity.Usuario;
import ar.com.st.service.AuthService;
import ar.com.st.service.SolicitudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * Controlador REST para manejo de solicitudes de apertura de cuenta
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Solicitudes de Apertura de Cuenta", description = "API para manejo de solicitudes de apertura de cuenta")
public class SolicitudController {

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

    @PostMapping
    @Operation(summary = "Crear una nueva solicitud", description = "Crea una nueva solicitud de apertura de cuenta")
    public ResponseEntity<Solicitud> crearSolicitud(
            @Parameter(description = "Tipo de solicitud") @RequestParam Solicitud.Tipo tipo,
            @Parameter(description = "ID del usuario que carga la solicitud") @RequestParam Long idUsuarioCargo) {
        
        log.info("Creando nueva solicitud de tipo: {} para usuario: {}", tipo, idUsuarioCargo);
        
        // Validar que si el usuario autenticado tiene rol CLIENTE, 
        // solo pueda crear solicitudes para sí mismo
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Usuario usuario = authService.obtenerUsuarioPorUsername(authentication.getName())
                        .orElse(null);
                
                if (usuario != null) {
                    // Verificar si el usuario tiene rol CLIENTE
                    boolean esCliente = usuario.getRoles().stream()
                            .anyMatch(rol -> rol.getNombre() == ar.com.st.entity.Rol.NombreRol.CLIENTE);
                    
                    if (esCliente && !usuario.getId().equals(idUsuarioCargo)) {
                        log.warn("Intento de acceso no autorizado: Usuario {} (CLIENTE) intentó crear una solicitud para el usuario {}", 
                                usuario.getId(), idUsuarioCargo);
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error validando acceso de cliente: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Solicitud solicitud = solicitudService.crearSolicitud(tipo, idUsuarioCargo);
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitud);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una solicitud por ID", description = "Obtiene una solicitud específica por su ID")
    public ResponseEntity<Solicitud> obtenerPorId(@PathVariable Long id) {
        log.info("Obteniendo solicitud con ID: {}", id);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }
        
        return solicitudService.obtenerPorId(id)
                .map(solicitud -> ResponseEntity.ok(solicitud))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Obtener todas las solicitudes", description = "Obtiene todas las solicitudes de apertura de cuenta (vista resumida)")
    public ResponseEntity<List<SolicitudResumenDTO>> obtenerTodas() {
        log.info("Obteniendo todas las solicitudes");

        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<List<SolicitudResumenDTO>>) validacion;
        }

        List<SolicitudResumenDTO> resultado = solicitudService.obtenerTodasResumen();
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar solicitudes con filtros", description = "Busca solicitudes aplicando filtros de texto, tipo y estado")
    public ResponseEntity<List<Solicitud>> buscar(
            @Parameter(description = "Filtro de texto") @RequestParam(required = false) String filtro,
            @Parameter(description = "Tipos de solicitud") @RequestParam(required = false) Collection<Solicitud.Tipo> tipos,
            @Parameter(description = "Estados de solicitud") @RequestParam(required = false) Collection<Solicitud.Estado> estados) {
        
        log.info("Buscando solicitudes con filtro: {}, tipos: {}, estados: {}", filtro, tipos, estados);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<List<Solicitud>>) validacion;
        }
        
        List<Solicitud> solicitudes = solicitudService.buscar(filtro, tipos, estados);
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping("/estado/{estado}")
    @Operation(summary = "Obtener solicitudes por estado", description = "Obtiene todas las solicitudes con un estado específico")
    public ResponseEntity<List<Solicitud>> obtenerPorEstado(@PathVariable Solicitud.Estado estado) {
        log.info("Obteniendo solicitudes por estado: {}", estado);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<List<Solicitud>>) validacion;
        }
        
        List<Solicitud> solicitudes = solicitudService.obtenerPorEstado(estado);
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping("/tipo/{tipo}")
    @Operation(summary = "Obtener solicitudes por tipo", description = "Obtiene todas las solicitudes de un tipo específico")
    public ResponseEntity<List<Solicitud>> obtenerPorTipo(@PathVariable Solicitud.Tipo tipo) {
        log.info("Obteniendo solicitudes por tipo: {}", tipo);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<List<Solicitud>>) validacion;
        }
        
        List<Solicitud> solicitudes = solicitudService.obtenerPorTipo(tipo);
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping("/usuario/{idUsuario}")
    @Operation(summary = "Obtener solicitudes por usuario", description = "Obtiene todas las solicitudes de un usuario específico")
    public ResponseEntity<List<Solicitud>> obtenerPorUsuario(@PathVariable Long idUsuario) {
        log.info("Obteniendo solicitudes por usuario: {}", idUsuario);
        
        // Validar que si el usuario autenticado tiene rol CLIENTE, 
        // solo pueda ver sus propias solicitudes
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Usuario usuario = authService.obtenerUsuarioPorUsername(authentication.getName())
                        .orElse(null);
                
                if (usuario != null) {
                    // Verificar si el usuario tiene rol CLIENTE
                    boolean esCliente = usuario.getRoles().stream()
                            .anyMatch(rol -> rol.getNombre() == ar.com.st.entity.Rol.NombreRol.CLIENTE);
                    
                    if (esCliente && !usuario.getId().equals(idUsuario)) {
                        log.warn("Intento de acceso no autorizado: Usuario {} (CLIENTE) intentó acceder a solicitudes del usuario {}", 
                                usuario.getId(), idUsuario);
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error validando acceso de cliente: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<Solicitud> solicitudes = solicitudService.obtenerPorUsuario(idUsuario);
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping("/usuario/{idUsuario}/activa")
    @Operation(summary = "Obtener solicitud activa de un usuario", description = "Obtiene la solicitud activa de un usuario específico")
    public ResponseEntity<Solicitud> obtenerActivaPorUsuario(@PathVariable Long idUsuario) {
        log.info("Obteniendo solicitud activa para usuario: {}", idUsuario);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }
        
        return solicitudService.obtenerActivaPorUsuario(idUsuario)
                .map(solicitud -> ResponseEntity.ok(solicitud))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una solicitud", description = "Actualiza una solicitud existente")
    public ResponseEntity<Solicitud> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody Solicitud solicitud) {
        
        log.info("Actualizando solicitud con ID: {}", id);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }
        
        solicitud.setId(id);
        Solicitud solicitudActualizada = solicitudService.actualizar(solicitud);
        return ResponseEntity.ok(solicitudActualizada);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una solicitud", description = "Elimina una solicitud por su ID")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("Eliminando solicitud con ID: {}", id);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<Void>) validacion;
        }
        
        solicitudService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar una solicitud", description = "Cancela una solicitud existente")
    public ResponseEntity<Solicitud> cancelarSolicitud(@PathVariable Long id) {
        log.info("Cancelando solicitud con ID: {}", id);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }
        
        Solicitud solicitud = solicitudService.cancelarSolicitud(id);
        return ResponseEntity.ok(solicitud);
    }

    @PutMapping("/{id}/finalizar")
    @Operation(summary = "Finalizar la carga de una solicitud", description = "Finaliza la carga de una solicitud y la marca como pendiente de firma")
    public ResponseEntity<Solicitud> finalizarCarga(@PathVariable Long id) {
        log.info("Finalizando carga de solicitud con ID: {}", id);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }
        
        Solicitud solicitud = solicitudService.finalizarCarga(id);
        return ResponseEntity.ok(solicitud);
    }

    @PutMapping("/{id}/aprobar")
    @Operation(summary = "Aprobar una solicitud", description = "Aprueba una solicitud existente")
    public ResponseEntity<Solicitud> aprobarSolicitud(
            @PathVariable Long id,
            @Parameter(description = "Motivo de aprobación") @RequestParam(required = false) String motivo,
            @Parameter(description = "ID de la cuenta") @RequestParam Long idCuenta) {
        
        log.info("Aprobando solicitud con ID: {} con idCuenta: {}", id, idCuenta);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }
        
        Solicitud solicitud = solicitudService.aprobarSolicitud(id, motivo, idCuenta);
        return ResponseEntity.ok(solicitud);
    }

    @PutMapping("/{id}/rechazar")
    @Operation(summary = "Rechazar una solicitud", description = "Rechaza una solicitud existente")
    public ResponseEntity<Solicitud> rechazarSolicitud(
            @PathVariable Long id,
            @Parameter(description = "Motivo de rechazo") @RequestParam String motivo) {
        
        log.info("Rechazando solicitud con ID: {}", id);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }
        
        Solicitud solicitud = solicitudService.rechazarSolicitud(id, motivo);
        return ResponseEntity.ok(solicitud);
    }

    @GetMapping("/ultimo-id-cuenta")
    @Operation(summary = "Obtener el último ID de cuenta", description = "Obtiene el último ID de cuenta generado")
    public ResponseEntity<Integer> obtenerUltimoIdCuenta() {
        log.info("Obteniendo último ID de cuenta");
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<Integer>) validacion;
        }
        
        Integer ultimoId = solicitudService.obtenerUltimoIdCuenta();
        return ResponseEntity.ok(ultimoId);
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Generar PDF de una solicitud", description = "Genera y descarga el PDF de una solicitud")
    public ResponseEntity<byte[]> generarPdf(@PathVariable Long id) {
        log.info("Generando PDF para solicitud con ID: {}", id);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<byte[]>) validacion;
        }
        
        byte[] pdf = solicitudService.generarPdf(id);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=solicitud-" + id + ".pdf")
                .body(pdf);
    }

    @PostMapping("/{id}/enviar-bienvenida")
    @Operation(summary = "Enviar email de bienvenida", description = "Envía un email de bienvenida al cliente de la solicitud")
    public ResponseEntity<Void> enviarMailBienvenida(@PathVariable Long id) {
        log.info("Enviando email de bienvenida para solicitud con ID: {}", id);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<Void>) validacion;
        }
        
        solicitudService.enviarMailBienvenida(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/enviar-datos-acceso")
    @Operation(summary = "Enviar email con datos de acceso", description = "Envía un email con los datos de acceso (usuario y clave) al cliente")
    public ResponseEntity<Void> enviarDatosAcceso(
            @PathVariable Long id,
            @Parameter(description = "Usuario de acceso") @RequestParam String usuario,
            @Parameter(description = "Clave de acceso") @RequestParam String clave,
            @Parameter(description = "Correos electrónicos adicionales separados por coma") @RequestParam(required = false) String correosElectronicos) {
        
        log.info("Enviando email con datos de acceso para solicitud con ID: {}", id);
        
        ResponseEntity<?> validacion = validarNoCliente();
        if (validacion != null) {
            return (ResponseEntity<Void>) validacion;
        }
        
        solicitudService.enviarDatosAcceso(id, usuario, clave, correosElectronicos);
        return ResponseEntity.ok().build();
    }
}
