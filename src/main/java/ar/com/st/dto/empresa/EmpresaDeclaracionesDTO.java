package ar.com.st.dto.empresa;

import lombok.Data;

/**
 * DTO para declaraciones de empresa
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class EmpresaDeclaracionesDTO {
    private Long solicitudId;
    private boolean declaraUIF;
    private String motivoUIF;
    private boolean esFATCA;
    private String motivoFatca;
}
