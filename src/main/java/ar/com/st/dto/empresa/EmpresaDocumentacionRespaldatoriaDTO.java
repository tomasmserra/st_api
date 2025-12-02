package ar.com.st.dto.empresa;

import lombok.Data;

/**
 * DTO para documentación respaldatoria de empresa
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class EmpresaDocumentacionRespaldatoriaDTO {
    private Long solicitudId;
    private Long estatutoId; // ID del archivo del estatuto
    private Long balanceId; // ID del archivo del balance
    private Long accionistaId; // ID del archivo de accionistas
    private Long poderActaId; // ID del archivo del poder/acta
    private Long poderId; // ID del archivo del poder
    private Long ddjjGananciasId; // ID del archivo de DDJJ de ganancias
}
