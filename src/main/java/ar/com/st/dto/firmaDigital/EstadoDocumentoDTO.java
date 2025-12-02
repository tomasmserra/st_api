package ar.com.st.dto.firmaDigital;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para representar el estado de un documento de firma digital
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class EstadoDocumentoDTO {
    private String id;
    private EstadoFirmaDTO.Estado estado;
    private LocalDateTime fecha;
    private String motivoCancelacion;
    private String titulo;
    private String url;
    private List<EstadoFirmaDTO> firmas;
}

