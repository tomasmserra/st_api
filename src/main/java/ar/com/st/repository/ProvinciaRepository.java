package ar.com.st.repository;

import ar.com.st.entity.Provincia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para manejo de provincias
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Repository
public interface ProvinciaRepository extends JpaRepository<Provincia, Long> {
    
    /**
     * Obtiene todas las provincias ordenadas por nombre
     */
    List<Provincia> findAllByOrderByNombreAsc();
    
    /**
     * Busca una provincia por su código
     */
    java.util.Optional<Provincia> findByCodigo(String codigo);
}

