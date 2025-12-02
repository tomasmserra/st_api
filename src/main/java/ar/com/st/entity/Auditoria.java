package ar.com.st.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad para registrar acciones de auditoría en el sistema
 *
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Entity
@Getter
@Setter
@Table(name = "AC_AUDITORIA")
public class Auditoria implements Serializable {

    public enum TipoAccion {
        APROBACION_CUENTA,
        RECHAZO_CUENTA,
        CANCELACION_SOLICITUD,
        MODIFICACION_SOLICITUD,
        CREACION_SOLICITUD,
        ENVIO_CORREO,
        OTRO
    }

    public enum Resultado {
        EXITOSO,
        FALLIDO
    }

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_ACCION", nullable = false)
    private TipoAccion tipoAccion;

    @Column(name = "FECHA", nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO")
    @JsonIgnore
    private Usuario usuario;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SOLICITUD")
    @JsonIgnore
    private Solicitud solicitud;

    @Enumerated(EnumType.STRING)
    @Column(name = "RESULTADO", nullable = false)
    private Resultado resultado;

    @Column(name = "MENSAJE", length = 2000)
    private String mensaje;

    @Column(name = "DETALLES", length = 5000)
    private String detalles;

    @Column(name = "ID_CUENTA")
    private Integer idCuenta;

    /**
     * Obtiene el ID del usuario sin cargar la entidad completa (para serialización JSON)
     */
    @JsonProperty("idUsuario")
    public Long getIdUsuario() {
        if (usuario != null) {
            try {
                // Si es un proxy no inicializado, intentar obtener el ID sin inicializarlo
                if (usuario instanceof HibernateProxy) {
                    HibernateProxy proxy = (HibernateProxy) usuario;
                    return (Long) proxy.getHibernateLazyInitializer().getIdentifier();
                }
                return usuario.getId();
            } catch (Exception e) {
                // Si hay algún error, retornar null
                return null;
            }
        }
        return null;
    }

    /**
     * Obtiene el ID de la solicitud sin cargar la entidad completa (para serialización JSON)
     */
    @JsonProperty("idSolicitud")
    public Long getIdSolicitud() {
        if (solicitud != null) {
            try {
                // Si es un proxy no inicializado, intentar obtener el ID sin inicializarlo
                if (solicitud instanceof HibernateProxy) {
                    HibernateProxy proxy = (HibernateProxy) solicitud;
                    return (Long) proxy.getHibernateLazyInitializer().getIdentifier();
                }
                return solicitud.getId();
            } catch (Exception e) {
                // Si hay algún error, retornar null
                return null;
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.id);
        hash = 79 * hash + Objects.hashCode(this.tipoAccion);
        hash = 79 * hash + Objects.hashCode(this.fecha);
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
        final Auditoria other = (Auditoria) obj;
        if (this.tipoAccion != other.tipoAccion) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return Objects.equals(this.fecha, other.fecha);
    }
}

