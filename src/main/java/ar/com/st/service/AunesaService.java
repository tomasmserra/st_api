package ar.com.st.service;

import ar.com.st.dto.aunesa.AltaCuentaResultado;
import ar.com.st.entity.Localidad;
import ar.com.st.entity.Provincia;
import ar.com.st.entity.Solicitud;

import java.util.List;

/**
 * Servicio para comunicación con AUNESA
 * @author Tomás Serra <tomas@serra.com.ar>
 */
public interface AunesaService {
    
    /**
     * Da de alta una cuenta comitente en AUNESA para la solicitud indicada
     *
     * @param solicitud solicitud a dar de alta
     * @return resultado de la operación
     */
    AltaCuentaResultado altaCuentaComitente(Solicitud solicitud);
    
    /**
     * Obtiene todas las provincias de AUNESA
     *
     * @return lista de provincias
     */
    List<Provincia> obtenerProvincias();
    
    /**
     * Obtiene todas las localidades de AUNESA
     *
     * @return lista de localidades
     */
    List<Localidad> obtenerLocalidades();
    
    /**
     * Obtiene las localidades de una provincia específica de AUNESA
     *
     * @param codigoProvincia código de la provincia
     * @return lista de localidades de la provincia
     */
    List<Localidad> obtenerLocalidades(String codigoProvincia);
}

