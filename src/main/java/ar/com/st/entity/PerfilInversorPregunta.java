package ar.com.st.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Entity
@Table(name = "AC_PERFIL_INVERSOR_PREGUNTA")
public class PerfilInversorPregunta implements Serializable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;
    
    @Column(name = "NOMBRE_CORTO", nullable = true)
    private String nombreCorto;

    @Column(name = "PREGUNTA", nullable = true)
    private String pregunta;
    
    @Column(name = "HABILITADA", nullable = true)
    private boolean habilitada;

    @OneToMany(mappedBy = "perfilInversorPregunta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PerfilInversorPreguntaOpcion> opciones;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isHabilitada() {
        return habilitada;
    }

    public void setHabilitada(boolean habilitada) {
        this.habilitada = habilitada;
    }
    
    public String getPregunta() {
        return pregunta;
    }

    public void setPregunta(String pregunta) {
        this.pregunta = pregunta;
    }

    public List<PerfilInversorPreguntaOpcion> getOpciones() {
        return opciones;
    }

    public void setOpciones(List<PerfilInversorPreguntaOpcion> opciones) {
        this.opciones = opciones;
    }

    public String getNombreCorto() {
        return nombreCorto;
    }

    public void setNombreCorto(String nombreCorto) {
        this.nombreCorto = nombreCorto;
    }

    @Override
    public String toString() {
        return "PerfilInversorPregunta{" + "id=" + id + ", nombreCorto=" + nombreCorto + ", pregunta=" + pregunta + ", habilitada=" + habilitada + ", opciones=" + opciones + '}';
    }
}
