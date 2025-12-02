package ar.com.st.dto.individuo;

import ar.com.st.entity.Solicitud;
import lombok.Data;

import java.util.List;

/**
 * DTO para declaración de origen de fondos de individuo
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class IndividuoDdJJOrigenFondosDTO {
    private Long solicitudId;
    private List<Solicitud.DdJjOrigenFondosTipo> ddJjOrigenFondos;
    private Long comprobanteDdJjOrigenFondosId; // ID del archivo del comprobante
}
