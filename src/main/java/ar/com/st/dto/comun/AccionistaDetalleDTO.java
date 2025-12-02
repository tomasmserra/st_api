package ar.com.st.dto.comun;

import ar.com.st.dto.empresa.EmpresaAccionistaDetalleDTO;
import ar.com.st.dto.individuo.FirmanteDetalleDTO;
import jakarta.validation.Valid;
import lombok.Data;

/**
 * DTO unificado para accionistas (Persona o Empresa)
 * 
 * @author Tomás Serra
 */
@Data
public class AccionistaDetalleDTO {

    /**
     * Identificador del accionista. Presente únicamente para operaciones de actualización.
     */
    private Long id;

    /**
     * Tipo de accionista: "PERSONA" o "EMPRESA"
     */
    private String tipo;

    /**
     * Datos de Persona (si tipo == "PERSONA")
     */
    @Valid
    private FirmanteDetalleDTO datosPersona;

    /**
     * Datos de Empresa (si tipo == "EMPRESA")
     */
    @Valid
    private EmpresaAccionistaDetalleDTO datosEmpresa;
}

