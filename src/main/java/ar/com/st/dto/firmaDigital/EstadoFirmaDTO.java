package ar.com.st.dto.firmaDigital;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO para representar el estado de una firma individual
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class EstadoFirmaDTO {
    private String id;
    private String url;
    private LocalDateTime fecha;
    private Estado estado;
    private String email;

    public enum Estado {
        INCOMPLETO,
        CANCELADO,
        PENDIENTE,
        COMPLETO
    }
}

