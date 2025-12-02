package ar.com.st.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.util.List;

/**
 * Entidad para manejo de empresas
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Entity
@Getter
@Setter
@Table(name = "AC_EMPRESA")
@DiscriminatorValue("Empresa")
public class Empresa extends Organizacion {

    public enum Tipo {
        TITULAR("Titular"),
        ACCIONISTA("Accionista");

        private String descripcion;

        private Tipo(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public enum TipoEmpresa {
        AGRUPACION("Agrupación"),
        ASOCIACION("Asociación"),
        COOPERATIVA("Coperativa"),
        SOCIEDAD_ANONIMA("Sociedad anónima"),
        SOCIEDAD_SIN_FINES_LUCR("Sociedad sin fines de lucro"),
        SRL("SRL"),
        SAS("SAS"),
        FIDEICOMISO("Fideicomiso"),
        OTRA("Otra");

        private String descripcion;

        private TipoEmpresa(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public enum UsoFirma {
        INDISTINTA("Indistinta"),
        CONJUNTA("Conjunta");

        private final String descripcion;

        private UsoFirma(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public enum TipoID {
        CUIT,
        CDI
    }

    public enum LugarInscripcionRegistro {
        CNV("CNV"),
        DJP("DJP"),
        ETR("ETR"),
        EXT("EXT"),
        IAC("IAC"),
        IAM("IAM"),
        INAES("INAES"),
        IGJ("IGJ"),
        NOI("NOI"),
        RIP("RIP"),
        RPC("RPC"),
        SAC("SAC"),
        SEC("SEC"),
        Otro("Otro");

        private final String descripcion;

        private LugarInscripcionRegistro(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = true)
    private Tipo tipo;

    @Column(name = "DENOMINACION", nullable = true)
    private String denominacion;

    @Column(name = "ACTIVIDAD", nullable = true)
    private String actividad;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_EMPRESA", nullable = false)
    private TipoEmpresa tipoEmpresa;

    @Column(name = "FECHA_CONSTITUCION", nullable = true)
    private LocalDate fechaConstitucion;

    @Column(name = "NUMERO_ACTA", nullable = true)
    private String numeroActa;

    @Column(name = "PAIS_ORIGEN", nullable = true)
    private String paisOrigen;

    @Column(name = "PAIS_RESIDENCIA", nullable = true)
    private String paisResidencia;

    @Column(name = "CIERRE_BALANCE", nullable = true)
    private LocalDate fechaCierreBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "USO_FIRMA", nullable = true)
    private UsoFirma usoFirma;

    @Enumerated(EnumType.STRING)
    @Column(name = "LUGAR_INSCRIPCION_REGISTRO", nullable = true)
    private LugarInscripcionRegistro lugarInscripcionRegistro;

    @Column(name = "NUMERO_REGISTRO", nullable = true)
    private String numeroRegistro;

    @Column(name = "PAIS_REGISTRO", nullable = true)
    private String paisRegistro;

    @Column(name = "PROVINCIA_REGISTRO", nullable = true)
    private String provinciaRegistro;

    @Column(name = "LUGAR_REGISTRO", nullable = true)
    private String lugarRegistro;

    @Column(name = "FECHA_REGISTRO", nullable = true)
    private LocalDate fechaRegistro;

    @Column(name = "FOLIO", nullable = true)
    private String folio;

    @Column(name = "LIBRO", nullable = true)
    private String libro;

    @Column(name = "TOMO", nullable = true)
    private String tomo;

    @Column(name = "TELEFONO", nullable = true)
    private String telefono;

    @Column(name = "CELULAR", nullable = true)
    private String celular;

    @Column(name = "CORREO_ELECTRONICO", nullable = true)
    private String correoElectronico;

    @Column(name = "DECLARA_UIF")
    private Boolean declaraUIF;

    @Column(name = "MOTIVO_UIF")
    private String motivoUIF;

    @Column(name = "DECLARA_FATCA")
    private Boolean esFATCA;

    @Column(name = "MOTIVO_FATCA")
    private String motivoFatca;

    @Column(name = "PORCENTAJE")
    private Double porcentaje;

    @ManyToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ESTATUTO")
    private Archivo estatuto;

    @ManyToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_BALANCE")
    private Archivo balance;

    @ManyToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ACCIONISTAS")
    private Archivo accionista;

    @ManyToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PODER_ACTA")
    private Archivo poderActa;

    @ManyToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CONSTANCIA_CUIT")
    private Archivo constanciaCuit;

    @ManyToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_DDJJ_GANANCIAS")
    private Archivo ddjjGanancias;

    @ManyToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PODER")
    private Archivo poder;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "AC_EMPRESA_ACCIONISTA",
            joinColumns = @JoinColumn(name = "ID_AC_EMPRESA"),
            inverseJoinColumns = @JoinColumn(name = "ID_AC_ORGANIZACION"))
    @OrderColumn(name = "ORDEN")
    @JsonIgnore
    private List<Organizacion> accionistas;
}
