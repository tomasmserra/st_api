package ar.com.st.service;

import ar.com.st.dto.solicitud.SolicitudResumenDTO;
import ar.com.st.entity.Solicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para manejo de solicitudes de apertura de cuenta
 * @author Tomás Serra <tomas@serra.com.ar>
 */
public interface SolicitudService {

    /**
     * Crea una nueva solicitud del tipo indicado para el usuario
     */
    Solicitud crearSolicitud(Solicitud.Tipo tipo, Long idUsuarioCargo);

    /**
     * Obtiene una solicitud por ID
     */
    Optional<Solicitud> obtenerPorId(Long id);

    /**
     * Obtiene todas las solicitudes
     */
    List<Solicitud> obtenerTodas();

    /**
     * Obtiene solicitudes con paginación
     */
    Page<Solicitud> obtenerTodas(Pageable pageable);

    /**
     * Busca solicitudes según criterios
     */
    List<Solicitud> buscar(String filtro, Collection<Solicitud.Tipo> tipos, Collection<Solicitud.Estado> estados);

    /**
     * Obtiene solicitudes por estado
     */
    List<Solicitud> obtenerPorEstado(Solicitud.Estado estado);

    /**
     * Obtiene solicitudes por tipo
     */
    List<Solicitud> obtenerPorTipo(Solicitud.Tipo tipo);

    /**
     * Obtiene solicitudes por usuario
     */
    List<Solicitud> obtenerPorUsuario(Long idUsuario);

    /**
     * Obtiene la solicitud activa de un usuario
     */
    Optional<Solicitud> obtenerActivaPorUsuario(Long idUsuario);

    /**
     * Guarda una solicitud
     */
    Solicitud guardar(Solicitud solicitud);

    /**
     * Actualiza una solicitud
     */
    Solicitud actualizar(Solicitud solicitud);

    /**
     * Elimina una solicitud
     */
    void eliminar(Long id);

    /**
     * Cancela una solicitud
     */
    Solicitud cancelarSolicitud(Long id);

    /**
     * Finaliza la carga de una solicitud
     */
    Solicitud finalizarCarga(Long id);

    /**
     * Aprueba una solicitud
     */
    Solicitud aprobarSolicitud(Long id, String motivo, Long idCuenta);

    /**
     * Rechaza una solicitud
     */
    Solicitud rechazarSolicitud(Long id, String motivo);

    /**
     * Obtiene el último ID de cuenta
     */
    Integer obtenerUltimoIdCuenta();

    /**
     * Genera PDF de la solicitud
     */
    byte[] generarPdf(Long id);
    
    /**
     * Envía email de bienvenida a la solicitud
     */
    void enviarMailBienvenida(Long solicitudId);
    
    /**
     * Envía email con datos de acceso (usuario y clave)
     */
    void enviarDatosAcceso(Long solicitudId, String usuario, String clave, String correosElectronicos);
    
    /**
     * Obtiene todas las solicitudes como resumen (DTO)
     */
    List<SolicitudResumenDTO> obtenerTodasResumen();
    
    /**
     * Mapea una entidad Solicitud a su DTO de resumen
     */
    SolicitudResumenDTO mapearSolicitudResumen(Solicitud solicitud);
    
}
