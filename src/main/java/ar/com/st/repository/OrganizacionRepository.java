package ar.com.st.repository;

import ar.com.st.entity.Organizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para manejo de organizaciones
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Repository
public interface OrganizacionRepository extends JpaRepository<Organizacion, Long> {
}
