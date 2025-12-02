package ar.com.st.repository;

import ar.com.st.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad Usuario
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    /**
     * Busca un usuario por su nombre de usuario
     */
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<Usuario> findByUsername(@Param("username") String username);
    
    /**
     * Busca un usuario por su email
     */
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<Usuario> findByEmail(@Param("email") String email);
    
    /**
     * Verifica si existe un usuario con el nombre de usuario dado
     */
    boolean existsByUsername(String username);
    
    /**
     * Verifica si existe un usuario con el email dado
     */
    boolean existsByEmail(String email);
    
    /**
     * Busca usuarios activos por nombre de usuario
     */
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.roles WHERE u.username = :username AND u.activo = true")
    Optional<Usuario> findActiveByUsername(@Param("username") String username);
    
    /**
     * Busca usuarios activos por email
     */
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.roles WHERE u.email = :email AND u.activo = true")
    Optional<Usuario> findActiveByEmail(@Param("email") String email);
}
