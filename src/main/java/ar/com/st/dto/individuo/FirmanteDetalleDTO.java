package ar.com.st.dto.individuo;

import ar.com.st.dto.comun.CuentasBancariasDTO;
import ar.com.st.dto.comun.DatosFiscalesDTO;
import ar.com.st.dto.comun.DomicilioDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO contenedor para alta/actualización de firmantes con toda la información requerida.
 *
 * @author Tomás Serra
 */
@Data
public class FirmanteDetalleDTO {

    /**
     * Identificador del firmante. Presente únicamente para operaciones de actualización.
     */
    private Long id;

    @Valid
    @NotNull
    private IndividuoDatosPrincipalesDTO datosPrincipales;

    @Valid
    private IndividuoDatosPersonalesDTO datosPersonales;

    @Valid
    private IndividuoDeclaracionesDTO declaraciones;

    @Valid
    private CuentasBancariasDTO cuentasBancarias;

    @Valid
    private DatosFiscalesDTO datosFiscales;

    @Valid
    private DomicilioDTO domicilio;
}

