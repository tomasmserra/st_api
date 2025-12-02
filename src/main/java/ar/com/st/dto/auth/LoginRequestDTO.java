package ar.com.st.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para request de login
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class LoginRequestDTO {
    
    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String username;
    
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
