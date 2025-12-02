package ar.com.st.dto.comun;

import ar.com.st.entity.DatosFiscales;
import lombok.Data;

/**
 * DTO común para datos fiscales
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class DatosFiscalesDTO {
    private DatosFiscales.Tipo tipo;
    private String claveFiscal;
    private DatosFiscales.TipoIva tipoIva;
    private DatosFiscales.TipoGanancia tipoGanancia;
    private String residenciaFiscal;
    private boolean debeCompletarFiscalExterior;
}
