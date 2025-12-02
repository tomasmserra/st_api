package ar.com.st.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad Usuario para autenticación
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Entity
@Table(name = "USUARIO")
@Data
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
public class Usuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "USERNAME", unique = true, nullable = false, length = 50)
    private String username;
    
    @Column(name = "EMAIL", unique = true, nullable = false, length = 100)
    private String email;
    
    @Column(name = "PASSWORD", nullable = false)
    private String password;
    
    @Column(name = "NOMBRES", length = 100)
    private String nombres;
    
    @Column(name = "APELLIDOS", length = 100)
    private String apellidos;
    
    @Column(name = "ACTIVO", nullable = false)
    private Boolean activo = true;
    
    @Column(name = "FECHA_CREACION", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    
    @Column(name = "ULTIMO_ACCESO")
    private LocalDateTime ultimoAcceso;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "USUARIO_ROL",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Rol> roles = new HashSet<>();
    
    public Usuario() {}
    
    public Usuario(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
    
    public void agregarRol(Rol rol) {
        this.roles.add(rol);
    }
    
    public void removerRol(Rol rol) {
        this.roles.remove(rol);
    }
}
