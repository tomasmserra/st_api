package ar.com.st.service;

/**
 * Servicio para manejo de provincias y localidades
 * @author Tomás Serra <tomas@serra.com.ar>
 */
public interface AmbitoService {
    
    /**
     * Sincroniza las provincias y localidades desde AUNESA
     * Obtiene todas las provincias de AUNESA, luego para cada una obtiene sus localidades
     * y actualiza la base de datos local
     */
    void actualizarProvincias();
}

