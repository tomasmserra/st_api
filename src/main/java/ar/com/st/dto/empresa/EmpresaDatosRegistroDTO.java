package ar.com.st.dto.empresa;

import java.time.LocalDate;

import ar.com.st.entity.Empresa;
import lombok.Data;

/**
 * DTO para datos de registro de la empresa
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class EmpresaDatosRegistroDTO {
    private Long solicitudId;
    private Empresa.LugarInscripcionRegistro lugarInscripcionRegistro;
    private String numeroRegistro;
    private String paisRegistro;
    private String provinciaRegistro;
    private String lugarRegistro;
    private LocalDate fechaRegistro;
    private String folio;
    private String libro;
    private String tomo;
}
