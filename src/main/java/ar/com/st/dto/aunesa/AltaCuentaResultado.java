package ar.com.st.dto.aunesa;

import lombok.Data;

/**
 * DTO para el resultado de alta de cuenta en AUNESA
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class AltaCuentaResultado {
    
    private boolean exitoso;
    private String mensajeError;
}

