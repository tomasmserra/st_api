package ar.com.st.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para solicitud de cambio de contraseña
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class CambiarPasswordRequestDTO {
    
    @NotBlank(message = "La contraseña actual es requerida")
    private String passwordActual;
    
    @NotBlank(message = "La nueva contraseña es requerida")
    private String passwordNueva;
}

