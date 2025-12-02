package ar.com.st.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para request de envío de código de verificación
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class EnviarCodigoRequestDTO {
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    private String email;
}
