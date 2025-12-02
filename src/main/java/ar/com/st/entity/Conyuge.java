package ar.com.st.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entidad para manejo de datos del cónyuge
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Entity
@Getter
@Setter
@Table(name = "AC_CONYUGE")
public class Conyuge implements Serializable {
    
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(name = "NOMBRES", nullable = true)
    private String nombres;

    @Column(name = "APELLIDOS", nullable = true)
    private String apellidos;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_ID", nullable = true)
    private Persona.TipoID tipoID;

    @Column(name = "ID_NUMERO", nullable = true)
    private String idNumero;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_CLAVE_FISCAL", nullable = true)
    private DatosFiscales.Tipo tipoClaveFiscal;

    @Column(name = "CLAVE_FISCAL", nullable = true)
    private String claveFiscal;

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.id);
        hash = 59 * hash + Objects.hashCode(this.idNumero);
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
        final Conyuge other = (Conyuge) obj;
        if (!Objects.equals(this.idNumero, other.idNumero)) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return "Conyuge{" + "nombres=" + nombres + ", apellidos=" + apellidos + ", tipoID=" + tipoID + ", idNumero=" + idNumero + ", tipoClaveFiscal=" + tipoClaveFiscal + ", claveFiscal=" + claveFiscal + '}';
    }
}
