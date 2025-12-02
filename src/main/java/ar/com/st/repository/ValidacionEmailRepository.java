package ar.com.st.repository;

import ar.com.st.entity.ValidacionEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad ValidacionEmail
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Repository
public interface ValidacionEmailRepository extends JpaRepository<ValidacionEmail, Long> {
    
    /**
     * Busca una validación activa por email y código
     */
    @Query("SELECT v FROM ValidacionEmail v WHERE v.email = :email AND v.validationCode = :code AND v.activo = true AND v.fechaExpiracion > :now")
    Optional<ValidacionEmail> findActiveByEmailAndCode(@Param("email") String email, @Param("code") String code, @Param("now") LocalDateTime now);
    
    /**
     * Busca validaciones activas por email
     */
    @Query("SELECT v FROM ValidacionEmail v WHERE v.email = :email AND v.activo = true ORDER BY v.fecha DESC")
    List<ValidacionEmail> findActiveByEmail(@Param("email") String email);
    
    /**
     * Busca la última validación activa por email
     */
    @Query("SELECT v FROM ValidacionEmail v WHERE v.email = :email AND v.activo = true ORDER BY v.fecha DESC")
    Optional<ValidacionEmail> findLastActiveByEmail(@Param("email") String email);
    
    /**
     * Desactiva todas las validaciones de un email
     */
    @Modifying
    @Query("UPDATE ValidacionEmail v SET v.activo = false WHERE v.email = :email AND v.activo = true")
    void desactivarTodasPorEmail(@Param("email") String email);
    
    /**
     * Elimina validaciones expiradas
     */
    @Modifying
    @Query("DELETE FROM ValidacionEmail v WHERE v.fechaExpiracion < :now")
    void eliminarExpiradas(@Param("now") LocalDateTime now);
    
    /**
     * Cuenta las validaciones activas de un email en las últimas 24 horas
     */
    @Query("SELECT COUNT(v) FROM ValidacionEmail v WHERE v.email = :email AND v.activo = true AND v.fecha > :fecha")
    Long countActiveByEmailSince(@Param("email") String email, @Param("fecha") LocalDateTime fecha);
}
