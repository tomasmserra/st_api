package ar.com.st.dto.comun;

import ar.com.st.entity.CuentaBancaria;
import lombok.Data;

import java.util.List;

/**
 * DTO común para cuentas bancarias
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class CuentasBancariasDTO {
    private List<CuentaBancaria> cuentasBancarias;
    private boolean debeCompletarCuentasBancariasExterior;
}
