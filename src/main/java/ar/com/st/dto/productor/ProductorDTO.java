package ar.com.st.dto.productor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para crear y actualizar productores
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class ProductorDTO {
    
    private Long id;
    
    @NotBlank(message = "El nombre es requerido")
    private String nombres;
    
    @NotBlank(message = "El apellido es requerido")
    private String apellidos;
    
    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe tener un formato válido")
    private String correoElectronico;
    
    private String celular;
}

