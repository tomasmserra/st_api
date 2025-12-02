package ar.com.st.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO para request de verificación de código
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class VerificarCodigoRequestDTO {
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    private String email;
    
    @NotBlank(message = "El código de verificación es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código debe tener exactamente 6 dígitos")
    private String codigo;
}
