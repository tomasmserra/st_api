package ar.com.st.repository;

import ar.com.st.entity.Auditoria;
import ar.com.st.entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository para la entidad Auditoria
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    
    /**
     * Obtiene todas las auditorías de una solicitud
     * @param solicitud solicitud de la cual se desean obtener las auditorías
     * @return lista de auditorías ordenadas por fecha descendente
     */
    List<Auditoria> findBySolicitudOrderByFechaDesc(Solicitud solicitud);
    
    /**
     * Obtiene las auditorías por tipo de acción
     * @param tipoAccion tipo de acción
     * @return lista de auditorías ordenadas por fecha descendente
     */
    List<Auditoria> findByTipoAccionOrderByFechaDesc(Auditoria.TipoAccion tipoAccion);
    
    /**
     * Obtiene las auditorías por solicitud y tipo de acción
     * @param solicitud solicitud
     * @param tipoAccion tipo de acción
     * @return lista de auditorías ordenadas por fecha descendente
     */
    List<Auditoria> findBySolicitudAndTipoAccionOrderByFechaDesc(Solicitud solicitud, Auditoria.TipoAccion tipoAccion);
    
    /**
     * Obtiene todas las auditorías ordenadas por fecha descendente
     * @return lista de todas las auditorías ordenadas por fecha descendente
     */
    List<Auditoria> findAllByOrderByFechaDesc();
}

