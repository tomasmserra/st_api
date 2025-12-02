package ar.com.st.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entidad para manejo de cuentas bancarias
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Entity
@Getter
@Setter
@Table(name = "AC_CUENTA_BANCARIA")
public class CuentaBancaria implements Serializable {

    public enum TipoMoneda {
        PESOS("ARS"),
        DOLARES("USD"),
        BIMONETARIA("BIMONETARIA");

        private final String descripcion;

        private TipoMoneda(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public enum TipoCliente {
        PERSONAL("Personal"),
        BUSINESS("Business");

        private final String descripcion;

        private TipoCliente(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public enum Tipo {
        CUENTA_CORRIENTE("Cuenta corriente", true),
        CAJA_AHORRO("Caja de ahorro", true),
        CUENTA_JUDICIAL("Cuenta judicial", true),
        OTRO("Otro", true),
        CHECKING("Checking", false),
        SAVINGS("Savings", false);

        private final String descripcion;
        private final boolean esNacional;

        private Tipo(String descripcion, boolean esNacional) {
            this.descripcion = descripcion;
            this.esNacional = esNacional;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public boolean isEsNacional() {
            return esNacional;
        }
    }

    public enum TipoClaveBancaria {
        CBU,
        CVU
    }

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false)
    private Tipo tipo;

    @Column(name = "BANCO")
    private String banco;

    @Enumerated(EnumType.STRING)
    @Column(name = "MONEDA", nullable = false)
    private TipoMoneda moneda;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_CLAVE_BANCARIA", nullable = false)
    private TipoClaveBancaria tipoClaveBancaria;

    @Column(name = "CLAVE_BANCARIA")
    private String claveBancaria;

    @Column(name = "PAIS", nullable = true)
    private String pais;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_CLIENTE", nullable = true)
    private TipoCliente tipoCliente;

    @Column(name = "NUMERO_ABA")
    private String numeroAba;

    @Column(name = "IDENTIFICACION_SWIFT")
    private String identificacionSwift;

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.id);
        hash = 43 * hash + Objects.hashCode(this.tipo);
        hash = 43 * hash + Objects.hashCode(this.banco);
        hash = 43 * hash + Objects.hashCode(this.moneda);
        hash = 43 * hash + Objects.hashCode(this.tipoClaveBancaria);
        hash = 43 * hash + Objects.hashCode(this.claveBancaria);
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
        final CuentaBancaria other = (CuentaBancaria) obj;
        if (!Objects.equals(this.banco, other.banco)) {
            return false;
        }
        if (!Objects.equals(this.claveBancaria, other.claveBancaria)) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (this.tipo != other.tipo) {
            return false;
        }
        if (this.moneda != other.moneda) {
            return false;
        }
        return this.tipoClaveBancaria == other.tipoClaveBancaria;
    }
}
