package ar.com.st.dto.comun;

import lombok.Data;

/**
 * DTO común para términos y condiciones
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class TerminosCondicionesDTO {
    private Long solicitudId;
    private boolean aceptaTerminosCondiciones;
    private boolean aceptaReglamentoGestionFondos;
    private boolean aceptaComisiones;
}
