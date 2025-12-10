package ar.com.st.controller;

import ar.com.st.dto.comun.DatosFiscalesDTO;
import ar.com.st.dto.comun.DomicilioDTO;
import ar.com.st.dto.individuo.FirmanteDetalleDTO;
import ar.com.st.dto.individuo.IndividuoDeclaracionesDTO;
import ar.com.st.dto.individuo.IndividuoDatosPersonalesDTO;
import ar.com.st.dto.individuo.IndividuoDatosPrincipalesDTO;
import ar.com.st.entity.Conyuge;
import ar.com.st.entity.DatosFiscales;
import ar.com.st.entity.Domicilio;
import ar.com.st.entity.PerfilInversor;
import ar.com.st.entity.Persona;
import ar.com.st.entity.Solicitud;
import ar.com.st.entity.Usuario;
import ar.com.st.repository.ArchivoRepository;
import ar.com.st.service.AuthService;
import ar.com.st.service.SolicitudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controlador para pasos posteriores de apertura de cuenta (firmantes, accionistas, etc.)
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@RestController
@RequestMapping("/api/apertura-posterior")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Apertura de Cuenta - Pasos Posteriores", description = "API para pasos posteriores como firmantes y accionistas")
public class AperturaPosteriorController {

    private final SolicitudService solicitudService;
    private final ArchivoRepository archivoRepository;
    private final AuthService authService;

    /**
     * Valida que si el usuario autenticado tiene rol CLIENTE, 
     * solo pueda acceder a solicitudes donde él es el usuarioCargo
     */
    private ResponseEntity<?> validarAccesoCliente(Long solicitudId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(403).build();
            }
            
            Usuario usuario = authService.obtenerUsuarioPorUsername(authentication.getName())
                    .orElse(null);
            
            if (usuario == null) {
                return ResponseEntity.status(403).build();
            }
            
            // Verificar si el usuario tiene rol CLIENTE
            boolean esCliente = usuario.getRoles().stream()
                    .anyMatch(rol -> rol.getNombre() == ar.com.st.entity.Rol.NombreRol.CLIENTE);
            
            if (esCliente) {
                // Si es CLIENTE, verificar que es el usuarioCargo de la solicitud
                Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
                if (solicitudOpt.isEmpty()) {
                    return ResponseEntity.notFound().build();
                }
                
                Solicitud solicitud = solicitudOpt.get();
                if (solicitud.getUsuarioCargo() == null || 
                    !Objects.equals(solicitud.getUsuarioCargo().getId(), usuario.getId())) {
                    log.warn("Intento de acceso no autorizado: Usuario {} intentó acceder a solicitud {} de otro usuario", 
                            usuario.getId(), solicitudId);
                    return ResponseEntity.status(403).build();
                }
            }
            
