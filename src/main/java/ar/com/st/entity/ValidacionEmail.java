package ar.com.st.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad para validación de email con código de verificación
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Entity
@Table(name = "SEC_VALIDACION_EMAIL")
@Data
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
public class ValidacionEmail implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "FECHA", nullable = false)
    private LocalDateTime fecha;
    
    @Column(name = "EMAIL", nullable = false, length = 100)
    private String email;
    
    @Column(name = "CLAVE", length = 50, nullable = false)
    private String clave = "";
    
    @Column(name = "VALIDATION_CODE", nullable = false, length = 10)
    private String validationCode;
    
    @Column(name = "ACTIVO", nullable = false)
    private boolean activo = true;
    
    @Column(name = "FECHA_EXPIRACION")
    private LocalDateTime fechaExpiracion;
    
    public ValidacionEmail() {
        this.fecha = LocalDateTime.now();
        this.activo = true;
        this.clave = "";
        // El código expira en 15 minutos
        this.fechaExpiracion = LocalDateTime.now().plusMinutes(15);
    }
    
    public ValidacionEmail(String email, String validationCode) {
        this();
        this.email = email;
        this.validationCode = validationCode;
    }
    
    /**
     * Verifica si el código ha expirado
     */
    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(this.fechaExpiracion);
    }
    
    
    /**
     * Desactiva la validación
     */
    public void desactivar() {
        this.activo = false;
    }
}
