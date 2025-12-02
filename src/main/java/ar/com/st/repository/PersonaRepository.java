package ar.com.st.repository;

import ar.com.st.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para manejo de personas
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    /**
     * Busca personas por tipo
     */
    List<Persona> findByTipo(Persona.Tipo tipo);

    /**
     * Busca personas por número de documento
     */
    List<Persona> findByIdNumero(String idNumero);

    /**
     * Busca personas por email
     */
    List<Persona> findByCorreoElectronico(String correoElectronico);
}
