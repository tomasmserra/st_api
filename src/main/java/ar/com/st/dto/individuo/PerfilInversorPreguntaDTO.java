package ar.com.st.dto.individuo;

import lombok.Data;
import java.util.List;

/**
 * DTO para pregunta del perfil de inversor
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class PerfilInversorPreguntaDTO {
    private Long id;
    private String nombreCorto;
    private String pregunta;
    private boolean habilitada;
    private List<PerfilInversorOpcionDTO> opciones;
}
