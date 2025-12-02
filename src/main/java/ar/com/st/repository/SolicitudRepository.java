package ar.com.st.repository;

import ar.com.st.entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para manejo de solicitudes
 * 
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    /**
     * Busca solicitudes por tipo
     */
    List<Solicitud> findByTipo(Solicitud.Tipo tipo);

    /**
     * Busca solicitudes por estado
     */
    List<Solicitud> findByEstado(Solicitud.Estado estado);

    /**
     * Busca solicitudes por usuario que la cargo
     */
    @Query("SELECT s FROM Solicitud s JOIN FETCH s.titular WHERE s.usuarioCargo.id = :idUsuarioCargo")
    List<Solicitud> findByUsuarioCargoId(@Param("idUsuarioCargo") Long idUsuarioCargo);

    /**
     * Busca la solicitud activa de un usuario
     */
    @Query("SELECT s FROM Solicitud s JOIN FETCH s.titular WHERE s.usuarioCargo.id = :idUsuario AND s.estado IN ('INCOMPLETA', 'PENDIENTE', 'PENTIENTE_FIRMA')")
    Optional<Solicitud> findActivaByUsuario(@Param("idUsuario") Long idUsuario);

    /**
     * Busca solicitudes con filtros
     */
    @Query("SELECT s FROM Solicitud s WHERE " +
            "(:filtro IS NULL OR " +
            "LOWER(s.comoNosConocio) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
            "LOWER(CAST(s.id AS string)) LIKE LOWER(CONCAT('%', :filtro, '%'))) AND " +
            "(:tipos IS NULL OR s.tipo IN :tipos) AND " +
            "(:estados IS NULL OR s.estado IN :estados)")
    List<Solicitud> buscarConFiltros(@Param("filtro") String filtro,
            @Param("tipos") Collection<Solicitud.Tipo> tipos,
            @Param("estados") Collection<Solicitud.Estado> estados);

    /**
     * Obtiene el último ID de cuenta
     */
    @Query("SELECT COALESCE(MAX(s.idCuenta), 0) FROM Solicitud s WHERE s.idCuenta IS NOT NULL")
    Integer obtenerUltimoIdCuenta();

    @Query("""
        select s
        from Solicitud s
        left join fetch TREAT(s.titular as Persona) p
        left join fetch TREAT(s.titular as Empresa) e
        where s.id = :id
      """)
      Optional<Solicitud> findByIdWithTitularSubtipos(@Param("id") Long id);

    /**
     * Obtiene todas las solicitudes con el titular cargado (usando JOIN FETCH)
     */
    @Query("SELECT DISTINCT s FROM Solicitud s LEFT JOIN FETCH s.titular")
    List<Solicitud> findAllWithTitular();

}
