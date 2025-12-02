package ar.com.st.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad Rol para autorización
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Entity
@Table(name = "ROL")
@Data
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
public class Rol {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "NOMBRE", unique = true, nullable = false, length = 50)
    private NombreRol nombre;
    
    @Column(name = "DESCRIPCION", length = 200)
    private String descripcion;
    
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<Usuario> usuarios = new HashSet<>();
    
    public Rol() {}
    
    public Rol(NombreRol nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }
    
    /**
     * Enum con los roles del sistema
     */
    public enum NombreRol {
        ADMIN("Administrador"),
        CLIENTE("Cliente"),
        OPERADOR("Operador"),
        SUPERVISOR("Supervisor");
        
        private final String descripcion;
        
        NombreRol(String descripcion) {
            this.descripcion = descripcion;
        }
        
        public String getDescripcion() {
            return descripcion;
        }
    }
}
