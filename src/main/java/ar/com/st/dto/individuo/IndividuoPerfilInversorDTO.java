package ar.com.st.dto.individuo;

import lombok.Data;

import java.util.List;

/**
 * DTO para perfil de inversor de individuo
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class IndividuoPerfilInversorDTO {
    private List<PerfilInversorPreguntaDTO> preguntas; // Lista de preguntas con sus opciones
    private List<PerfilInversorRespuestaDTO> respuestas; // Respuestas del cliente (preguntaId -> opcionId)
    private String tipoPerfil; // Resultado de la calificación
}
