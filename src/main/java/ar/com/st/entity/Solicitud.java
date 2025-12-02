package ar.com.st.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entidad principal para manejo de solicitudes de apertura de cuenta
 * 
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Entity
@Getter
@Setter
@Table(name = "AC_SOLICITUD")
public class Solicitud implements Serializable {

    public enum Tipo {
        EMPRESA_SGR,
        EMPRESA_CLASICA,
        INDIVIDUO
    }

    public enum Estado {
        PENDIENTE,
        PENTIENTE_FIRMA,
        FIRMADO,
        APROBADA,
        RECHAZADA,
        INCOMPLETA,
        ERROR_DETECTADO,
        CANCELADA,
        ERROR_PDF
    }

    public enum DdJjOrigenFondosTipo {
        TRANSFERENCIA_BANCARIA("Transferencia Bancaria a nombre del titular"),
        TRANSFERENCIA_ESPECIES("Transferencia de Especies"),
        CHEQUE("Entrega de Cheques a nombre del titular.");

        private final String descripcion;

        private DdJjOrigenFondosTipo(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO_CARGO")
    @JsonIgnore
    private Usuario usuarioCargo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO_APROBO")
    @JsonIgnore
    private Usuario usuarioAprobo;

    // Campos para serialización JSON
    @Transient
    private Long idUsuarioCargo;

    @Transient
    private Long idUsuarioAprobo;

    @Column(name = "FECHA_APROBO", nullable = true)
    private LocalDateTime fechaAprobo;

    @Column(name = "FECHA_ALTA", nullable = true)
    private LocalDateTime fechaAlta;

    @Column(name = "FECHA_ULTIMA_MODIFICACION", nullable = true)
    private LocalDateTime fechaUltimaModificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = true)
    private Estado estado;

    @Column(name = "ID_CUENTA", nullable = true)
    private Integer idCuenta;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = true)
    private Tipo tipo;

    @Column(name = "ACEPTA_TERMINOS_CONDICIONES")
    private Boolean aceptaTerminosCondiciones;

    @Column(name = "ACEPTA_REGLAMENTO_GESTION_FONDOS")
    private Boolean aceptaReglamentoGestionFondos;

    @Column(name = "ACEPTA_COMISIONES")
    private Boolean aceptaComisiones;

    @Column(name = "ID_FIRMA_DIGITAL")
    private String idFirmaDigital;

    @Column(name = "MOTIVO_APROBADO_SIN_FIRMA")
    private String motivoAprobadoSinFirma;

    @Column(name = "COMO_NOS_CONOCIO")
    private String comoNosConocio;

    @ManyToOne(optional = true, fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "ID_PRODUCTOR")
    @JsonIgnore
    private Persona productor;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "ID_TITULAR")
    @JsonIgnore
    private Organizacion titular;

    @Column(name = "TIENE_FIRMANTES")
    private Boolean tieneFirmantes;

    // Campos adicionales para serialización JSON
    @Transient
    private Long idProductor;

    @Transient
    private Long idTitular;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "AC_SOLICITUD_FIRMANTE", joinColumns = @JoinColumn(name = "ID_AC_SOLICITUD"), inverseJoinColumns = @JoinColumn(name = "ID_AC_PERSONA"))
    @OrderColumn(name = "ORDEN")
    @JsonIgnore
    private List<Persona> firmantes = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(name = "AC_SOLICITUD_CUENTA_BANCARIA", joinColumns = @JoinColumn(name = "ID_AC_SOLICITUD"), inverseJoinColumns = @JoinColumn(name = "ID_AC_CUENTA_BANCARIA"))
    @OrderColumn(name = "ORDEN")
    @JsonIgnore
    private List<CuentaBancaria> cuentasBancarias = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "AC_DDJJ_FONDOS_ORIGEN", joinColumns = @JoinColumn(name = "ID_AC_SOLICITUD"))
    @Column(name = "ORIGEN")
    @OrderColumn(name = "ORDEN")
    @Enumerated(EnumType.STRING)
    private List<DdJjOrigenFondosTipo> ddJjOrigenFondos = new ArrayList<>();

    @ManyToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ARCHIVO")
    @JsonIgnore
    private Archivo comprobanteDdJjOrigenFondos;

    // Campo adicional para serialización JSON
    @Transient
    private Long idArchivo;

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.id);
        hash = 79 * hash + Objects.hashCode(this.estado);
        hash = 79 * hash + Objects.hashCode(this.tipo);
        hash = 79 * hash + Objects.hashCode(this.comoNosConocio);
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
        final Solicitud other = (Solicitud) obj;
        if (!Objects.equals(this.comoNosConocio, other.comoNosConocio)) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (this.estado != other.estado) {
            return false;
        }
        return this.tipo == other.tipo;
    }

    @PreUpdate
    public void onUpdate() {
        this.fechaUltimaModificacion = LocalDateTime.now();
    }

    @PrePersist
    public void onCreate() {
        this.fechaUltimaModificacion = LocalDateTime.now();
    }

    @PostLoad
    public void onLoad() {
        // Poblar campos transientes para serialización JSON
        this.idUsuarioCargo = usuarioCargo != null ? usuarioCargo.getId() : null;
        this.idUsuarioAprobo = usuarioAprobo != null ? usuarioAprobo.getId() : null;
        
        // Manejar productor con validación de existencia
        try {
            this.idProductor = productor != null ? productor.getId() : null;
        } catch (jakarta.persistence.EntityNotFoundException e) {
            // El productor fue eliminado - establecer como null
            this.idProductor = null;
        }
        
        this.idTitular = titular != null ? titular.getId() : null;
        this.idArchivo = comprobanteDdJjOrigenFondos != null ? comprobanteDdJjOrigenFondos.getId() : null;
    }

    /**
     * Obtiene el nombre del titular basado en su tipo (empresa o persona)
     * @return nombre del titular
     */
    public String getNombre() {
        if (titular != null) {
            Class<?> titularClass = Hibernate.getClass(titular);
            if (titularClass.equals(ar.com.st.entity.Empresa.class)) {
                ar.com.st.entity.Empresa empresa = (ar.com.st.entity.Empresa) titular;
                return empresa.getDenominacion();
            } else if (titularClass.equals(ar.com.st.entity.Persona.class)) {
                ar.com.st.entity.Persona persona = (ar.com.st.entity.Persona) titular;
                String nombres = persona.getNombres() != null ? persona.getNombres() : "";
                String apellidos = persona.getApellidos() != null ? persona.getApellidos() : "";
                return (nombres + " " + apellidos).trim();
            }
        }
        return null;
    }

    // Getters personalizados que retornan false si el valor es null
    public boolean isAceptaTerminosCondiciones() {
        return aceptaTerminosCondiciones != null ? aceptaTerminosCondiciones : false;
    }

    public boolean isAceptaReglamentoGestionFondos() {
        return aceptaReglamentoGestionFondos != null ? aceptaReglamentoGestionFondos : false;
    }

    public boolean isAceptaComisiones() {
        return aceptaComisiones != null ? aceptaComisiones : false;
    }

    public boolean isTieneFirmantes() {
        return tieneFirmantes != null ? tieneFirmantes : false;
    }
}
