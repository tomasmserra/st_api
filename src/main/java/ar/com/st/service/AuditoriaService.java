package ar.com.st.service;

import ar.com.st.entity.Auditoria;
import ar.com.st.entity.Solicitud;

import java.util.List;

/**
 * Servicio para manejo de auditoría del sistema
 * @author Tomás Serra <tomas@serra.com.ar>
 */
public interface AuditoriaService {

    /**
     * Registra una acción de auditoría
     *
     * @param tipoAccion tipo de acción realizada
     * @param solicitud solicitud relacionada (puede ser null)
     * @param resultado resultado de la acción (exitoso o fallido)
     * @param mensaje mensaje descriptivo
     * @param detalles detalles adicionales (puede ser null)
     */
    void registrarAccion(Auditoria.TipoAccion tipoAccion, Solicitud solicitud, 
            Auditoria.Resultado resultado, String mensaje, String detalles);

    /**
     * Registra una acción de auditoría exitosa
     *
     * @param tipoAccion tipo de acción realizada
     * @param solicitud solicitud relacionada (puede ser null)
     * @param mensaje mensaje descriptivo
     */
    void registrarAccionExitosa(Auditoria.TipoAccion tipoAccion, Solicitud solicitud, String mensaje);

    /**
     * Registra una acción de auditoría fallida
     *
     * @param tipoAccion tipo de acción realizada
     * @param solicitud solicitud relacionada (puede ser null)
     * @param mensaje mensaje descriptivo
     * @param detalles detalles del error (puede ser null)
     */
    void registrarAccionFallida(Auditoria.TipoAccion tipoAccion, Solicitud solicitud, 
            String mensaje, String detalles);

    /**
     * Obtiene todas las auditorías de una solicitud
     *
     * @param solicitud solicitud de la cual se desean obtener las auditorías
     * @return lista de auditorías ordenadas por fecha descendente
     */
    List<Auditoria> obtenerPorSolicitud(Solicitud solicitud);

    /**
     * Obtiene las auditorías por tipo de acción
     *
     * @param tipoAccion tipo de acción
     * @return lista de auditorías ordenadas por fecha descendente
     */
    List<Auditoria> obtenerPorTipoAccion(Auditoria.TipoAccion tipoAccion);
    
    /**
     * Obtiene todas las auditorías
     *
     * @return lista de todas las auditorías ordenadas por fecha descendente
     */
    List<Auditoria> obtenerTodas();
}

