package ar.com.st.controller;

import ar.com.st.entity.Localidad;
import ar.com.st.entity.Provincia;
import ar.com.st.repository.LocalidadRepository;
import ar.com.st.repository.ProvinciaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para manejo de provincias y localidades
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@RestController
@RequestMapping("/api/ambito")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ámbito", description = "API para obtener provincias y localidades")
public class AmbitoController {

    private final ProvinciaRepository provinciaRepository;
    private final LocalidadRepository localidadRepository;

    @GetMapping("/provincias")
    @Operation(summary = "Obtener todas las provincias", description = "Obtiene todas las provincias ordenadas por nombre")
    public ResponseEntity<List<Provincia>> obtenerProvincias() {
        log.info("Obteniendo todas las provincias");
        List<Provincia> provincias = provinciaRepository.findAllByOrderByNombreAsc();
        return ResponseEntity.ok(provincias);
    }

    @GetMapping("/localidades/{provinciaId}")
    @Operation(summary = "Obtener localidades por provincia", description = "Obtiene todas las localidades de una provincia ordenadas por nombre")
    public ResponseEntity<List<Localidad>> obtenerLocalidadesPorProvincia(
            @Parameter(description = "ID de la provincia") @PathVariable Long provinciaId) {
        log.info("Obteniendo localidades para la provincia con ID: {}", provinciaId);
        List<Localidad> localidades = localidadRepository.findByProvinciaIdOrderByNombreAsc(provinciaId);
        return ResponseEntity.ok(localidades);
    }
}

