package ar.com.st.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad para manejo de localidades
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Entity
@Getter
@Setter
@Table(name = "LOCALIDAD")
public class Localidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NOMBRE")
    private String nombre;

    @Column(name = "CODIGO_CVSA")
    private String codigoCVSA;

    @Column(name = "CODIGO_UIF")
    private String codigoUIF;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_PROVINCIA", nullable = false)
    @JsonBackReference
    private Provincia provincia;
}

