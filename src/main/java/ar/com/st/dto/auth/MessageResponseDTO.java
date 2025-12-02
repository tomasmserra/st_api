package ar.com.st.dto.auth;

import lombok.Data;

/**
 * DTO para response de mensajes
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class MessageResponseDTO {
    
    private String message;
    
    public MessageResponseDTO(String message) {
        this.message = message;
    }
}
