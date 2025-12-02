package ar.com.st.controller;

import ar.com.st.dto.productor.ProductorDTO;
import ar.com.st.entity.Organizacion;
import ar.com.st.entity.Persona;
import ar.com.st.entity.Solicitud;
import ar.com.st.repository.OrganizacionRepository;
import ar.com.st.repository.PersonaRepository;
import ar.com.st.service.SolicitudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controlador REST para administración de productores
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@RestController
@RequestMapping("/api/productores")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Productores", description = "API para administración de productores")
public class ProductorController {

    private final PersonaRepository personaRepository;
    private final OrganizacionRepository organizacionRepository;
    private final SolicitudService solicitudService;
    private final EntityManager entityManager;

    @GetMapping
    @Operation(summary = "Obtener todos los productores", 
               description = "Obtiene la lista de todos los productores registrados")
    public ResponseEntity<List<ProductorDTO>> obtenerTodos() {
        List<Persona> productores = personaRepository.findByTipo(Persona.Tipo.PRODUCTOR);
        List<ProductorDTO> dtos = productores.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener productor por ID", 
               description = "Obtiene un productor específico por su ID")
    public ResponseEntity<ProductorDTO> obtenerPorId(
            @Parameter(description = "ID del productor") @PathVariable Long id) {
        Optional<Persona> productorOpt = personaRepository.findById(id);
        if (productorOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Persona productor = productorOpt.get();
        if (productor.getTipo() != Persona.Tipo.PRODUCTOR) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(convertirADTO(productor));
    }

    @PostMapping("/asignar")
    @Operation(summary = "Asignar productor a solicitud", 
               description = "Asigna un productor a una solicitud específica")
    public ResponseEntity<Void> asignarProductor(
            @Parameter(description = "ID de la solicitud") @RequestParam Long idSolicitud,
            @Parameter(description = "ID del productor") @RequestParam Long idProductor) {
        // Validar que la solicitud existe
        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(idSolicitud);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Validar que el productor existe y es de tipo PRODUCTOR
        Optional<Organizacion> organizacionOpt = organizacionRepository.findById(idProductor);
        
        if (organizacionOpt.isEmpty()) {
            // Si no se encuentra con JPA, intentar buscar directamente por query JPQL
            Query queryDirecta = entityManager.createQuery(
                "SELECT p FROM Persona p WHERE p.id = :id", Persona.class);
            queryDirecta.setParameter("id", idProductor);
            try {
                Persona personaDirecta = (Persona) queryDirecta.getSingleResult();
                organizacionOpt = Optional.of(personaDirecta);
            } catch (Exception e) {
                return ResponseEntity.notFound().build();
            }
        }
        
        Organizacion organizacion = organizacionOpt.get();
        
        if (!(organizacion instanceof Persona)) {
            return ResponseEntity.badRequest().build();
        }
        
        Persona productor = (Persona) organizacion;
        
        if (productor.getTipo() == null || productor.getTipo() != Persona.Tipo.PRODUCTOR) {
            return ResponseEntity.badRequest().build();
        }
        
        // Asignar el productor a la solicitud
        Solicitud solicitud = solicitudOpt.get();
        solicitud.setProductor(productor);
        solicitudService.actualizar(solicitud);
        
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @Operation(summary = "Crear productor", 
               description = "Crea un nuevo productor")
    public ResponseEntity<ProductorDTO> crear(
            @Parameter(description = "Datos del productor") @Valid @RequestBody ProductorDTO dto) {
        Persona productor = new Persona();
        productor.setTipo(Persona.Tipo.PRODUCTOR);
        aplicarDatos(productor, dto);
        
        // Crear entidades relacionadas requeridas
        if (productor.getDomicilio() == null) {
            productor.setDomicilio(new ar.com.st.entity.Domicilio());
        }
        if (productor.getPerfilInversor() == null) {
            productor.setPerfilInversor(new ar.com.st.entity.PerfilInversor());
        }
        if (productor.getDatosFiscales() == null) {
            productor.setDatosFiscales(new ar.com.st.entity.DatosFiscales());
        }
        if (productor.getDatosFiscalesExterior() == null) {
            productor.setDatosFiscalesExterior(new ar.com.st.entity.DatosFiscales());
        }
        if (productor.getConyuge() == null) {
            productor.setConyuge(new ar.com.st.entity.Conyuge());
        }
        
        Persona guardado = personaRepository.save(productor);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertirADTO(guardado));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar productor", 
               description = "Actualiza los datos de un productor existente")
    public ResponseEntity<ProductorDTO> actualizar(
            @Parameter(description = "ID del productor") @PathVariable Long id,
            @Parameter(description = "Datos actualizados del productor") @Valid @RequestBody ProductorDTO dto) {
        Optional<Persona> productorOpt = personaRepository.findById(id);
        if (productorOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Persona productor = productorOpt.get();
        if (productor.getTipo() != Persona.Tipo.PRODUCTOR) {
            return ResponseEntity.notFound().build();
        }
        
        aplicarDatos(productor, dto);
        Persona actualizado = personaRepository.save(productor);
        
        return ResponseEntity.ok(convertirADTO(actualizado));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar productor", 
               description = "Elimina un productor del sistema")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del productor") @PathVariable Long id) {
        Optional<Persona> productorOpt = personaRepository.findById(id);
        if (productorOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Persona productor = productorOpt.get();
        if (productor.getTipo() != Persona.Tipo.PRODUCTOR) {
            return ResponseEntity.notFound().build();
        }
        
        personaRepository.delete(productor);
        return ResponseEntity.noContent().build();
    }

    /**
     * Convierte una entidad Persona a ProductorDTO
     */
    private ProductorDTO convertirADTO(Persona productor) {
        ProductorDTO dto = new ProductorDTO();
        dto.setId(productor.getId());
        dto.setNombres(productor.getNombres());
        dto.setApellidos(productor.getApellidos());
        dto.setCorreoElectronico(productor.getCorreoElectronico());
        dto.setCelular(productor.getCelular());
        return dto;
    }

    /**
     * Aplica los datos del DTO a la entidad Persona
     */
    private void aplicarDatos(Persona productor, ProductorDTO dto) {
        productor.setNombres(dto.getNombres());
        productor.setApellidos(dto.getApellidos());
        productor.setCorreoElectronico(dto.getCorreoElectronico());
        productor.setCelular(dto.getCelular());
    }
}

