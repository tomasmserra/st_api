package ar.com.st.dto.individuo;

import lombok.Data;

/**
 * DTO para declaraciones de individuo
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class IndividuoDeclaracionesDTO {
    private boolean esPep;
    private String motivoPep;
    private boolean esFATCA;
    private String motivoFatca;
    private boolean declaraUIF;
    private String motivoUIF;
}
