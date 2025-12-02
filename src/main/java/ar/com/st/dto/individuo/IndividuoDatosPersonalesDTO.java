package ar.com.st.dto.individuo;

import ar.com.st.entity.Persona;   
import java.time.LocalDate;
import lombok.Data;

/**
 * DTO para datos personales de individuo
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class IndividuoDatosPersonalesDTO {
    private Persona.TipoID tipoID;
    private String idNumero;
    private LocalDate fechaNacimiento;
    private String lugarNacimiento;
    private String nacionalidad;
    private String paisOrigen;
    private String paisResidencia;
    private String actividad;
    private Persona.Sexo sexo;
    private Persona.EstadoCivil estadoCivil;
    private Long dniFrenteArchivoId; // ID del archivo del DNI - frente
    private Long dniReversoArchivoId; // ID del archivo del DNI - reverso
    // Datos del cónyuge (condicional)
    private ConyugeDTO conyuge;
}
