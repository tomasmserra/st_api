package ar.com.st.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entidad para manejo de domicilios
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Entity
@Getter
@Setter
@Table(name = "AC_DOMICILIO")
public class Domicilio implements Serializable {

    public enum Tipo {
        LEGAL("Domicilio legal", "Legal"),
        REAL("Domicilio real","Real"),
        CORRESPONDENCIA("Domicilio correspondencia","Correspondencia");

        private final String descripcion;
        private final String valor;

        private Tipo(String descripcion, String valor) {
            this.descripcion = descripcion;
            this.valor = valor;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public String getValor() {
            return valor;
        }
    }

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false)
    private Tipo tipo;

    @Column(name = "CALLE")
    private String calle;

    @Column(name = "NUMERO")
    private String numero;

    @Column(name = "PISO")
    private String piso;

    @Column(name = "DEPTO")
    private String depto;

    @Column(name = "BARRIO")
    private String barrio;

    @Column(name = "CIUDAD")
    private String ciudad;

    @Column(name = "PROVINCIA")
    private String provincia;

    @Column(name = "PAIS")
    private String pais;

    @Column(name = "CP")
    private String cp;

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.id);
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
        final Domicilio other = (Domicilio) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Domicilio{" + "id=" + id + ", tipo=" + tipo + ", calle=" + calle + ", numero=" + numero + ", piso=" + piso + ", depto=" + depto + ", barrio=" + barrio + ", ciudad=" + ciudad + ", provincia=" + provincia + ", pais=" + pais + ", cp=" + cp + '}';
    }
}
