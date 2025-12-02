package ar.com.st.dto.individuo;

import ar.com.st.entity.DatosFiscales;
import ar.com.st.entity.Persona;
import lombok.Data;

/**
 * DTO para datos del cónyuge
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class ConyugeDTO {
    private String nombres;
    private String apellidos;
    private Persona.TipoID tipoID;
    private String idNumero;
    private DatosFiscales.Tipo tipoClaveFiscal;
    private String claveFiscal;
}
