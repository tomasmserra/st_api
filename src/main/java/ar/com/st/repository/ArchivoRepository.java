package ar.com.st.repository;

import ar.com.st.entity.Archivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para manejo de archivos
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Repository
public interface ArchivoRepository extends JpaRepository<Archivo, Long> {
}
