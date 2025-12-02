package ar.com.st.dto.comun;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para creación/actualización de accionistas
 * 
 * @author Tomás Serra
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccionistaCreadoResponse {
    
    /**
     * Identificador del accionista creado o actualizado
     */
    private Long id;
    
    /**
     * Tipo de accionista: "PERSONA" o "EMPRESA"
     */
    private String tipo;
    
    /**
     * Indica si fue creado (true) o actualizado (false)
     */
    private Boolean creado;
}

