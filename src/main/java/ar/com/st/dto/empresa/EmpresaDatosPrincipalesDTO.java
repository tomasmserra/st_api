package ar.com.st.dto.empresa;

import ar.com.st.entity.Empresa;
import lombok.Data;

/**
 * DTO para datos principales de empresa
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class EmpresaDatosPrincipalesDTO {
    private Long solicitudId;
    private String denominacion;
    private Empresa.TipoEmpresa tipoEmpresa;
    private String telefono;
    private String celular;
    private String correoElectronico;
    private Empresa.UsoFirma usoFirma;
    private String actividad;
    private String comoNosConocio;
    private Double porcentajeParticipacion;
}
