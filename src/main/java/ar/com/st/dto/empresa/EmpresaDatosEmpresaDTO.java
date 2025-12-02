package ar.com.st.dto.empresa;

import java.time.LocalDate;

import lombok.Data;

/**
 * DTO para datos de la empresa
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class EmpresaDatosEmpresaDTO {
    private Long solicitudId;
    private LocalDate fechaConstitucion;
    private String numeroActa;
    private String paisOrigen;
    private String paisResidencia;
    private LocalDate fechaCierreBalance;
}
