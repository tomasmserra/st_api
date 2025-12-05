package ar.com.st.controller;

import ar.com.st.dto.comun.*;
import ar.com.st.dto.individuo.*;
import ar.com.st.entity.PerfilInversorRespuesta;
import ar.com.st.entity.Persona;
import ar.com.st.entity.Solicitud;
import ar.com.st.entity.CuentaBancaria;
import ar.com.st.repository.ArchivoRepository;
import ar.com.st.repository.PerfilInversorPreguntaRepository;
import ar.com.st.entity.Conyuge;
import ar.com.st.entity.DatosFiscales;
import ar.com.st.entity.Domicilio;
import ar.com.st.entity.Usuario;
import ar.com.st.service.AuthService;
import ar.com.st.service.SolicitudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Controlador para apertura de cuenta de individuo
 * 
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@RestController
@RequestMapping("/api/individuo-apertura")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Apertura de Cuenta Individuo", description = "API para apertura de cuenta de individuo paso a paso")
public class IndividuoAperturaController {

    private final SolicitudService solicitudService;
    private final ArchivoRepository archivoRepository;
    private final PerfilInversorPreguntaRepository perfilInversorPreguntaRepository;
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

    // ========== DATOS PRINCIPALES ==========

    @GetMapping("/datos-principales/{solicitudId}")
    @Operation(summary = "Obtener Datos Principales", description = "Obtiene los datos principales de una solicitud de individuo")
    public ResponseEntity<IndividuoDatosPrincipalesDTO> obtenerDatosPrincipales(@PathVariable Long solicitudId) {
        log.info("Obteniendo datos principales de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<IndividuoDatosPrincipalesDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTipo() != Solicitud.Tipo.INDIVIDUO) {
            return ResponseEntity.badRequest().build();
        }

        IndividuoDatosPrincipalesDTO dto = new IndividuoDatosPrincipalesDTO();
        dto.setComoNosConocio(solicitud.getComoNosConocio());

        if (solicitud.getTitular() != null && solicitud.getTitular() instanceof Persona persona) {
            dto.setNombres(persona.getNombres());
            dto.setApellidos(persona.getApellidos());
            dto.setCelular(persona.getCelular());
            dto.setCorreoElectronico(persona.getCorreoElectronico());
        }

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/datos-principales/{solicitudId}")
    @Operation(summary = "Actualizar Datos Principales", description = "Actualiza los datos principales de una solicitud de individuo")
    public ResponseEntity<Solicitud> actualizarDatosPrincipales(
            @PathVariable Long solicitudId,
            @Parameter(description = "Datos principales actualizados") @Valid @RequestBody IndividuoDatosPrincipalesDTO dto) {

        log.info("Actualizando datos principales de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTipo() != Solicitud.Tipo.INDIVIDUO) {
            return ResponseEntity.badRequest().build();
        }

        Persona titular = null;
        if (solicitud.getTitular() == null) {
            titular = new Persona();
            titular.setTipo(Persona.Tipo.TITULAR);
            solicitud.setTitular(titular);
        } else {
            titular = (Persona) solicitud.getTitular();
        }

        titular.setNombres(dto.getNombres());
        titular.setApellidos(dto.getApellidos());
        titular.setCelular(dto.getCelular());
        titular.setCorreoElectronico(dto.getCorreoElectronico());
        solicitud.setComoNosConocio(dto.getComoNosConocio());

        Solicitud solicitudActualizada = solicitudService.actualizar(solicitud);
        return ResponseEntity.ok(solicitudActualizada);
    }

    // ========== DATOS PERSONALES ==========

    @GetMapping("/datos-personales/{solicitudId}")
    @Operation(summary = "Obtener Datos Personales", description = "Obtiene los datos personales del individuo")
    public ResponseEntity<IndividuoDatosPersonalesDTO> obtenerDatosPersonales(@PathVariable Long solicitudId) {
        log.info("Obteniendo datos personales de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<IndividuoDatosPersonalesDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTipo() != Solicitud.Tipo.INDIVIDUO) {
            return ResponseEntity.badRequest().build();
        }

        IndividuoDatosPersonalesDTO dto = new IndividuoDatosPersonalesDTO();
        if (solicitud.getTitular() != null && solicitud.getTitular() instanceof Persona persona) {
            dto.setTipoID(persona.getTipoID());
            dto.setIdNumero(persona.getIdNumero());
            dto.setFechaNacimiento(persona.getFechaNacimiento());
            dto.setLugarNacimiento(persona.getLugarNacimiento());
            dto.setNacionalidad(persona.getNacionalidad());
            dto.setPaisOrigen(persona.getPaisOrigen());
            dto.setPaisResidencia(persona.getPaisResidencia());
            dto.setActividad(persona.getActividad());
            dto.setSexo(persona.getSexo());
            dto.setEstadoCivil(persona.getEstadoCivil());
            dto.setDniFrenteArchivoId(persona.getDniFrente() != null ? persona.getDniFrente().getId() : null);
            dto.setDniReversoArchivoId(persona.getDniReverso() != null ? persona.getDniReverso().getId() : null);
            if (persona.getConyuge() != null) {
                ConyugeDTO conyugeDTO = new ConyugeDTO();
                conyugeDTO.setTipoID(persona.getConyuge().getTipoID());
                conyugeDTO.setIdNumero(persona.getConyuge().getIdNumero());
                conyugeDTO.setTipoClaveFiscal(persona.getConyuge().getTipoClaveFiscal());
                conyugeDTO.setClaveFiscal(persona.getConyuge().getClaveFiscal());
                dto.setConyuge(conyugeDTO);
            }
        }
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/datos-personales/{solicitudId}")
    @Operation(summary = "Actualizar Datos Personales", description = "Actualiza los datos personales del individuo")
    public ResponseEntity<Solicitud> actualizarDatosPersonales(
            @PathVariable Long solicitudId,
            @Parameter(description = "Datos personales del individuo") @Valid @RequestBody IndividuoDatosPersonalesDTO dto) {

        log.info("Actualizando datos personales de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTipo() != Solicitud.Tipo.INDIVIDUO) {
            return ResponseEntity.badRequest().build();
        }

        if (solicitud.getTitular() != null && solicitud.getTitular() instanceof Persona persona) {
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

            if (dto.getConyuge() != null && dto.getConyuge().getTipoID() != null) {
                if (persona.getConyuge() == null) {
                    persona.setConyuge(new Conyuge());
                }
                persona.getConyuge().setTipoID(dto.getConyuge().getTipoID());
                persona.getConyuge().setIdNumero(dto.getConyuge().getIdNumero());
                persona.getConyuge().setTipoClaveFiscal(dto.getConyuge().getTipoClaveFiscal());
                persona.getConyuge().setClaveFiscal(dto.getConyuge().getClaveFiscal());
            }
        }

        Solicitud solicitudActualizada = solicitudService.actualizar(solicitud);
        return ResponseEntity.ok(solicitudActualizada);
    }

    // ========== DOMICILIO ==========

    @GetMapping("/domicilio/{solicitudId}")
    @Operation(summary = "Obtener Domicilio", description = "Obtiene el domicilio del individuo")
    public ResponseEntity<DomicilioDTO> obtenerDomicilio(@PathVariable Long solicitudId) {
        log.info("Obteniendo domicilio de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<DomicilioDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();

        DomicilioDTO dto = new DomicilioDTO();
        if (solicitud.getTitular() != null &&
                solicitud.getTitular() instanceof Persona persona &&
                persona.getDomicilio() != null) {
            dto.setTipo(persona.getDomicilio().getTipo());
            dto.setCalle(persona.getDomicilio().getCalle());
            dto.setNumero(persona.getDomicilio().getNumero());
            dto.setPiso(persona.getDomicilio().getPiso());
            dto.setDepto(persona.getDomicilio().getDepto());
            dto.setBarrio(persona.getDomicilio().getBarrio());
            dto.setCiudad(persona.getDomicilio().getCiudad());
            dto.setProvincia(persona.getDomicilio().getProvincia());
            dto.setPais(persona.getDomicilio().getPais());
            dto.setCp(persona.getDomicilio().getCp());
        }

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/domicilio/{solicitudId}")
    @Operation(summary = "Actualizar Domicilio", description = "Actualiza el domicilio del individuo")
    public ResponseEntity<Solicitud> actualizarDomicilio(
            @PathVariable Long solicitudId,
            @Parameter(description = "Datos del domicilio") @Valid @RequestBody DomicilioDTO dto) {

        log.info("Actualizando domicilio de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTitular() != null &&
                solicitud.getTitular() instanceof Persona persona) {

            if (persona.getDomicilio() == null) {
                persona.setDomicilio(new Domicilio());
            }

            persona.getDomicilio().setTipo(dto.getTipo());
            persona.getDomicilio().setCalle(dto.getCalle());
            persona.getDomicilio().setNumero(dto.getNumero());
            persona.getDomicilio().setPiso(dto.getPiso());
            persona.getDomicilio().setDepto(dto.getDepto());
            persona.getDomicilio().setBarrio(dto.getBarrio());
            persona.getDomicilio().setCiudad(dto.getCiudad());
            persona.getDomicilio().setProvincia(dto.getProvincia());
            persona.getDomicilio().setPais(dto.getPais());
            persona.getDomicilio().setCp(dto.getCp());
        }

        Solicitud solicitudActualizada = solicitudService.actualizar(solicitud);
        return ResponseEntity.ok(solicitudActualizada);
    }

    // ========== DATOS FISCALES ==========

    @GetMapping("/datos-fiscales/{solicitudId}")
    @Operation(summary = "Obtener Datos Fiscales", description = "Obtiene los datos fiscales del individuo")
    public ResponseEntity<DatosFiscalesDTO> obtenerDatosFiscales(@PathVariable Long solicitudId) {
        log.info("Obteniendo datos fiscales de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<DatosFiscalesDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        DatosFiscalesDTO dto = new DatosFiscalesDTO();

        if (solicitud.getTitular() != null &&
                solicitud.getTitular() instanceof Persona persona &&
                persona.getDatosFiscales() != null) {
            dto.setTipo(persona.getDatosFiscales().getTipo());
            dto.setClaveFiscal(persona.getDatosFiscales().getClaveFiscal());
            dto.setTipoIva(persona.getDatosFiscales().getTipoIva());
            dto.setTipoGanancia(persona.getDatosFiscales().getTipoGanancia());
        }

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/datos-fiscales/{solicitudId}")
    @Operation(summary = "Actualizar Datos Fiscales", description = "Actualiza los datos fiscales del individuo")
    public ResponseEntity<Solicitud> actualizarDatosFiscales(
            @PathVariable Long solicitudId,
            @Parameter(description = "Datos fiscales") @Valid @RequestBody DatosFiscalesDTO dto) {

        log.info("Actualizando datos fiscales de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTitular() != null &&
                solicitud.getTitular() instanceof Persona persona) {
            if (persona.getDatosFiscales() == null) {
                persona.setDatosFiscales(new DatosFiscales());
            }
            persona.getDatosFiscales().setTipo(dto.getTipo());
            persona.getDatosFiscales().setClaveFiscal(dto.getClaveFiscal());
            persona.getDatosFiscales().setTipoIva(dto.getTipoIva());
            persona.getDatosFiscales().setTipoGanancia(dto.getTipoGanancia());
        }

        Solicitud solicitudActualizada = solicitudService.actualizar(solicitud);
        return ResponseEntity.ok(solicitudActualizada);
    }

    // ========== DECLARACIONES ==========

    @GetMapping("/declaraciones/{solicitudId}")
    @Operation(summary = "Obtener Declaraciones", description = "Obtiene las declaraciones del individuo")
    public ResponseEntity<IndividuoDeclaracionesDTO> obtenerDeclaraciones(@PathVariable Long solicitudId) {
        log.info("Obteniendo declaraciones de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<IndividuoDeclaracionesDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        IndividuoDeclaracionesDTO dto = new IndividuoDeclaracionesDTO();
        if (solicitud.getTitular() != null &&
                solicitud.getTitular() instanceof Persona persona) {
            dto.setEsPep(persona.getEsPep() != null ? persona.getEsPep() : false);
            dto.setMotivoPep(persona.getMotivoPep());
            dto.setEsFATCA(persona.getEsFATCA() != null ? persona.getEsFATCA() : false);
            dto.setMotivoFatca(persona.getMotivoFatca());
            dto.setDeclaraUIF(persona.getDeclaraUIF() != null ? persona.getDeclaraUIF() : false);
            dto.setMotivoUIF(persona.getMotivoUIF());
        }

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/declaraciones/{solicitudId}")
    @Operation(summary = "Actualizar Declaraciones", description = "Actualiza las declaraciones del individuo")
    public ResponseEntity<Solicitud> actualizarDeclaraciones(
            @PathVariable Long solicitudId,
            @Parameter(description = "Declaraciones") @Valid @RequestBody IndividuoDeclaracionesDTO dto) {

        log.info("Actualizando declaraciones de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTitular() != null &&
                solicitud.getTitular() instanceof Persona persona) {
            persona.setEsPep(dto.isEsPep());
            persona.setMotivoPep(dto.getMotivoPep());
            persona.setEsFATCA(dto.isEsFATCA());
            persona.setMotivoFatca(dto.getMotivoFatca());
            persona.setDeclaraUIF(dto.isDeclaraUIF());
            persona.setMotivoUIF(dto.getMotivoUIF());
        }

        Solicitud solicitudActualizada = solicitudService.actualizar(solicitud);
        return ResponseEntity.ok(solicitudActualizada);
    }

    // ========== CUENTAS BANCARIAS ==========

    @GetMapping("/cuentas-bancarias/{solicitudId}")
    @Operation(summary = "Obtener Cuentas Bancarias", description = "Obtiene las cuentas bancarias del individuo")
    public ResponseEntity<CuentasBancariasDTO> obtenerCuentasBancarias(@PathVariable Long solicitudId) {
        log.info("Obteniendo cuentas bancarias de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<CuentasBancariasDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTipo() != Solicitud.Tipo.INDIVIDUO) {
            return ResponseEntity.badRequest().build();
        }

        CuentasBancariasDTO dto = new CuentasBancariasDTO();
        if (solicitud.getCuentasBancarias() != null) {
            // Filtrar solo cuentas argentinas (país null o Argentina)
            List<CuentaBancaria> cuentasArgentinas = solicitud.getCuentasBancarias().stream()
                    .filter(cuenta -> cuenta.getPais() == null || "Argentina".equalsIgnoreCase(cuenta.getPais()))
                    .collect(Collectors.toList());
            dto.setCuentasBancarias(cuentasArgentinas);
        } else {
            dto.setCuentasBancarias(new ArrayList<>());
        }

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/cuentas-bancarias/{solicitudId}")
    @Operation(summary = "Actualizar Cuentas Bancarias", description = "Actualiza las cuentas bancarias del individuo")
    public ResponseEntity<Solicitud> actualizarCuentasBancarias(
            @PathVariable Long solicitudId,
            @Parameter(description = "Cuentas bancarias") @Valid @RequestBody CuentasBancariasDTO dto) {

        log.info("Actualizando cuentas bancarias de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTipo() != Solicitud.Tipo.INDIVIDUO) {
            return ResponseEntity.badRequest().build();
        }

        List<CuentaBancaria> cuentasRequest = dto.getCuentasBancarias();
        if (cuentasRequest == null) {
            cuentasRequest = new ArrayList<>();
        }

        List<CuentaBancaria> cuentasPersistentes = solicitud.getCuentasBancarias();
        if (cuentasPersistentes == null) {
            cuentasPersistentes = new ArrayList<>();
            solicitud.setCuentasBancarias(cuentasPersistentes);
        }
        final List<CuentaBancaria> cuentasActuales = cuentasPersistentes;

        Map<Long, CuentaBancaria> cuentasPorId = cuentasActuales.stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(CuentaBancaria::getId, Function.identity()));

        Set<Long> idsRecibidos = cuentasRequest.stream()
                .map(CuentaBancaria::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        cuentasActuales.removeIf(c -> c.getId() != null && !idsRecibidos.contains(c.getId()));

        cuentasRequest.forEach(cuenta -> {
            if (cuenta.getId() != null) {
                CuentaBancaria existente = cuentasPorId.get(cuenta.getId());
                if (existente != null) {
                    existente.setTipo(cuenta.getTipo());
                    existente.setBanco(cuenta.getBanco());
                    existente.setMoneda(cuenta.getMoneda());
                    existente.setTipoClaveBancaria(cuenta.getTipoClaveBancaria());
                    existente.setClaveBancaria(cuenta.getClaveBancaria());
                    existente.setPais(cuenta.getPais());
                    existente.setTipoCliente(cuenta.getTipoCliente());
                    existente.setNumeroAba(cuenta.getNumeroAba());
                    existente.setIdentificacionSwift(cuenta.getIdentificacionSwift());
                    return;
                }
            }

            CuentaBancaria nuevaCuenta = new CuentaBancaria();
            nuevaCuenta.setTipo(cuenta.getTipo());
            nuevaCuenta.setBanco(cuenta.getBanco());
            nuevaCuenta.setMoneda(cuenta.getMoneda());
            nuevaCuenta.setTipoClaveBancaria(cuenta.getTipoClaveBancaria());
            nuevaCuenta.setClaveBancaria(cuenta.getClaveBancaria());
            nuevaCuenta.setPais(cuenta.getPais());
            nuevaCuenta.setTipoCliente(cuenta.getTipoCliente());
            nuevaCuenta.setNumeroAba(cuenta.getNumeroAba());
            nuevaCuenta.setIdentificacionSwift(cuenta.getIdentificacionSwift());
            cuentasActuales.add(nuevaCuenta);
        });

        Solicitud solicitudActualizada = solicitudService.actualizar(solicitud);
        return ResponseEntity.ok(solicitudActualizada);
    }

    // ========== PERFIL INVERSOR ==========

    @GetMapping("/perfil-inversor/{solicitudId}")
    @Operation(summary = "Obtener Perfil del Inversor", description = "Obtiene el perfil de inversor del individuo")
    public ResponseEntity<IndividuoPerfilInversorDTO> obtenerPerfilInversor(@PathVariable Long solicitudId) {
        log.info("Obteniendo perfil de inversor de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<IndividuoPerfilInversorDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();

        IndividuoPerfilInversorDTO dto = new IndividuoPerfilInversorDTO();

        // Obtener todas las preguntas habilitadas
        List<ar.com.st.entity.PerfilInversorPregunta> preguntasEntity = perfilInversorPreguntaRepository
                .findByHabilitadaTrue();

        // Mapear preguntas a DTOs
        List<PerfilInversorPreguntaDTO> preguntasDTO = preguntasEntity.stream().map(pregunta -> {
            PerfilInversorPreguntaDTO preguntaDTO = new PerfilInversorPreguntaDTO();
            preguntaDTO.setId(pregunta.getId());
            preguntaDTO.setNombreCorto(pregunta.getNombreCorto());
            preguntaDTO.setPregunta(pregunta.getPregunta());
            preguntaDTO.setHabilitada(pregunta.isHabilitada());

            // Mapear opciones
            if (pregunta.getOpciones() != null) {
                List<PerfilInversorOpcionDTO> opcionesDTO = pregunta.getOpciones().stream().map(opcion -> {
                    PerfilInversorOpcionDTO opcionDTO = new PerfilInversorOpcionDTO();
                    opcionDTO.setId(opcion.getId());
                    opcionDTO.setValor(opcion.getValor());
                    opcionDTO.setPuntaje(opcion.getPuntaje());
                    opcionDTO.setDeterminante(opcion.isDeterminante());
                    opcionDTO.setTipoPerfil(opcion.getTipoPerfil() != null ? opcion.getTipoPerfil().toString() : null);
                    return opcionDTO;
                }).collect(Collectors.toList());
                preguntaDTO.setOpciones(opcionesDTO);
            }

            return preguntaDTO;
        }).collect(Collectors.toList());

        dto.setPreguntas(preguntasDTO);

        // Obtener respuestas del cliente si existen
        List<PerfilInversorRespuestaDTO> respuestasDTO = new ArrayList<>();
        if (solicitud.getTitular() != null &&
                solicitud.getTitular() instanceof Persona persona &&
                persona.getPerfilInversor() != null &&
                persona.getPerfilInversor().getRespuestas() != null) {

            respuestasDTO = persona.getPerfilInversor().getRespuestas().stream().map(respuesta -> {
                PerfilInversorRespuestaDTO respuestaDTO = new PerfilInversorRespuestaDTO();
                respuestaDTO.setPreguntaId(respuesta.getPerfilInversorPregunta().getId());
                respuestaDTO.setOpcionId(respuesta.getPerfilInversorPreguntaOpcion().getId());
                return respuestaDTO;
            }).collect(Collectors.toList());

            if (persona.getPerfilInversor().getTipo() != null) {
                dto.setTipoPerfil(persona.getPerfilInversor().getTipo().getDescripcion());
            }
        }

        dto.setRespuestas(respuestasDTO);

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/perfil-inversor/{solicitudId}")
    @Operation(summary = "Actualizar Perfil del Inversor", description = "Actualiza el perfil de inversor del individuo")
    public ResponseEntity<Solicitud> actualizarPerfilInversor(
            @PathVariable Long solicitudId,
            @Parameter(description = "Perfil de inversor") @Valid @RequestBody IndividuoPerfilInversorDTO dto) {

        log.info("Actualizando perfil de inversor de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();

        if (solicitud.getTitular() != null && solicitud.getTitular() instanceof Persona persona) {

            // Crear o obtener el perfil inversor de la persona
            ar.com.st.entity.PerfilInversor perfilInversor = persona.getPerfilInversor();
            if (perfilInversor == null) {
                perfilInversor = new ar.com.st.entity.PerfilInversor();
                persona.setPerfilInversor(perfilInversor);
            }

            if(StringUtils.isNotBlank(dto.getTipoPerfil())) {
                perfilInversor.setTipo(ar.com.st.entity.PerfilInversor.Tipo.valueOf(dto.getTipoPerfil()));
            }

            // Agregar nuevas respuestas
            if (dto.getRespuestas() != null) {
                // Limpiar respuestas existentes
                perfilInversor.getRespuestas().clear();
                for (PerfilInversorRespuestaDTO respuestaDTO : dto.getRespuestas()) {
                    PerfilInversorRespuesta respuesta = new PerfilInversorRespuesta();

                    // Buscar la pregunta por ID
                    Optional<ar.com.st.entity.PerfilInversorPregunta> preguntaOpt = perfilInversorPreguntaRepository
                            .findById(respuestaDTO.getPreguntaId());
                    if (preguntaOpt.isPresent()) {
                        respuesta.setPerfilInversorPregunta(preguntaOpt.get());

                        // Buscar la opción seleccionada
                        ar.com.st.entity.PerfilInversorPregunta pregunta = preguntaOpt.get();
                        if (pregunta.getOpciones() != null) {
                            Optional<ar.com.st.entity.PerfilInversorPreguntaOpcion> opcionOpt = pregunta.getOpciones()
                                    .stream()
                                    .filter(opcion -> opcion.getId().equals(respuestaDTO.getOpcionId()))
                                    .findFirst();

                            if (opcionOpt.isPresent()) {
                                respuesta.setPerfilInversorPreguntaOpcion(opcionOpt.get());
                                respuesta.setPerfilInversor(perfilInversor);
                                perfilInversor.getRespuestas().add(respuesta);
                            }
                        }
                    }
                }
            }
        }

        Solicitud solicitudActualizada = solicitudService.actualizar(solicitud);
        return ResponseEntity.ok(solicitudActualizada);
    }


    // ========== DECLARACIÓN DE INGRESOS ==========

    @GetMapping("/ddjj-origen-fondos/{solicitudId}")
    @Operation(summary = "Obtener Declaración de Ingresos", description = "Obtiene la declaración de origen de fondos del individuo")
    public ResponseEntity<IndividuoDdJJOrigenFondosDTO> obtenerDdJJOrigenFondos(@PathVariable Long solicitudId) {
        log.info("Obteniendo DDJJ origen fondos de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<IndividuoDdJJOrigenFondosDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTipo() != Solicitud.Tipo.INDIVIDUO) {
            return ResponseEntity.badRequest().build();
        }

        IndividuoDdJJOrigenFondosDTO dto = new IndividuoDdJJOrigenFondosDTO();
        dto.setSolicitudId(solicitud.getId());
        dto.setDdJjOrigenFondos(solicitud.getDdJjOrigenFondos());
        dto.setComprobanteDdJjOrigenFondosId(
                solicitud.getComprobanteDdJjOrigenFondos() != null ? solicitud.getComprobanteDdJjOrigenFondos().getId()
                        : null);

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/ddjj-origen-fondos/{solicitudId}")
    @Operation(summary = "Actualizar Declaración de Ingresos", description = "Actualiza la declaración de origen de fondos del individuo")
    public ResponseEntity<Solicitud> actualizarDdJJOrigenFondos(
            @PathVariable Long solicitudId,
            @Parameter(description = "Declaración de origen de fondos") @Valid @RequestBody IndividuoDdJJOrigenFondosDTO dto) {

        log.info("Actualizando DDJJ origen fondos de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTipo() != Solicitud.Tipo.INDIVIDUO) {
            return ResponseEntity.badRequest().build();
        }

        // Actualizar tipos de origen de fondos
        if (dto.getDdJjOrigenFondos() != null) {
            solicitud.setDdJjOrigenFondos(dto.getDdJjOrigenFondos());
        }

        // Actualizar archivo de comprobante si se proporciona
        if (dto.getComprobanteDdJjOrigenFondosId() != null) {
            archivoRepository.findById(dto.getComprobanteDdJjOrigenFondosId())
                    .ifPresent(archivo -> solicitud.setComprobanteDdJjOrigenFondos(archivo));
        } else {
            // Si no se proporciona ID, limpiar el archivo
            solicitud.setComprobanteDdJjOrigenFondos(null);
        }

        Solicitud solicitudActualizada = solicitudService.actualizar(solicitud);
        return ResponseEntity.ok(solicitudActualizada);
    }

    // ========== TÉRMINOS Y CONDICIONES ==========

    @GetMapping("/terminos-condiciones/{solicitudId}")
    @Operation(summary = "Obtener Términos y Condiciones", description = "Obtiene los términos y condiciones de la solicitud")
    public ResponseEntity<TerminosCondicionesDTO> obtenerTerminosCondiciones(@PathVariable Long solicitudId) {
        log.info("Obteniendo términos y condiciones de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<TerminosCondicionesDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        // TODO: Mapear términos y condiciones al DTO
        TerminosCondicionesDTO dto = new TerminosCondicionesDTO();
        dto.setAceptaTerminosCondiciones(solicitud.isAceptaTerminosCondiciones());
        dto.setAceptaReglamentoGestionFondos(solicitud.isAceptaReglamentoGestionFondos());
        dto.setAceptaComisiones(solicitud.isAceptaComisiones());

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/terminos-condiciones/{solicitudId}")
    @Operation(summary = "Actualizar Términos y Condiciones", description = "Finaliza la solicitud con términos y condiciones")
    public ResponseEntity<Solicitud> actualizarTerminosCondiciones(
            @PathVariable Long solicitudId,
            @Parameter(description = "Términos y condiciones") @Valid @RequestBody TerminosCondicionesDTO dto) {

        log.info("Finalizando solicitud de individuo con términos y condiciones: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        solicitud.setAceptaTerminosCondiciones(dto.isAceptaTerminosCondiciones());
        solicitud.setAceptaReglamentoGestionFondos(dto.isAceptaReglamentoGestionFondos());
        solicitud.setAceptaComisiones(dto.isAceptaComisiones());

        // Finalizar la carga de la solicitud
        Solicitud solicitudFinalizada = solicitudService.finalizarCarga(solicitudId);
        return ResponseEntity.ok(solicitudFinalizada);
    }

    // ========== DATOS FISCALES EXTERIOR (CONDICIONAL) ==========

    @PutMapping("/datos-fiscales-exterior/{solicitudId}")
    @Operation(summary = "Datos Fiscales Exterior", description = "Actualiza los datos fiscales del exterior (solo si no es país local)")
    public ResponseEntity<Solicitud> datosFiscalesExterior(
            @PathVariable Long solicitudId,
            @Parameter(description = "Datos fiscales del exterior") @Valid @RequestBody DatosFiscalesDTO dto) {

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTitular() != null && solicitud.getTitular() instanceof Persona persona) {
            if (persona.getDatosFiscalesExterior() == null) {
                persona.setDatosFiscalesExterior(new DatosFiscales());
            }

            persona.getDatosFiscalesExterior().setTipo(dto.getTipo());
            persona.getDatosFiscalesExterior().setClaveFiscal(dto.getClaveFiscal());
            persona.getDatosFiscalesExterior().setResidenciaFiscal(dto.getResidenciaFiscal());
        }

        Solicitud solicitudActualizada = solicitudService.actualizar(solicitud);
        return ResponseEntity.ok(solicitudActualizada);
    }

    @GetMapping("/datos-fiscales-exterior/{solicitudId}")
    @Operation(summary = "Obtener Datos Fiscales exterior", description = "Obtiene los datos fiscales del exterior (solo si no es país local)")
    public ResponseEntity<DatosFiscalesDTO> obtenerDatosFiscalesExterior(@PathVariable Long solicitudId) {
        log.info("Obteniendo datos fiscales de exterior de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<DatosFiscalesDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        DatosFiscalesDTO dto = new DatosFiscalesDTO();
        dto.setDebeCompletarFiscalExterior(false);
        if (solicitud.getTitular() != null &&
                solicitud.getTitular() instanceof Persona persona &&
                persona.getDatosFiscales() != null) {
            if (persona.getDomicilio().getPais() != null
                    && !"Argentina".equalsIgnoreCase(persona.getDomicilio().getPais())) {
                dto.setTipo(persona.getDatosFiscales().getTipo());
                dto.setClaveFiscal(persona.getDatosFiscales().getClaveFiscal());
                dto.setTipoIva(persona.getDatosFiscales().getTipoIva());
                dto.setTipoGanancia(persona.getDatosFiscales().getTipoGanancia());
                dto.setDebeCompletarFiscalExterior(true);
            }
        }

        return ResponseEntity.ok(dto);
    }

    // ========== CUENTA BANCARIA EXTERIOR (CONDICIONAL) ==========

    @PutMapping("/cuentas-bancarias-exterior/{solicitudId}")
    @Operation(summary = "Cuentas Bancarias Exterior", description = "Actualiza la cuenta bancaria del exterior (solo si no es país local)")
    public ResponseEntity<Solicitud> cuentaBancariaExterior(
            @PathVariable Long solicitudId,
            @Parameter(description = "Cuenta bancaria del exterior") @Valid @RequestBody CuentasBancariasDTO dto) {

        log.info("Actualizando cuentas bancarias del exterior para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Solicitud>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTipo() != Solicitud.Tipo.INDIVIDUO) {
            return ResponseEntity.badRequest().build();
        }

        // Inicializar lista si es null
        if (CollectionUtils.isEmpty(solicitud.getCuentasBancarias())) {
            solicitud.setCuentasBancarias(new ArrayList<>());
        }

        // Remover cuentas del exterior existentes
        List<CuentaBancaria> cuentasArgentinas = solicitud.getCuentasBancarias().stream()
                .filter(cuenta -> cuenta.getPais() == null || "Argentina".equalsIgnoreCase(cuenta.getPais()))
                .collect(Collectors.toList());

        // Limpiar todas las cuentas y agregar solo las argentinas
        solicitud.getCuentasBancarias().clear();
        solicitud.getCuentasBancarias().addAll(cuentasArgentinas);

        // Agregar las nuevas cuentas del exterior
        if (dto.getCuentasBancarias() != null) {
            solicitud.getCuentasBancarias().addAll(dto.getCuentasBancarias());
        }

        Solicitud solicitudActualizada = solicitudService.actualizar(solicitud);
        return ResponseEntity.ok(solicitudActualizada);
    }

    @GetMapping("/cuentas-bancarias-exterior/{solicitudId}")
    @Operation(summary = "Obtener Cuentas Bancarias Exterior", description = "Obtiene las cuentas bancarias del exterior (solo si no es país local)")
    public ResponseEntity<CuentasBancariasDTO> obtenerCuentasBancariasExterior(@PathVariable Long solicitudId) {
        log.info("Obteniendo cuentas bancarias de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<CuentasBancariasDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        CuentasBancariasDTO dto = new CuentasBancariasDTO();
        dto.setDebeCompletarCuentasBancariasExterior(false);

        if (solicitud.getTitular() != null &&
                solicitud.getTitular() instanceof Persona persona &&
                persona.getDomicilio().getPais() != null &&
                !"Argentina".equalsIgnoreCase(persona.getDomicilio().getPais())) {
            dto.setDebeCompletarCuentasBancariasExterior(true);
            if (CollectionUtils.isEmpty(solicitud.getCuentasBancarias())) {
                dto.setCuentasBancarias(new ArrayList<>());
            } else {
                dto.setCuentasBancarias(solicitud.getCuentasBancarias().stream()
                        .filter(cuenta -> cuenta.getPais() != null && !"Argentina".equalsIgnoreCase(cuenta.getPais()))
                        .collect(Collectors.toList()));
            }
        }

        return ResponseEntity.ok(dto);
    }
}
