package ar.com.st.dto.solicitud;

import ar.com.st.entity.Solicitud;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO de resumen de Solicitud para listados.
 */
@Data
public class SolicitudResumenDTO {

    private Long id;
    private Long idUsuarioCargo;
    /**
     * Email del usuario que cargó la solicitud (usuario que inició sesión para darse de alta)
     */
    private String emailUsuarioCargo;
    private Long idUsuarioAprobo;
    private LocalDateTime fechaAprobo;
    private LocalDateTime fechaAlta;
    private LocalDateTime fechaUltimaModificacion;
    private Solicitud.Estado estado;
    private Integer idCuenta;
    private Solicitud.Tipo tipo;
    private String idFirmaDigital;
    private Long idProductor;
    /**
     * Nombre completo del productor (nombres + apellidos)
     */
    private String productor;
    /**
     * Nombre completo del titular (persona) o denominación (empresa)
     */
    private String titular;
}


