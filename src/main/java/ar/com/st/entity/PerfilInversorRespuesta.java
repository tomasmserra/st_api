package ar.com.st.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonBackReference;
import java.io.Serializable;

/**
 *
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Entity
@Table(name = "AC_PERFIL_INVERSOR_RESPUESTA")
public class PerfilInversorRespuesta implements Serializable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ID_PREGUNTA")
    private PerfilInversorPregunta perfilInversorPregunta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ID_OPCION")
    private PerfilInversorPreguntaOpcion perfilInversorPreguntaOpcion;

    @ManyToOne(optional = true)
    @JoinColumn(name = "ID_AC_PERFIL_INVERSOR", nullable = false)
    @JsonBackReference
    private PerfilInversor perfilInversor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PerfilInversor getPerfilInversor() {
        return perfilInversor;
    }

    public void setPerfilInversor(PerfilInversor perfilInversor) {
        this.perfilInversor = perfilInversor;
    }
    
    public PerfilInversorPregunta getPerfilInversorPregunta() {
        return perfilInversorPregunta;
    }

    public void setPerfilInversorPregunta(PerfilInversorPregunta perfilInversorPregunta) {
        this.perfilInversorPregunta = perfilInversorPregunta;
    }

    public PerfilInversorPreguntaOpcion getPerfilInversorPreguntaOpcion() {
        return perfilInversorPreguntaOpcion;
    }

    public void setPerfilInversorPreguntaOpcion(PerfilInversorPreguntaOpcion perfilInversorPreguntaOpcion) {
        this.perfilInversorPreguntaOpcion = perfilInversorPreguntaOpcion;
    }

    @Override
    public String toString() {
        return "PerfilInversorRespuesta{" + "id=" + id + ", perfilInversorPregunta=" + perfilInversorPregunta + ", perfilInversorPreguntaOpcion=" + perfilInversorPreguntaOpcion + '}';
    }
}
