package ar.com.st.service;

import ar.com.st.entity.Rol;
import ar.com.st.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para inicializar roles por defecto
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RolService implements CommandLineRunner {
    
    private final RolRepository rolRepository;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        inicializarRoles();
    }
    
    /**
     * Inicializa los roles por defecto del sistema
     */
    private void inicializarRoles() {
        for (Rol.NombreRol nombreRol : Rol.NombreRol.values()) {
            if (!rolRepository.existsByNombre(nombreRol)) {
                Rol rol = new Rol(nombreRol, nombreRol.getDescripcion());
                rolRepository.save(rol);
                log.info("Rol creado: {}", nombreRol.name());
            }
        }
    }
}
