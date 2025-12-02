package ar.com.st.repository;

import ar.com.st.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para manejo de empresas
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    /**
     * Busca empresas por denominación
     */
    List<Empresa> findByDenominacionContainingIgnoreCase(String denominacion);

    /**
     * Busca empresas por tipo
     */
    List<Empresa> findByTipoEmpresa(Empresa.TipoEmpresa tipoEmpresa);

    /**
     * Busca empresas por email
     */
    List<Empresa> findByCorreoElectronico(String correoElectronico);
}
