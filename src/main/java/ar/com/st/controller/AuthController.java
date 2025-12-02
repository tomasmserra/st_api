package ar.com.st.controller;

import ar.com.st.dto.auth.*;
import ar.com.st.entity.Usuario;
import ar.com.st.security.JwtUtils;
import ar.com.st.service.AuthService;
import ar.com.st.service.ValidacionEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador para autenticación
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticación", description = "API para autenticación y registro de usuarios")
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final JwtUtils jwtUtils;
    private final ValidacionEmailService validacionEmailService;
    
    @PostMapping("/enviar-codigo")
    @Operation(summary = "Enviar código de verificación", description = "Envía un código de verificación al email del usuario")
    public ResponseEntity<?> enviarCodigo(@Valid @RequestBody EnviarCodigoRequestDTO request) {
        try {
            log.info("Enviando código de verificación a: {}", request.getEmail());
            
            validacionEmailService.generarYEnviarCodigo(request.getEmail());
            
            return ResponseEntity.ok(new MessageResponseDTO("Código de verificación enviado a su email"));
            
        } catch (Exception e) {
            log.error("Error enviando código a {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error al enviar el código de verificación. Intente nuevamente."));
        }
    }
    
    @PostMapping("/verificar-codigo")
    @Operation(summary = "Verificar código y obtener token", description = "Verifica el código de verificación y devuelve un token JWT")
    public ResponseEntity<?> verificarCodigo(@Valid @RequestBody VerificarCodigoRequestDTO request) {
        try {
            log.info("Verificando código para: {}", request.getEmail());
            
            // Verificar el código
            boolean codigoValido = validacionEmailService.validarCodigo(request.getEmail(), request.getCodigo());
            
            if (!codigoValido) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponseDTO("Código de verificación inválido o expirado"));
            }
            
            // Buscar o crear usuario por email
            Usuario usuario = authService.obtenerUsuarioPorEmail(request.getEmail())
                    .orElseGet(() -> {
                        // Crear usuario automáticamente si no existe
                        String username = request.getEmail().split("@")[0]; // Usar parte antes del @ como username
                        return authService.registrarUsuario(username, request.getEmail(), "temp_password", "", "");
                    });
            
            // Generar token JWT
            String jwt = jwtUtils.generateTokenFromUsername(usuario.getUsername());
            
            List<String> roles = usuario.getRoles().stream()
                    .map(rol -> rol.getNombre().name())
                    .collect(Collectors.toList());
            
            // Actualizar último acceso
            authService.actualizarUltimoAcceso(usuario.getUsername());
            
            LoginResponseDTO response = new LoginResponseDTO(
                jwt,
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getNombres(),
                usuario.getApellidos(),
                roles,
                System.currentTimeMillis() + jwtUtils.getJwtExpirationMs()
            );
            
            log.info("Verificación exitosa para: {}", request.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error verificando código para {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error al verificar el código. Intente nuevamente."));
        }
    }
    
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión (método tradicional)", description = "Autentica un usuario con username/password y devuelve un token JWT. Solo para roles ADMIN, OPERADOR, SUPERVISOR.")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            log.info("Intentando login tradicional para usuario: {}", loginRequest.getUsername());
            
            // Verificar que el usuario existe y no es CLIENTE
            Usuario usuario = authService.obtenerUsuarioPorUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Verificar que no es un cliente (los clientes deben usar el flujo de código)
            boolean esCliente = usuario.getRoles().stream()
                    .anyMatch(rol -> rol.getNombre() == ar.com.st.entity.Rol.NombreRol.CLIENTE);
            
            if (esCliente) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponseDTO("Los clientes deben usar el flujo de código de verificación. Use /api/auth/enviar-codigo"));
            }
            
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);
            
            List<String> roles = usuario.getRoles().stream()
                    .map(rol -> rol.getNombre().name())
                    .collect(Collectors.toList());
            
            // Actualizar último acceso
            authService.actualizarUltimoAcceso(loginRequest.getUsername());
            
            LoginResponseDTO response = new LoginResponseDTO(
                jwt,
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getNombres(),
                usuario.getApellidos(),
                roles,
                System.currentTimeMillis() + jwtUtils.getJwtExpirationMs()
            );
            
            log.info("Login exitoso para usuario: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error en login para usuario {}: {}", loginRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error al iniciar sesión. Verifique sus credenciales."));
        }
    }
    
    
    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Cierra la sesión del usuario actual")
    public ResponseEntity<?> logout() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                log.info("Logout para usuario: {}", authentication.getName());
                SecurityContextHolder.clearContext();
            }
            return ResponseEntity.ok(new MessageResponseDTO("Sesión cerrada exitosamente"));
        } catch (Exception e) {
            log.error("Error en logout: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error al cerrar sesión"));
        }
    }
    
    @GetMapping("/me")
    @Operation(summary = "Información del usuario", description = "Obtiene la información del usuario autenticado")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponseDTO("Usuario no autenticado"));
            }
            
            Usuario usuario = authService.obtenerUsuarioPorUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            List<String> roles = usuario.getRoles().stream()
                    .map(rol -> rol.getNombre().name())
                    .collect(Collectors.toList());
            
            LoginResponseDTO response = new LoginResponseDTO(
                null, // No incluimos el token en esta respuesta
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getNombres(),
                usuario.getApellidos(),
                roles,
                null
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error obteniendo información del usuario: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO("Error al obtener información del usuario"));
        }
    }
    
    @PostMapping("/cambiar-password")
    @Operation(summary = "Cambiar contraseña", description = "Cambia la contraseña del usuario autenticado")
    public ResponseEntity<?> cambiarPassword(@Valid @RequestBody CambiarPasswordRequestDTO request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponseDTO("Usuario no autenticado"));
            }
            
            String username = authentication.getName();
            
            // Validar que la nueva contraseña sea diferente a la actual
            if (request.getPasswordActual().equals(request.getPasswordNueva())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponseDTO("La nueva contraseña debe ser diferente a la actual"));
            }
            
            // Cambiar la contraseña validando la contraseña actual
            authService.cambiarPasswordConValidacion(
                username, 
                request.getPasswordActual(), 
                request.getPasswordNueva()
            );
            
            log.info("Contraseña cambiada exitosamente para usuario: {}", username);
            return ResponseEntity.ok(new MessageResponseDTO("Contraseña cambiada exitosamente"));
            
        } catch (RuntimeException e) {
            log.warn("Error al cambiar contraseña: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDTO(e.getMessage()));
        } catch (Exception e) {
            log.error("Error inesperado al cambiar contraseña: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponseDTO("Error al cambiar la contraseña. Intente nuevamente."));
        }
    }
}
