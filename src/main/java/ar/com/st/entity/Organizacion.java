package ar.com.st.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Proxy;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Entidad base para organizaciones (Persona o Empresa)
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Entity
@Getter
@Setter
@ToString
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "ENTITY_TYPE")
@Table(name = "AC_ORGANIZACION")
public class Organizacion implements Serializable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "ID_AC_DOMICILIO")
    private Domicilio domicilio;

    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "ID_AC_PERFIL_INVERSOR")
    private PerfilInversor perfilInversor;

    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "ID_AC_DATOS_FISCALES")
    private DatosFiscales datosFiscales;

    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "ID_AC_DATOS_FISCALES_EXTERIOR")
    private DatosFiscales datosFiscalesExterior;
}
