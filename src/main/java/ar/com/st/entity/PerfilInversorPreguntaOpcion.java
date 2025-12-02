package ar.com.st.entity;

import ar.com.st.entity.PerfilInversor.Tipo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 *
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Entity
@Table(name = "AC_PERFIL_INVERSOR_OPCION")
public class PerfilInversorPreguntaOpcion implements Serializable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(name = "VALOR")
    private String valor;

    @Column(name = "PUNTAJE")
    private Integer puntaje;

    @Column(name = "DETERMINANTE")
    private boolean determinante;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_PERFIL", nullable = true)
    private Tipo tipoPerfil;

    @ManyToOne(optional = true)
    @JoinColumn(name = "ID_AC_PERFIL_INVERSOR_PREGUNTA", nullable = false)
    private PerfilInversorPregunta perfilInversorPregunta;

    public PerfilInversorPreguntaOpcion() {
    }

    public PerfilInversorPreguntaOpcion(String valor) {
        this.valor = valor;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public Integer getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(Integer puntaje) {
        this.puntaje = puntaje;
    }

    public PerfilInversorPregunta getPerfilInversorPregunta() {
        return perfilInversorPregunta;
    }

    public void setPerfilInversorPregunta(PerfilInversorPregunta perfilInversorPregunta) {
        this.perfilInversorPregunta = perfilInversorPregunta;
    }

    public boolean isDeterminante() {
        return determinante;
    }

    public void setDeterminante(boolean determinante) {
        this.determinante = determinante;
    }

    public Tipo getTipoPerfil() {
        return tipoPerfil;
    }

    public void setTipoPerfil(Tipo tipoPerfil) {
        this.tipoPerfil = tipoPerfil;
    }

    @Override
    public String toString() {
        return "PerfilInversorPreguntaOpcion{" + "id=" + id + ", valor=" + valor + ", puntaje=" + puntaje + ", determinante=" + determinante + ", tipoPerfil=" + tipoPerfil + ", perfilInversorPregunta=" + perfilInversorPregunta + '}';
    }

}
