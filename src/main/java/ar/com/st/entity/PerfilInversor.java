package ar.com.st.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entidad para manejo del perfil de inversor
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Entity
@Getter
@Setter
@Table(name = "AC_PERFIL_INVERSOR")
public class PerfilInversor implements Serializable {

    public enum Tipo {
        AGRESIVO(55, 90,"Agresivo"),
        MODERADO(37, 55,"Moderado"),
        CONSERVADOR(1, 37,"Conservador");

        private Integer limiteInferior;
        private Integer limiteSuperior;
        private String descripcion;

        private Tipo(Integer limiteInferior, Integer limiteSuperior, String descripcion) {
            this.limiteInferior = limiteInferior;
            this.limiteSuperior = limiteSuperior;
            this.descripcion = descripcion;
        }

        public Integer getLimiteInferior() {
            return limiteInferior;
        }

        public Integer getLimiteSuperior() {
            return limiteSuperior;
        }

        public String getDescripcion() {
            return descripcion;
        }
        
        public boolean verficarEnIntervaloe(long valor) {
            return limiteInferior < valor && limiteSuperior > valor;
        }
    }

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = true)
    private Tipo tipo;

    @OneToMany(mappedBy = "perfilInversor", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<PerfilInversorRespuesta> respuestas;

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PerfilInversor other = (PerfilInversor) obj;
        return Objects.equals(this.id, other.id);
    }
}
