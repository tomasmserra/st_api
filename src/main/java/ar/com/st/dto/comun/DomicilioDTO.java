package ar.com.st.dto.comun;

import ar.com.st.entity.Domicilio;
import lombok.Data;

/**
 * DTO común para domicilio
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class DomicilioDTO {
    private Domicilio.Tipo tipo;
    private String calle;
    private String numero;
    private String piso;
    private String depto;
    private String barrio;
    private String ciudad;
    private String provincia;
    private String pais;
    private String cp;
}
