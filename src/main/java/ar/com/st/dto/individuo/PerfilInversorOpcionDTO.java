package ar.com.st.dto.individuo;

import lombok.Data;

/**
 * DTO para opción del perfil de inversor
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class PerfilInversorOpcionDTO {
    private Long id;
    private String valor;
    private Integer puntaje;
    private boolean determinante;
    private String tipoPerfil;
}
