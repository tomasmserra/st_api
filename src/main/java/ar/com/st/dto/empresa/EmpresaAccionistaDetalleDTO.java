package ar.com.st.dto.empresa;

import ar.com.st.dto.comun.AccionistaDetalleDTO;
import ar.com.st.dto.comun.DatosFiscalesDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * DTO contenedor para alta/actualización de empresas accionistas con toda la información requerida.
 *
 * @author Tomás Serra
 */
@Data
public class EmpresaAccionistaDetalleDTO {

    /**
     * Identificador de la empresa. Presente únicamente para operaciones de actualización.
     */
    private Long id;

    @Valid
    @NotNull
    private EmpresaDatosPrincipalesDTO datosPrincipales;

    @Valid
    @NotNull
    private DatosFiscalesDTO datosFiscales;

    /**
     * Lista de accionistas de la empresa (puede contener Personas o Empresas)
     * Permite estructura recursiva: una Empresa accionista puede tener sus propios accionistas
     */
    @Valid
    private List<AccionistaDetalleDTO> accionistas;
}

