package ar.com.st.dto.individuo;

import lombok.Data;

/**
 * DTO para datos principales de individuo
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class IndividuoDatosPrincipalesDTO {
    private String nombres;
    private String apellidos;
    private String celular;
    private String correoElectronico;
    private String comoNosConocio;
    private Double porcentajeParticipacion;
}
