package ar.com.st.repository;

import ar.com.st.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad Rol
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {
    
    /**
     * Busca un rol por su nombre
     */
    Optional<Rol> findByNombre(Rol.NombreRol nombre);
    
    /**
     * Verifica si existe un rol con el nombre dado
     */
    boolean existsByNombre(Rol.NombreRol nombre);
}
