package ar.com.st.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entidad para manejo de datos fiscales
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Entity
@Getter
@Setter
@Table(name = "AC_DATOS_FISCALES")
public class DatosFiscales implements Serializable {

    public enum Tipo {
        CUIL(true),
        CUIT(true),
        CDI(true),
        NIF(false),
        NIE(false),
        CIF(false), 
        RUT(false),
        RUN(false),
        NIT(false),
        SAT(false),
        RFC(false),
        NSS(false),
        SSN(false),
        TIN(false),
        TaxID(false),
        CPF(false),
        DUI(false),
        RTU(false),
        Otro(false);
        
        private boolean esNacional;
        
        private Tipo(boolean esNacional) {
            this.esNacional = esNacional;
        }

        public boolean isEsNacional() {
            return esNacional;
        }
    }

    public enum TipoIva {
        RESPONSABLE_INSCRIPTO("Responsable inscripto"),
        RESPONSABLE_MONOTRIBUTO("Responsable monotributo"),
        CONSUMIDOR_FINAL("Consumidor final"),
        EXENTO("Exento"),
        NO_CATEGORIZADO("No categorizado"),
        NO_ALCANZADO("No alcanzado");

        private final String descripcion;

        private TipoIva(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public enum TipoGanancia {
        INSCRIPTO("Inscripto"),
        NO_INSCRIPTO("No inscripto"),
        EXENTO("Exento"),
        RESPONSABLE_MONOTRIBUTO("Responsable monotributo");

        private final String descripcion;

        private TipoGanancia(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_CLAVE_FISCAL", nullable = true)
    private Tipo tipo;

    @Column(name = "CLAVE_FISCAL", nullable = true)
    private String claveFiscal;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_IVA", nullable = true)
    private TipoIva tipoIva;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_GANANCIA", nullable = true)
    private TipoGanancia tipoGanancia;

    @Column(name = "RESIDENCIA_FISCAL", nullable = true)
    private String residenciaFiscal;

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.id);
        hash = 97 * hash + Objects.hashCode(this.claveFiscal);
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
        final DatosFiscales other = (DatosFiscales) obj;
        if (!Objects.equals(this.claveFiscal, other.claveFiscal)) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }
}
