package ar.com.st.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidad para manejo de personas físicas
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Entity
@Getter
@Setter
@ToString
@Table(name = "AC_PERSONA")
@DiscriminatorValue("Persona")
public class Persona extends Organizacion {

    public enum Tipo {
        TITULAR("Titular"),
        CO_TITULAR("Co-Titular"),
        ACCIONISTA("Accionista"),
        FIRMANTE("Firmante"),
        PRODUCTOR("Productor");

        private String descripcion;

        private Tipo(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public enum TipoID {
        DNI,
        LC,
        LE,
        EXT,
        PAS
    }

    public enum Sexo {
        MASCULINO("Masculino"),
        FEMENINO("Femenino"),
        NO_BINARIO("No Binario");

        private final String descripcion;

        private Sexo(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public enum EstadoCivil {
        CASADO("Casado"),
        SOLTERO("Soltero"),
        DIVORCIADO("Divorciado"),
        SEPARADO("Separado"),
        VIUDO("Viudo"),
        CONCUBINATO("Unión Convivencial");

        private final String valor;

        private EstadoCivil(String valor) {
            this.valor = valor;
        }

        public String getValor() {
            return valor;
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = true)
    private Tipo tipo;

    @Column(name = "NOMBRES", nullable = true, length = 128)
    private String nombres;

    @Column(name = "APELLIDOS", nullable = true, length = 128)
    private String apellidos;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_ID", nullable = true)
    private TipoID tipoID;

    @Column(name = "ID_NUMERO", nullable = true)
    private String idNumero;

    @Column(name = "FECHA_NACIMIENTO", nullable = true)
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "SEXO", nullable = true)
    private Sexo sexo;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO_CIVIL", nullable = true)
    private EstadoCivil estadoCivil;

    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "ID_AC_CONYUGE")
    private Conyuge conyuge;

    @Column(name = "NACIONALIDAD", nullable = true)
    private String nacionalidad;

    @Column(name = "PAIS_RESIDENCIA", nullable = true)
    private String paisResidencia;

    @Column(name = "PAIS_ORIGEN", nullable = true)
    private String paisOrigen;

    @Column(name = "LUGAR_NACIMIENTO", nullable = true, length = 128)
    private String lugarNacimiento;

    @Column(name = "ACTIVIDAD", nullable = true, length = 128)
    private String actividad;

    @Column(name = "TELEFONO", nullable = true)
    private String telefono;

    @Column(name = "CELULAR", nullable = true)
    private String celular;

    @Column(name = "CORREO_ELECTRONICO", nullable = true, length = 128)
    private String correoElectronico;

    @ManyToOne(optional = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ARCHIVO_DNI_FRENTE")
    private Archivo dniFrente;

    @ManyToOne(optional = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ARCHIVO_DNI_REVERSO")
    private Archivo dniReverso;

    @Column(name = "ES_PEP")
    private Boolean esPep;

    @Column(name = "MOTIVO_PEP", length = 128)
    private String motivoPep;

    @Column(name = "ES_FATCA")
    private Boolean esFATCA;

    @Column(name = "MOTIVO_FATCA", length = 128)
    private String motivoFatca;

    @Column(name = "DECLARA_UIF")
    private Boolean declaraUIF;

    @Column(name = "MOTIVO_UIF", length = 128)
    private String motivoUIF;

    @Column(name = "PORCENTAJE")
    private Double porcentaje;

    /**
     * Trunca los campos de texto a 128 caracteres antes de persistir o actualizar
     * para evitar errores de "value too long for type character varying(128)"
     */
    @PrePersist
    @PreUpdate
    private void truncarCampos() {
        if (nombres != null && nombres.length() > 128) {
            nombres = nombres.substring(0, 128);
        }
        if (apellidos != null && apellidos.length() > 128) {
            apellidos = apellidos.substring(0, 128);
        }
        if (lugarNacimiento != null && lugarNacimiento.length() > 128) {
            lugarNacimiento = lugarNacimiento.substring(0, 128);
        }
        if (actividad != null && actividad.length() > 128) {
            actividad = actividad.substring(0, 128);
        }
        if (correoElectronico != null && correoElectronico.length() > 128) {
            correoElectronico = correoElectronico.substring(0, 128);
        }
        if (motivoPep != null && motivoPep.length() > 128) {
            motivoPep = motivoPep.substring(0, 128);
        }
        if (motivoFatca != null && motivoFatca.length() > 128) {
            motivoFatca = motivoFatca.substring(0, 128);
        }
        if (motivoUIF != null && motivoUIF.length() > 128) {
            motivoUIF = motivoUIF.substring(0, 128);
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.tipo);
        hash = 89 * hash + Objects.hashCode(this.nombres);
        hash = 89 * hash + Objects.hashCode(this.apellidos);
        hash = 89 * hash + Objects.hashCode(this.tipoID);
        hash = 89 * hash + Objects.hashCode(this.idNumero);
        hash = 89 * hash + Objects.hashCode(this.fechaNacimiento);
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
        final Persona other = (Persona) obj;
        if (!Objects.equals(this.nombres, other.nombres)) {
            return false;
        }
        if (!Objects.equals(this.apellidos, other.apellidos)) {
            return false;
        }
        if (!Objects.equals(this.idNumero, other.idNumero)) {
            return false;
        }
        if (this.tipo != other.tipo) {
            return false;
        }
        if (this.tipoID != other.tipoID) {
            return false;
        }
        return Objects.equals(this.fechaNacimiento, other.fechaNacimiento);
    }
}
