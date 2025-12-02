package ar.com.st.repository;

import ar.com.st.entity.Localidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para manejo de localidades
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Repository
public interface LocalidadRepository extends JpaRepository<Localidad, Long> {
    
    /**
     * Obtiene todas las localidades de una provincia ordenadas por nombre
     */
    List<Localidad> findByProvinciaIdOrderByNombreAsc(Long provinciaId);
}

