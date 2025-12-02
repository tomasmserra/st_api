package ar.com.st.service;

import ar.com.st.entity.Rol;
import ar.com.st.entity.Usuario;
import ar.com.st.repository.RolRepository;
import ar.com.st.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servicio para manejo de autenticación y usuarios
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Registra un nuevo usuario
     */
    @Transactional
    public Usuario registrarUsuario(String username, String email, String password, String nombres, String apellidos) {
        log.info("Registrando nuevo usuario: {}", username);
        
        if (usuarioRepository.existsByUsername(username)) {
            throw new RuntimeException("El nombre de usuario ya existe");
        }
        
        if (usuarioRepository.existsByEmail(email)) {
            throw new RuntimeException("El email ya está registrado");
        }
        
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setActivo(true);
        usuario.setFechaCreacion(LocalDateTime.now());
        
        // Asignar rol de cliente por defecto
        Rol rolCliente = rolRepository.findByNombre(Rol.NombreRol.CLIENTE)
                .orElseThrow(() -> new RuntimeException("Rol CLIENTE no encontrado"));
        usuario.agregarRol(rolCliente);
        
        return usuarioRepository.save(usuario);
    }
    
    /**
     * Actualiza el último acceso del usuario
     */
    @Transactional
    public void actualizarUltimoAcceso(String username) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);
        }
    }
    
    /**
     * Obtiene un usuario por su nombre de usuario
     */
    public Optional<Usuario> obtenerUsuarioPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }
    
    /**
     * Obtiene un usuario por su email
     */
    public Optional<Usuario> obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }
    
    /**
     * Verifica si un usuario existe por nombre de usuario
     */
    public boolean existeUsuario(String username) {
        return usuarioRepository.existsByUsername(username);
    }
    
    /**
     * Verifica si un email está registrado
     */
    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }
    
    /**
     * Cambia la contraseña de un usuario
     */
    @Transactional
    public void cambiarPassword(String username, String nuevaPassword) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setPassword(passwordEncoder.encode(nuevaPassword));
            usuarioRepository.save(usuario);
            log.info("Contraseña actualizada para usuario: {}", username);
        } else {
            throw new RuntimeException("Usuario no encontrado");
        }
    }
    
    /**
     * Cambia la contraseña de un usuario validando la contraseña actual
     */
    @Transactional
    public void cambiarPasswordConValidacion(String username, String passwordActual, String passwordNueva) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }
        
        Usuario usuario = usuarioOpt.get();
        
        // Validar que la contraseña actual sea correcta
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }
        
        // Cambiar la contraseña
        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        usuarioRepository.save(usuario);
        log.info("Contraseña actualizada para usuario: {}", username);
    }
    
    /**
     * Activa o desactiva un usuario
     */
    @Transactional
    public void cambiarEstadoUsuario(String username, boolean activo) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setActivo(activo);
            usuarioRepository.save(usuario);
            log.info("Estado del usuario {} cambiado a: {}", username, activo ? "ACTIVO" : "INACTIVO");
        } else {
            throw new RuntimeException("Usuario no encontrado");
        }
    }
}