            return null; // Acceso permitido
        } catch (Exception e) {
            log.error("Error validando acceso de cliente: {}", e.getMessage(), e);
            return ResponseEntity.status(403).build();
        }
    }

    // ========== FIRMANTES (SOLO PARA INDIVIDUO) ==========
    
    @PostMapping("/firmantes/{solicitudId}")
    @Operation(summary = "Agregar Firmante", description = "Agrega un firmante a la solicitud (solo para individuos)")
    public ResponseEntity<Void> agregarFirmante(
            @PathVariable Long solicitudId,
            @Parameter(description = "Datos completos del firmante") @Valid @RequestBody FirmanteDetalleDTO dto) {
        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Void>) validacion;
        }
        return guardarFirmante(solicitudId, null, dto);
    }

    @GetMapping("/firmantes/{solicitudId}")
    @Operation(summary = "Obtener Firmantes", description = "Obtiene todos los firmantes de una solicitud")
    public ResponseEntity<List<FirmanteDetalleDTO>> obtenerFirmantes(@PathVariable Long solicitudId) {
        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<List<FirmanteDetalleDTO>>) validacion;
        }
        
        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Solicitud solicitud = solicitudOpt.get();
        List<Persona> firmantes = solicitud.getFirmantes();
        if (firmantes == null || firmantes.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        
        List<FirmanteDetalleDTO> firmantesDTO = firmantes.stream()
                .map(this::convertirPersonaADTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(firmantesDTO);
    }

    @GetMapping("/firmantes/{solicitudId}/{dniNumero}")
    @Operation(summary = "Obtener Firmantes por DNI", description = "Obtiene todos los firmantes de una solicitud por DNI")
    public ResponseEntity<List<FirmanteDetalleDTO>> obtenerFirmantesPorDni(@PathVariable Long solicitudId, @PathVariable String dniNumero) {
        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<List<FirmanteDetalleDTO>>) validacion;
        }
        
        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Solicitud solicitud = solicitudOpt.get();
        List<Persona> firmantes = solicitud.getFirmantes();
        if (firmantes == null || firmantes.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        
        List<FirmanteDetalleDTO> firmantesDTO = firmantes.stream()
                .filter(f -> Objects.equals(f.getIdNumero(), dniNumero))
                .map(this::convertirPersonaADTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(firmantesDTO);
    }

    @PutMapping("/firmantes/{solicitudId}/{firmanteId}")
    @Operation(summary = "Actualizar Firmante", description = "Actualiza los datos de un firmante")
    public ResponseEntity<Void> actualizarFirmante(
            @PathVariable Long solicitudId,
            @PathVariable Long firmanteId,
            @Parameter(description = "Datos completos del firmante") @Valid @RequestBody FirmanteDetalleDTO dto) {

        if (dto.getId() != null && !Objects.equals(dto.getId(), firmanteId)) {
            return ResponseEntity.badRequest().build();
        }
        
        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Void>) validacion;
        }
        
        return guardarFirmante(solicitudId, firmanteId, dto);
    }

    @DeleteMapping("/firmantes/{solicitudId}/{firmanteId}")
    @Operation(summary = "Eliminar Firmante", description = "Elimina un firmante de la solicitud")
    public ResponseEntity<Void> eliminarFirmante(
            @PathVariable Long solicitudId,
            @PathVariable Long firmanteId) {
        
        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Void>) validacion;
        }
        
        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO) {
            return ResponseEntity.badRequest().build();
        }

        List<Persona> firmantes = solicitud.getFirmantes();
        if (firmantes != null) {
            firmantes.removeIf(f -> Objects.equals(f.getId(), firmanteId));
        }

        solicitud.setTieneFirmantes(firmantes != null && !firmantes.isEmpty());
        
        solicitudService.actualizar(solicitud);
        return ResponseEntity.ok().build();
    }

    // ========== FINALIZACIÓN ==========
    
    @PostMapping("/finalizar/{solicitudId}")
    @Operation(summary = "Finalizar Solicitud", description = "Finaliza el proceso de apertura de cuenta")
    public ResponseEntity<Void> finalizarSolicitud(@PathVariable Long solicitudId) {
        
        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Void>) validacion;
        }
        
        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        solicitudService.finalizarCarga(solicitudId);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<Void> guardarFirmante(Long solicitudId, Long firmanteId, FirmanteDetalleDTO dto) {
        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();

        // Validar campos requeridos para firmantes
        if (dto.getDatosPrincipales() == null || dto.getDatosPersonales() == null || 
            dto.getDeclaraciones() == null || dto.getDatosFiscales() == null || dto.getDomicilio() == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Persona> firmantes = solicitud.getFirmantes();
        if (firmantes == null) {
            firmantes = new ArrayList<>();
            solicitud.setFirmantes(firmantes);
        }

        Persona firmante = null;
        if (firmanteId != null) {
            firmante = firmantes.stream()
                    .filter(f -> Objects.equals(f.getId(), firmanteId))
                    .findFirst()
                    .orElse(null);
            if (firmante == null) {
                return ResponseEntity.notFound().build();
            }
        } else if (dto.getId() != null) {
            firmante = firmantes.stream()
                    .filter(f -> Objects.equals(f.getId(), dto.getId()))
                    .findFirst()
                    .orElse(null);
        }

        if (firmante == null) {
            firmante = crearPersonaBase();
            firmante.setTipo(solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO ? Persona.Tipo.CO_TITULAR : Persona.Tipo.FIRMANTE);
            firmantes.add(firmante);
        }

        aplicarDatosPersona(firmante, dto);

        solicitud.setTieneFirmantes(!firmantes.isEmpty());
        solicitudService.actualizar(solicitud);
        return ResponseEntity.ok().build();
    }

    private Persona crearPersonaBase() {
        Persona persona = new Persona();
        
        if (persona.getDomicilio() == null) {
            persona.setDomicilio(new Domicilio());
        }
        if (persona.getDatosFiscales() == null) {
            persona.setDatosFiscales(new DatosFiscales());
        }
        if (persona.getDatosFiscalesExterior() == null) {
            persona.setDatosFiscalesExterior(new DatosFiscales());
        }
        if (persona.getPerfilInversor() == null) {
            PerfilInversor perfilInversor = new PerfilInversor();
            perfilInversor.setRespuestas(new ArrayList<>());
            persona.setPerfilInversor(perfilInversor);
        } else if (persona.getPerfilInversor().getRespuestas() == null) {
            persona.getPerfilInversor().setRespuestas(new ArrayList<>());
        }
        if (persona.getConyuge() == null) {
            persona.setConyuge(new Conyuge());
        }
        return persona;
    }

    private void aplicarDatosPersona(Persona persona, FirmanteDetalleDTO dto) {
        actualizarDatosPrincipales(persona, dto.getDatosPrincipales());
        actualizarDatosPersonales(persona, dto.getDatosPersonales());
        actualizarDeclaraciones(persona, dto.getDeclaraciones());
        actualizarDatosFiscales(persona, dto.getDatosFiscales());
        actualizarDomicilio(persona, dto.getDomicilio());
    }

    private void actualizarDatosPrincipales(Persona persona, IndividuoDatosPrincipalesDTO dto) {
        if (dto == null) {
            return;
        }
        persona.setNombres(dto.getNombres());
        persona.setApellidos(dto.getApellidos());
        persona.setCelular(dto.getCelular());
        persona.setCorreoElectronico(dto.getCorreoElectronico());
    }

    private void actualizarDatosPersonales(Persona persona, IndividuoDatosPersonalesDTO dto) {
        if (dto == null) {
            return;
        }
        persona.setTipoID(dto.getTipoID());
        persona.setIdNumero(dto.getIdNumero());
        persona.setFechaNacimiento(dto.getFechaNacimiento());
        persona.setLugarNacimiento(dto.getLugarNacimiento());
        persona.setNacionalidad(dto.getNacionalidad());
        persona.setPaisOrigen(dto.getPaisOrigen());
        persona.setPaisResidencia(dto.getPaisResidencia());
        persona.setActividad(dto.getActividad());
        persona.setSexo(dto.getSexo());
        persona.setEstadoCivil(dto.getEstadoCivil());

        if (dto.getDniFrenteArchivoId() != null) {
            archivoRepository.findById(dto.getDniFrenteArchivoId())
                    .ifPresent(persona::setDniFrente);
        }

        if (dto.getDniReversoArchivoId() != null) {
            archivoRepository.findById(dto.getDniReversoArchivoId())
                    .ifPresent(persona::setDniReverso);
        }

        if (dto.getConyuge() != null) {
            if (persona.getConyuge() == null) {
                persona.setConyuge(new Conyuge());
            }
            Conyuge conyuge = persona.getConyuge();
            conyuge.setNombres(dto.getConyuge().getNombres());
            conyuge.setApellidos(dto.getConyuge().getApellidos());
            conyuge.setTipoID(dto.getConyuge().getTipoID());
            conyuge.setIdNumero(dto.getConyuge().getIdNumero());
            conyuge.setTipoClaveFiscal(dto.getConyuge().getTipoClaveFiscal());
            conyuge.setClaveFiscal(dto.getConyuge().getClaveFiscal());
        }
    }

    private void actualizarDeclaraciones(Persona persona, IndividuoDeclaracionesDTO dto) {
        if (dto == null) {
            return;
        }
        persona.setEsPep(dto.isEsPep());
        persona.setMotivoPep(dto.getMotivoPep());
        persona.setEsFATCA(dto.isEsFATCA());
        persona.setMotivoFatca(dto.getMotivoFatca());
        persona.setDeclaraUIF(dto.isDeclaraUIF());
        persona.setMotivoUIF(dto.getMotivoUIF());
    }

    private void actualizarDatosFiscales(Persona persona, DatosFiscalesDTO dto) {
        if (dto == null) {
            return;
        }
        if (persona.getDatosFiscales() == null) {
            persona.setDatosFiscales(new DatosFiscales());
        }
        DatosFiscales datosFiscales = persona.getDatosFiscales();
        datosFiscales.setTipo(dto.getTipo());
        datosFiscales.setClaveFiscal(dto.getClaveFiscal());
        datosFiscales.setTipoIva(dto.getTipoIva());
        datosFiscales.setTipoGanancia(dto.getTipoGanancia());
        datosFiscales.setResidenciaFiscal(dto.getResidenciaFiscal());

        if (dto.isDebeCompletarFiscalExterior()) {
            if (persona.getDatosFiscalesExterior() == null) {
                persona.setDatosFiscalesExterior(new DatosFiscales());
            }
            DatosFiscales exterior = persona.getDatosFiscalesExterior();
            exterior.setTipo(dto.getTipo());
            exterior.setClaveFiscal(dto.getClaveFiscal());
            exterior.setTipoIva(dto.getTipoIva());
            exterior.setTipoGanancia(dto.getTipoGanancia());
            exterior.setResidenciaFiscal(dto.getResidenciaFiscal());
        } else if (persona.getDatosFiscalesExterior() == null) {
            persona.setDatosFiscalesExterior(new DatosFiscales());
        }
    }

    private void actualizarDomicilio(Persona persona, DomicilioDTO dto) {
        if (dto == null) {
            return;
        }
        if (persona.getDomicilio() == null) {
            persona.setDomicilio(new Domicilio());
        }
        Domicilio domicilio = persona.getDomicilio();
        if (dto.getTipo() != null) {
            domicilio.setTipo(dto.getTipo());
        }
        domicilio.setCalle(dto.getCalle());
        domicilio.setNumero(dto.getNumero());
        domicilio.setPiso(dto.getPiso());
        domicilio.setDepto(dto.getDepto());
        domicilio.setBarrio(dto.getBarrio());
        domicilio.setCiudad(dto.getCiudad());
        domicilio.setProvincia(dto.getProvincia());
        domicilio.setPais(dto.getPais());
        domicilio.setCp(dto.getCp());
    }

    // ========== MÉTODOS DE CONVERSIÓN A DTO ==========

    private FirmanteDetalleDTO convertirPersonaADTO(Persona persona) {
        if (persona == null) {
            return null;
        }

        FirmanteDetalleDTO dto = new FirmanteDetalleDTO();
        dto.setId(persona.getId());

        // Datos principales
        IndividuoDatosPrincipalesDTO datosPrincipales = new IndividuoDatosPrincipalesDTO();
        datosPrincipales.setNombres(persona.getNombres());
        datosPrincipales.setApellidos(persona.getApellidos());
        datosPrincipales.setCelular(persona.getCelular());
        datosPrincipales.setCorreoElectronico(persona.getCorreoElectronico());
        dto.setDatosPrincipales(datosPrincipales);

        // Datos personales
        IndividuoDatosPersonalesDTO datosPersonales = new IndividuoDatosPersonalesDTO();
        datosPersonales.setTipoID(persona.getTipoID());
        datosPersonales.setIdNumero(persona.getIdNumero());
        datosPersonales.setFechaNacimiento(persona.getFechaNacimiento());
        datosPersonales.setLugarNacimiento(persona.getLugarNacimiento());
        datosPersonales.setNacionalidad(persona.getNacionalidad());
        datosPersonales.setPaisOrigen(persona.getPaisOrigen());
        datosPersonales.setPaisResidencia(persona.getPaisResidencia());
        datosPersonales.setActividad(persona.getActividad());
        datosPersonales.setSexo(persona.getSexo());
        datosPersonales.setEstadoCivil(persona.getEstadoCivil());
        if (persona.getDniFrente() != null) {
            datosPersonales.setDniFrenteArchivoId(persona.getDniFrente().getId());
        }
        if (persona.getDniReverso() != null) {
            datosPersonales.setDniReversoArchivoId(persona.getDniReverso().getId());
        }
        if (persona.getConyuge() != null) {
            ar.com.st.dto.individuo.ConyugeDTO conyugeDTO = new ar.com.st.dto.individuo.ConyugeDTO();
            conyugeDTO.setNombres(persona.getConyuge().getNombres());
            conyugeDTO.setApellidos(persona.getConyuge().getApellidos());
            conyugeDTO.setTipoID(persona.getConyuge().getTipoID());
            conyugeDTO.setIdNumero(persona.getConyuge().getIdNumero());
            conyugeDTO.setTipoClaveFiscal(persona.getConyuge().getTipoClaveFiscal());
            conyugeDTO.setClaveFiscal(persona.getConyuge().getClaveFiscal());
            datosPersonales.setConyuge(conyugeDTO);
        }
        dto.setDatosPersonales(datosPersonales);

        // Declaraciones
        if (persona.getEsPep() != null || persona.getEsFATCA() != null || persona.getDeclaraUIF() != null) {
            IndividuoDeclaracionesDTO declaraciones = new IndividuoDeclaracionesDTO();
            declaraciones.setEsPep(persona.getEsPep() != null ? persona.getEsPep() : false);
            declaraciones.setMotivoPep(persona.getMotivoPep());
            declaraciones.setEsFATCA(persona.getEsFATCA() != null ? persona.getEsFATCA() : false);
            declaraciones.setMotivoFatca(persona.getMotivoFatca());
            declaraciones.setDeclaraUIF(persona.getDeclaraUIF() != null ? persona.getDeclaraUIF() : false);
            declaraciones.setMotivoUIF(persona.getMotivoUIF());
            dto.setDeclaraciones(declaraciones);
        }

        // Datos fiscales
        if (persona.getDatosFiscales() != null) {
            DatosFiscalesDTO datosFiscales = new DatosFiscalesDTO();
            datosFiscales.setTipo(persona.getDatosFiscales().getTipo());
            datosFiscales.setClaveFiscal(persona.getDatosFiscales().getClaveFiscal());
            datosFiscales.setTipoIva(persona.getDatosFiscales().getTipoIva());
            datosFiscales.setTipoGanancia(persona.getDatosFiscales().getTipoGanancia());
            datosFiscales.setResidenciaFiscal(persona.getDatosFiscales().getResidenciaFiscal());
            dto.setDatosFiscales(datosFiscales);
        }

        // Domicilio
        if (persona.getDomicilio() != null) {
            DomicilioDTO domicilio = new DomicilioDTO();
            domicilio.setTipo(persona.getDomicilio().getTipo());
            domicilio.setCalle(persona.getDomicilio().getCalle());
            domicilio.setNumero(persona.getDomicilio().getNumero());
            domicilio.setPiso(persona.getDomicilio().getPiso());
            domicilio.setDepto(persona.getDomicilio().getDepto());
            domicilio.setBarrio(persona.getDomicilio().getBarrio());
            domicilio.setCiudad(persona.getDomicilio().getCiudad());
            domicilio.setProvincia(persona.getDomicilio().getProvincia());
            domicilio.setPais(persona.getDomicilio().getPais());
            domicilio.setCp(persona.getDomicilio().getCp());
            dto.setDomicilio(domicilio);
        }

        return dto;
    }

}
