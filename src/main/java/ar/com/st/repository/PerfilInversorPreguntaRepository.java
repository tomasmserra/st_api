package ar.com.st.repository;

import ar.com.st.entity.PerfilInversorPregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository para preguntas del perfil de inversor
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Repository
public interface PerfilInversorPreguntaRepository extends JpaRepository<PerfilInversorPregunta, Long> {
    
    /**
     * Obtiene todas las preguntas habilitadas del perfil de inversor
     * @return Lista de preguntas habilitadas
     */
    List<PerfilInversorPregunta> findByHabilitadaTrue();
}
