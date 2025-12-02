package ar.com.st.controller;

import ar.com.st.entity.Archivo;
import ar.com.st.repository.ArchivoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Controlador REST para manejo de archivos
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@RestController
@RequestMapping("/api/archivos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Archivos", description = "API para manejo de archivos")
public class ArchivoController {

    private final ArchivoRepository archivoRepository;

    @GetMapping
    @Operation(summary = "Obtener todos los archivos", description = "Obtiene todos los archivos registrados")
    public ResponseEntity<List<Archivo>> obtenerTodas() {
        log.info("Obteniendo todos los archivos");
        List<Archivo> archivos = archivoRepository.findAll();
        return ResponseEntity.ok(archivos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un archivo por ID", description = "Obtiene un archivo específico por su ID")
    public ResponseEntity<Archivo> obtenerPorId(@PathVariable Long id) {
        log.info("Obteniendo archivo con ID: {}", id);
        return archivoRepository.findById(id)
                .map(archivo -> ResponseEntity.ok(archivo))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Descargar un archivo", description = "Descarga un archivo por su ID")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        log.info("Descargando archivo con ID: {}", id);
        return archivoRepository.findById(id)
                .map(archivo -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                    headers.setContentDispositionFormData("attachment", archivo.getNombre());
                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(archivo.getArchivoData());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/upload")
    @Operation(summary = "Subir un archivo", description = "Sube un nuevo archivo al sistema")
    public ResponseEntity<Archivo> subir(@RequestParam("file") MultipartFile file) {
        log.info("Subiendo archivo: {}", file.getOriginalFilename());
        
        try {
            Archivo archivo = new Archivo();
            archivo.setNombre(file.getOriginalFilename());
            archivo.setArchivoData(file.getBytes());
            
            Archivo archivoGuardado = archivoRepository.save(archivo);
            return ResponseEntity.status(201).body(archivoGuardado);
        } catch (IOException e) {
            log.error("Error al subir archivo", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un archivo", description = "Elimina un archivo del sistema")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("Eliminando archivo con ID: {}", id);
        if (!archivoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        archivoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
