package ar.com.st.service.impl;

import ar.com.st.entity.Localidad;
import ar.com.st.entity.Provincia;
import ar.com.st.repository.ProvinciaRepository;
import ar.com.st.service.AmbitoService;
import ar.com.st.service.AunesaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación del servicio para manejo de provincias y localidades
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AmbitoServiceImpl implements AmbitoService {

    private final AunesaService aunesaService;
    private final ProvinciaRepository provinciaRepository;

    @Override
    @Transactional
    public void actualizarProvincias() {
        log.info("Iniciando sincronización de provincias y localidades desde AUNESA");
        
        try {
            // Obtener todas las provincias de AUNESA
            List<Provincia> provinciasAunesa = aunesaService.obtenerProvincias();
            
            if (provinciasAunesa == null || provinciasAunesa.isEmpty()) {
                log.warn("No se obtuvieron provincias de AUNESA");
                return;
            }
            
            log.info("Se obtuvieron {} provincias de AUNESA", provinciasAunesa.size());
            
            // Procesar cada provincia
            for (Provincia provinciaAunesa : provinciasAunesa) {
                if (provinciaAunesa.getCodigo() == null || provinciaAunesa.getCodigo().isEmpty()) {
                    log.warn("Provincia sin código encontrada: {}", provinciaAunesa.getNombre());
                    continue;
                }
                
                // Buscar si la provincia ya existe en la base de datos local
                Provincia provinciaLocal = provinciaRepository.findByCodigo(provinciaAunesa.getCodigo())
                        .orElse(null);
                
                // Si no existe, crear nueva. Si existe, actualizar campos
                if (provinciaLocal == null) {
                    provinciaLocal = new Provincia();
                    provinciaLocal.setCodigo(provinciaAunesa.getCodigo());
                    log.debug("Nueva provincia encontrada: {}", provinciaAunesa.getNombre());
                } else {
                    log.debug("Actualizando provincia existente: {}", provinciaAunesa.getNombre());
                }
                
                // Actualizar campos de la provincia
                provinciaLocal.setNombre(provinciaAunesa.getNombre());
                provinciaLocal.setCodigoCVSA(provinciaAunesa.getCodigoCVSA());
                provinciaLocal.setCodigoUIF(provinciaAunesa.getCodigoUIF());
                
                // Obtener localidades de esta provincia desde AUNESA
                List<Localidad> localidadesAunesa = aunesaService.obtenerLocalidades(provinciaAunesa.getCodigo());
                
                if (localidadesAunesa != null && !localidadesAunesa.isEmpty()) {
                    log.debug("Se obtuvieron {} localidades para provincia {}", 
                            localidadesAunesa.size(), provinciaAunesa.getNombre());
                    
                    // Limpiar localidades existentes (se reemplazarán con las de AUNESA)
                    if (provinciaLocal.getId() != null && provinciaLocal.getLocalidades() != null) {
                        provinciaLocal.getLocalidades().clear();
                    }
                    
                    // Procesar cada localidad
                    for (Localidad localidadAunesa : localidadesAunesa) {
                        Localidad localidadLocal = new Localidad();
                        localidadLocal.setNombre(localidadAunesa.getNombre());
                        localidadLocal.setCodigoCVSA(localidadAunesa.getCodigoCVSA());
                        localidadLocal.setCodigoUIF(localidadAunesa.getCodigoUIF());
                        localidadLocal.setProvincia(provinciaLocal);
                        
                        // Agregar la localidad a la provincia
                        if (provinciaLocal.getLocalidades() == null) {
                            provinciaLocal.setLocalidades(new java.util.ArrayList<>());
                        }
                        provinciaLocal.getLocalidades().add(localidadLocal);
                    }
                } else {
                    log.warn("No se obtuvieron localidades para provincia {}", 
                            provinciaAunesa.getNombre());
                }
                
                // Guardar la provincia (y sus localidades por cascade)
                provinciaRepository.save(provinciaLocal);
                log.debug("Provincia {} guardada con {} localidades", 
                        provinciaLocal.getNombre(), 
                        provinciaLocal.getLocalidades() != null ? provinciaLocal.getLocalidades().size() : 0);
            }
            
            log.info("Sincronización de provincias y localidades completada exitosamente");
            
        } catch (Exception ex) {
            log.error("Error al sincronizar provincias y localidades desde AUNESA: {}", ex.getMessage(), ex);
            throw new RuntimeException("Error al sincronizar provincias y localidades: " + ex.getMessage(), ex);
        }
    }
}

