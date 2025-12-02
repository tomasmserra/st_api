package ar.com.st.service.impl;

import ar.com.st.entity.Auditoria;
import ar.com.st.entity.Solicitud;
import ar.com.st.entity.Usuario;
import ar.com.st.repository.AuditoriaRepository;
import ar.com.st.service.AuditoriaService;
import ar.com.st.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación del servicio de auditoría
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuditoriaServiceImpl implements AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final AuthService authService;

    @Override
    @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
    public void registrarAccion(Auditoria.TipoAccion tipoAccion, Solicitud solicitud,
            Auditoria.Resultado resultado, String mensaje, String detalles) {
        
        Auditoria auditoria = new Auditoria();
        auditoria.setTipoAccion(tipoAccion);
        auditoria.setFecha(LocalDateTime.now());
        
        // Obtener usuario autenticado
        Usuario usuario = null;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                usuario = authService.obtenerUsuarioPorUsername(authentication.getName())
                        .orElse(null);
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener el usuario autenticado para auditoría: {}", e.getMessage());
        }
        
        // Si no hay usuario autenticado, usar el usuarioCargo de la solicitud
        if (usuario == null && solicitud != null && solicitud.getUsuarioCargo() != null) {
            usuario = solicitud.getUsuarioCargo();
        }
        
        auditoria.setUsuario(usuario);
        auditoria.setSolicitud(solicitud);
        auditoria.setResultado(resultado);
        auditoria.setMensaje(mensaje);
        auditoria.setDetalles(detalles);
        
        if (solicitud != null && solicitud.getIdCuenta() != null) {
            auditoria.setIdCuenta(solicitud.getIdCuenta());
        }
        
        auditoriaRepository.save(auditoria);
        log.debug("Auditoría registrada: {} - {} - {}", tipoAccion, resultado, mensaje);
    }

    @Override
    @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
    public void registrarAccionExitosa(Auditoria.TipoAccion tipoAccion, Solicitud solicitud, String mensaje) {
        registrarAccion(tipoAccion, solicitud, Auditoria.Resultado.EXITOSO, mensaje, null);
    }

    @Override
    @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
    public void registrarAccionFallida(Auditoria.TipoAccion tipoAccion, Solicitud solicitud,
            String mensaje, String detalles) {
        registrarAccion(tipoAccion, solicitud, Auditoria.Resultado.FALLIDO, mensaje, detalles);
    }

    @Override
    public List<Auditoria> obtenerPorSolicitud(Solicitud solicitud) {
        return auditoriaRepository.findBySolicitudOrderByFechaDesc(solicitud);
    }

    @Override
    public List<Auditoria> obtenerPorTipoAccion(Auditoria.TipoAccion tipoAccion) {
        return auditoriaRepository.findByTipoAccionOrderByFechaDesc(tipoAccion);
    }
    
    @Override
    public List<Auditoria> obtenerTodas() {
        return auditoriaRepository.findAllByOrderByFechaDesc();
    }
}

