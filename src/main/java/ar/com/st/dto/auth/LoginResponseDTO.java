package ar.com.st.dto.auth;

import lombok.Data;

import java.util.List;

/**
 * DTO para response de login
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Data
public class LoginResponseDTO {
    
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String nombres;
    private String apellidos;
    private List<String> roles;
    private Long expirationTime;
    
    public LoginResponseDTO(String token, Long id, String username, String email, 
                           String nombres, String apellidos, List<String> roles, Long expirationTime) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.roles = roles;
        this.expirationTime = expirationTime;
    }
}
