package ar.com.st.controller;

import ar.com.st.dto.comun.AccionistaCreadoResponse;
import ar.com.st.dto.comun.*;
import ar.com.st.dto.empresa.*;
import ar.com.st.dto.individuo.FirmanteDetalleDTO;
import ar.com.st.dto.individuo.IndividuoDatosPersonalesDTO;
import ar.com.st.dto.individuo.IndividuoDatosPrincipalesDTO;
import ar.com.st.dto.individuo.IndividuoDeclaracionesDTO;
import ar.com.st.dto.individuo.IndividuoPerfilInversorDTO;
import ar.com.st.dto.individuo.PerfilInversorOpcionDTO;
import ar.com.st.dto.individuo.PerfilInversorPreguntaDTO;
import ar.com.st.dto.individuo.PerfilInversorRespuestaDTO;
import ar.com.st.entity.Conyuge;
import ar.com.st.entity.CuentaBancaria;
import ar.com.st.entity.DatosFiscales;
import ar.com.st.entity.Domicilio;
import ar.com.st.entity.Empresa;
import ar.com.st.entity.Organizacion;
import ar.com.st.entity.PerfilInversor;
import ar.com.st.entity.PerfilInversorRespuesta;
import ar.com.st.entity.Persona;
import ar.com.st.entity.Solicitud;
import ar.com.st.entity.Usuario;
import ar.com.st.repository.ArchivoRepository;
import ar.com.st.repository.PerfilInversorPreguntaRepository;
import ar.com.st.service.AuthService;
import ar.com.st.service.SolicitudService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Controlador para apertura de cuenta de empresa
 * 
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@RestController
@RequestMapping("/api/empresa-apertura")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Apertura de Cuenta Empresa", description = "API para apertura de cuenta de empresa paso a paso")
@SecurityRequirement(name = "bearerAuth")
public class EmpresaAperturaController {

    private final SolicitudService solicitudService;
    private final ArchivoRepository archivoRepository;
    private final PerfilInversorPreguntaRepository perfilInversorPreguntaRepository;
    private final AuthService authService;
    
    @PersistenceContext
    private EntityManager entityManager;
    
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
    @Operation(summary = "Obtener Datos Principales", description = "Obtiene los datos principales de una solicitud de empresa")
    public ResponseEntity<EmpresaDatosPrincipalesDTO> obtenerDatosPrincipales(@PathVariable Long solicitudId) {
        log.info("Obteniendo datos principales de empresa para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<EmpresaDatosPrincipalesDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO) {
            return ResponseEntity.badRequest().build();
        }

        EmpresaDatosPrincipalesDTO dto = new EmpresaDatosPrincipalesDTO();
        dto.setComoNosConocio(solicitud.getComoNosConocio());

        Empresa titular = (Empresa) solicitud.getTitular();
        if (titular != null) {
            dto.setDenominacion(titular.getDenominacion());
            dto.setTipoEmpresa(titular.getTipoEmpresa());
            dto.setTelefono(titular.getTelefono());
            dto.setCelular(titular.getCelular());
            dto.setCorreoElectronico(titular.getCorreoElectronico());
            dto.setUsoFirma(titular.getUsoFirma());
            dto.setActividad(titular.getActividad());
        }

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/datos-principales/{solicitudId}")
    @Operation(summary = "Actualizar Datos Principales", description = "Actualiza los datos principales de una solicitud de empresa")
    public ResponseEntity<Void> actualizarDatosPrincipales(
            @PathVariable Long solicitudId,
            @Parameter(description = "Datos principales actualizados") @Valid @RequestBody EmpresaDatosPrincipalesDTO dto) {

        log.info("Actualizando datos principales de empresa para solicitud: {}", solicitudId);

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

        if (solicitud.getTitular() == null) {
            solicitud.setTitular(new Empresa());
        }
        ((Empresa) solicitud.getTitular()).setDenominacion(dto.getDenominacion());
        ((Empresa) solicitud.getTitular()).setTipoEmpresa(dto.getTipoEmpresa());
        ((Empresa) solicitud.getTitular()).setTelefono(dto.getTelefono());
        ((Empresa) solicitud.getTitular()).setCelular(dto.getCelular());
        ((Empresa) solicitud.getTitular()).setCorreoElectronico(dto.getCorreoElectronico());
        ((Empresa) solicitud.getTitular()).setUsoFirma(dto.getUsoFirma());
        ((Empresa) solicitud.getTitular()).setActividad(dto.getActividad());

        solicitud.setComoNosConocio(dto.getComoNosConocio());

        solicitudService.actualizar(solicitud);
        return ResponseEntity.ok().build();
    }

    // ========== DOMICILIO ==========

    @GetMapping("/domicilio/{solicitudId}")
    @Operation(summary = "Obtener Domicilio", description = "Obtiene el domicilio de la empresa")
    public ResponseEntity<DomicilioDTO> obtenerDomicilio(@PathVariable Long solicitudId) {
        log.info("Obteniendo domicilio de empresa para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<DomicilioDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        Domicilio domicilio = solicitud.getTitular() != null && solicitud.getTitular() instanceof Empresa empresa
                ? empresa.getDomicilio()
                : null;
        if (domicilio == null) {
            domicilio = new Domicilio();
        }

        DomicilioDTO dto = new DomicilioDTO();
        dto.setTipo(domicilio.getTipo());
        dto.setCalle(domicilio.getCalle());
        dto.setNumero(domicilio.getNumero());
        dto.setPiso(domicilio.getPiso());
        dto.setDepto(domicilio.getDepto());
        dto.setBarrio(domicilio.getBarrio());
        dto.setCiudad(domicilio.getCiudad());
        dto.setProvincia(domicilio.getProvincia());
        dto.setPais(domicilio.getPais());
        dto.setCp(domicilio.getCp());

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/domicilio/{solicitudId}")
    @Operation(summary = "Actualizar Domicilio", description = "Actualiza el domicilio de la empresa")
    public ResponseEntity<Void> actualizarDomicilio(
            @PathVariable Long solicitudId,
            @Parameter(description = "Datos del domicilio") @Valid @RequestBody DomicilioDTO dto) {

        log.info("Actualizando domicilio de empresa para solicitud: {}", solicitudId);

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

        if (solicitud.getTitular() != null && solicitud.getTitular() instanceof Empresa empresa) {
            Domicilio domicilio = empresa.getDomicilio();

            if (domicilio == null) {
                domicilio = new Domicilio();
            }

            domicilio.setTipo(dto.getTipo());
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

        solicitudService.actualizar(solicitud);
        return ResponseEntity.ok().build();
    }

    // ========== DATOS EMPRESA ==========

    @GetMapping("/datos-empresa/{solicitudId}")
    @Operation(summary = "Obtener Datos de la Organización", description = "Obtiene los datos específicos de la empresa")
    public ResponseEntity<EmpresaDatosEmpresaDTO> obtenerDatosEmpresa(@PathVariable Long solicitudId) {
        log.info("Obteniendo datos de empresa para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<EmpresaDatosEmpresaDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        Empresa empresa = (Empresa) solicitud.getTitular();

        if (empresa == null) {
            empresa = new Empresa();
        }

        EmpresaDatosEmpresaDTO dto = new EmpresaDatosEmpresaDTO();
        dto.setFechaConstitucion(empresa.getFechaConstitucion());
        dto.setNumeroActa(empresa.getNumeroActa());
        dto.setPaisOrigen(empresa.getPaisOrigen());
        dto.setPaisResidencia(empresa.getPaisResidencia());
        dto.setFechaCierreBalance(empresa.getFechaCierreBalance());

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/datos-empresa/{solicitudId}")
    @Operation(summary = "Actualizar Datos de la Organización", description = "Actualiza los datos específicos de la empresa")
    public ResponseEntity<Void> actualizarDatosEmpresa(
            @PathVariable Long solicitudId,
            @Parameter(description = "Datos de la empresa") @Valid @RequestBody EmpresaDatosEmpresaDTO dto) {

        log.info("Actualizando datos de empresa para solicitud: {}", solicitudId);

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

        if (solicitud.getTitular() != null && solicitud.getTitular() instanceof Empresa empresa) {
            empresa.setFechaConstitucion(dto.getFechaConstitucion());
            empresa.setNumeroActa(dto.getNumeroActa());
            empresa.setPaisOrigen(dto.getPaisOrigen());
            empresa.setPaisResidencia(dto.getPaisResidencia());
            empresa.setFechaCierreBalance(dto.getFechaCierreBalance());
        }

        solicitudService.actualizar(solicitud);

        return ResponseEntity.ok().build();
    }

    // ========== DATOS FISCALES ==========

    @GetMapping("/datos-fiscales/{solicitudId}")
    @Operation(summary = "Obtener Datos Fiscales", description = "Obtiene los datos fiscales de la empresa")
    public ResponseEntity<DatosFiscalesDTO> obtenerDatosFiscales(@PathVariable Long solicitudId) {
        log.info("Obteniendo datos fiscales de empresa para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<DatosFiscalesDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        Empresa empresa = (Empresa) solicitud.getTitular();

        if (empresa == null) {
            empresa = new Empresa();
        }

        DatosFiscalesDTO dto = new DatosFiscalesDTO();
        dto.setTipo(empresa.getDatosFiscales().getTipo());
        dto.setClaveFiscal(empresa.getDatosFiscales().getClaveFiscal());
        dto.setTipoIva(empresa.getDatosFiscales().getTipoIva());
        dto.setTipoGanancia(empresa.getDatosFiscales().getTipoGanancia());

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/datos-fiscales/{solicitudId}")
    @Operation(summary = "Actualizar Datos Fiscales", description = "Actualiza los datos fiscales de la empresa")
    public ResponseEntity<Void> actualizarDatosFiscales(
            @PathVariable Long solicitudId,
            @Parameter(description = "Datos fiscales") @Valid @RequestBody DatosFiscalesDTO dto) {

        log.info("Actualizando datos fiscales de empresa para solicitud: {}", solicitudId);

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

        if (solicitud.getTitular() != null && solicitud.getTitular() instanceof Empresa empresa) {
            empresa.getDatosFiscales().setTipo(dto.getTipo());
            empresa.getDatosFiscales().setClaveFiscal(dto.getClaveFiscal());
            empresa.getDatosFiscales().setTipoIva(dto.getTipoIva());
            empresa.getDatosFiscales().setTipoGanancia(dto.getTipoGanancia());
        }

        solicitudService.actualizar(solicitud);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/datos-fiscales-exterior/{solicitudId}")
    @Operation(summary = "Datos Fiscales Exterior", description = "Actualiza los datos fiscales del exterior (solo si no es país local)")
    public ResponseEntity<Void> datosFiscalesExterior(
            @PathVariable Long solicitudId,
            @Parameter(description = "Datos fiscales del exterior") @Valid @RequestBody DatosFiscalesDTO dto) {

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Void>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTitular() != null && solicitud.getTitular() instanceof Empresa empresa) {
            if (empresa.getDatosFiscalesExterior() == null) {
                empresa.setDatosFiscalesExterior(new DatosFiscales());
            }

            empresa.getDatosFiscalesExterior().setTipo(dto.getTipo());
            empresa.getDatosFiscalesExterior().setClaveFiscal(dto.getClaveFiscal());
            empresa.getDatosFiscalesExterior().setResidenciaFiscal(dto.getResidenciaFiscal());
        }

        solicitudService.actualizar(solicitud);
        return ResponseEntity.ok().build();
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
                solicitud.getTitular() instanceof Empresa empresa &&
                empresa.getDatosFiscales() != null) {
            if (empresa.getDomicilio().getPais() != null
                    && !"Argentina".equalsIgnoreCase(empresa.getDomicilio().getPais())) {
                dto.setTipo(empresa.getDatosFiscales().getTipo());
                dto.setClaveFiscal(empresa.getDatosFiscales().getClaveFiscal());
                dto.setTipoIva(empresa.getDatosFiscales().getTipoIva());
                dto.setTipoGanancia(empresa.getDatosFiscales().getTipoGanancia());
                dto.setDebeCompletarFiscalExterior(true);
            }
        }

        return ResponseEntity.ok(dto);
    }

    // ========== DATOS REGISTRO ==========

    @GetMapping("/datos-registro/{solicitudId}")
    @Operation(summary = "Obtener Datos de Registro", description = "Obtiene los datos de registro de la empresa")
    public ResponseEntity<EmpresaDatosRegistroDTO> obtenerDatosRegistro(@PathVariable Long solicitudId) {
        log.info("Obteniendo datos de registro de empresa para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<EmpresaDatosRegistroDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        Empresa empresa = (Empresa) solicitud.getTitular();

        if (empresa == null) {
            empresa = new Empresa();
        }

        EmpresaDatosRegistroDTO dto = new EmpresaDatosRegistroDTO();
        dto.setLugarInscripcionRegistro(empresa.getLugarInscripcionRegistro());
        dto.setNumeroRegistro(empresa.getNumeroRegistro());
        dto.setPaisRegistro(empresa.getPaisRegistro());
        dto.setProvinciaRegistro(empresa.getProvinciaRegistro());
        dto.setLugarRegistro(empresa.getLugarRegistro());
        dto.setFechaRegistro(empresa.getFechaRegistro());
        dto.setFolio(empresa.getFolio());
        dto.setLibro(empresa.getLibro());
        dto.setTomo(empresa.getTomo());

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/datos-registro/{solicitudId}")
    @Operation(summary = "Actualizar Datos de Registro", description = "Actualiza los datos de registro de la empresa")
    public ResponseEntity<Void> actualizarDatosRegistro(
            @PathVariable Long solicitudId,
            @Parameter(description = "Datos de registro") @Valid @RequestBody EmpresaDatosRegistroDTO dto) {

        log.info("Actualizando datos de registro de empresa para solicitud: {}", solicitudId);

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

        if (solicitud.getTitular() != null && solicitud.getTitular() instanceof Empresa empresa) {
            empresa.setLugarInscripcionRegistro(dto.getLugarInscripcionRegistro());
            empresa.setNumeroRegistro(dto.getNumeroRegistro());
            empresa.setPaisRegistro(dto.getPaisRegistro());
            empresa.setProvinciaRegistro(dto.getProvinciaRegistro());
            empresa.setLugarRegistro(dto.getLugarRegistro());
            empresa.setFechaRegistro(dto.getFechaRegistro());
            empresa.setFolio(dto.getFolio());
            empresa.setLibro(dto.getLibro());
            empresa.setTomo(dto.getTomo());
        }

        solicitudService.actualizar(solicitud);

        return ResponseEntity.ok().build();
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
        if (solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO) {
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
    public ResponseEntity<Void> actualizarCuentasBancarias(
            @PathVariable Long solicitudId,
            @Parameter(description = "Cuentas bancarias") @Valid @RequestBody CuentasBancariasDTO dto) {

        log.info("Actualizando cuentas bancarias de individuo para solicitud: {}", solicitudId);

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

        solicitudService.actualizar(solicitud);
        return ResponseEntity.ok().build();
    }

    // ========== DECLARACIONES ==========

    @GetMapping("/declaraciones/{solicitudId}")
    @Operation(summary = "Obtener Declaraciones", description = "Obtiene las declaraciones de la empresa")
    public ResponseEntity<EmpresaDeclaracionesDTO> obtenerDeclaraciones(@PathVariable Long solicitudId) {
        log.info("Obteniendo declaraciones de empresa para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<EmpresaDeclaracionesDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        Empresa empresa = (Empresa) solicitud.getTitular();

        if (empresa == null) {
            empresa = new Empresa();
        }

        EmpresaDeclaracionesDTO dto = new EmpresaDeclaracionesDTO();
        dto.setDeclaraUIF(empresa.getDeclaraUIF() == null ? false : empresa.getDeclaraUIF());
        dto.setMotivoUIF(empresa.getMotivoUIF() == null ? "" : empresa.getMotivoUIF());
        dto.setEsFATCA(empresa.getEsFATCA() == null ? false : empresa.getEsFATCA());
        dto.setMotivoFatca(empresa.getMotivoFatca() == null ? "" : empresa.getMotivoFatca());

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/declaraciones/{solicitudId}")
    @Operation(summary = "Actualizar Declaraciones", description = "Actualiza las declaraciones de la empresa")
    public ResponseEntity<Void> actualizarDeclaraciones(
            @PathVariable Long solicitudId,
            @Parameter(description = "Declaraciones") @Valid @RequestBody EmpresaDeclaracionesDTO dto) {

        log.info("Actualizando declaraciones de empresa para solicitud: {}", solicitudId);

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

        if (solicitud.getTitular() != null && solicitud.getTitular() instanceof Empresa empresa) {
            empresa.setDeclaraUIF(dto.isDeclaraUIF());
            empresa.setMotivoUIF(dto.getMotivoUIF());
            empresa.setEsFATCA(dto.isEsFATCA());
            empresa.setMotivoFatca(dto.getMotivoFatca());
        }

        solicitudService.actualizar(solicitud);
        return ResponseEntity.ok().build();
    }

    // ========== ACCIONISTAS ==========

    @PostMapping("/accionistas/{solicitudId}")
    @Operation(summary = "Agregar Accionista", description = "Agrega un accionista (Persona o Empresa) a la empresa")
    public ResponseEntity<AccionistaCreadoResponse> agregarAccionista(
            @PathVariable Long solicitudId,
            @Parameter(description = "Datos completos del accionista (Persona o Empresa)") @Valid @RequestBody AccionistaDetalleDTO dto) {
        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<AccionistaCreadoResponse>) validacion;
        }
        return guardarAccionista(solicitudId, null, dto);
    }

    @GetMapping("/accionistas/{solicitudId}")
    @Operation(summary = "Obtener Accionistas", description = "Obtiene todos los accionistas de una empresa")
    public ResponseEntity<List<AccionistaDetalleDTO>> obtenerAccionistas(@PathVariable Long solicitudId) {
        log.info("Obteniendo accionistas de empresa para solicitud: {}", solicitudId);
        
        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<List<AccionistaDetalleDTO>>) validacion;
        }
        
        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO) {
            return ResponseEntity.badRequest().build();
        }
        
        if (!(solicitud.getTitular() instanceof Empresa empresa)) {
            return ResponseEntity.badRequest().build();
        }

        List<Organizacion> accionistas = empresa.getAccionistas();
        if (accionistas == null || accionistas.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        
        List<AccionistaDetalleDTO> accionistasDTO = accionistas.stream()
                .map(this::convertirOrganizacionADTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(accionistasDTO);
    }

    @GetMapping("/accionistas/{solicitudId}/{dniNumero}")
    @Operation(summary = "Obtener Accionistas por DNI/CUIT", description = "Filtra los accionistas por número de documento")
    public ResponseEntity<List<AccionistaDetalleDTO>> obtenerAccionistasPorDni(
            @PathVariable Long solicitudId,
            @PathVariable String dniNumero) {
        log.info("Obteniendo accionistas por DNI/CUIT {} de empresa para solicitud: {}", dniNumero, solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<List<AccionistaDetalleDTO>>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        if (!(solicitud.getTitular() instanceof Empresa empresa)) {
            return ResponseEntity.badRequest().build();
        }

        List<Organizacion> accionistas = empresa.getAccionistas();
        List<AccionistaDetalleDTO> filtrados = accionistas != null
                ? accionistas.stream()
                .filter(a -> {
                    // Si es Persona, buscar por idNumero
                    if (a instanceof Persona persona) {
                        return Objects.equals(persona.getIdNumero(), dniNumero);
                    }
                    // Si es Empresa, buscar por CUIT en datos fiscales
                    if (a instanceof Empresa empresaAccionista) {
                        DatosFiscales datosFiscales = empresaAccionista.getDatosFiscales();
                        if (datosFiscales != null && datosFiscales.getClaveFiscal() != null) {
                            return Objects.equals(datosFiscales.getClaveFiscal(), dniNumero);
                        }
                    }
                    return false;
                })
                .map(this::convertirOrganizacionADTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                : new ArrayList<>();

        return ResponseEntity.ok(filtrados);
    }

    @PutMapping("/accionistas/{solicitudId}/{accionistaId}")
    @Operation(summary = "Actualizar Accionista", description = "Actualiza los datos de un accionista (Persona o Empresa)")
    public ResponseEntity<AccionistaCreadoResponse> actualizarAccionista(
            @PathVariable Long solicitudId,
            @PathVariable Long accionistaId,
            @Parameter(description = "Datos completos del accionista (Persona o Empresa)") @Valid @RequestBody AccionistaDetalleDTO dto) {

        if (dto.getId() != null && !Objects.equals(dto.getId(), accionistaId)) {
            return ResponseEntity.badRequest().build();
        }

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<AccionistaCreadoResponse>) validacion;
        }

        return guardarAccionista(solicitudId, accionistaId, dto);
    }

    @DeleteMapping("/accionistas/{solicitudId}/{accionistaId}")
    @Operation(summary = "Eliminar Accionista", description = "Elimina un accionista de la empresa")
    public ResponseEntity<Void> eliminarAccionista(
            @PathVariable Long solicitudId,
            @PathVariable Long accionistaId) {
        
        log.info("Eliminando accionista {} de empresa para solicitud: {}", accionistaId, solicitudId);
        
        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Void>) validacion;
        }
        
        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Solicitud solicitud = solicitudOpt.get();
        if (!(solicitud.getTitular() instanceof Empresa empresa)) {
            return ResponseEntity.badRequest().build();
        }

        List<Organizacion> accionistas = empresa.getAccionistas();
        if (accionistas != null) {
            accionistas.removeIf(a -> Objects.equals(a.getId(), accionistaId));
        }

        solicitudService.actualizar(solicitud);
        return ResponseEntity.ok().build();
    }

    // ========== DOCUMENTACIÓN RESPALDATORIA ==========

    @GetMapping("/documentacion-respaldatoria/{solicitudId}")
    @Operation(summary = "Obtener Documentación Respaldatoria", description = "Obtiene la documentación respaldatoria de la empresa")
    public ResponseEntity<EmpresaDocumentacionRespaldatoriaDTO> obtenerDocumentacionRespaldatoria(
            @PathVariable Long solicitudId) {
        log.info("Obteniendo documentación respaldatoria de empresa para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<EmpresaDocumentacionRespaldatoriaDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        Empresa empresa = (Empresa) solicitud.getTitular();

        if (empresa == null) {
            empresa = new Empresa();
        }

        EmpresaDocumentacionRespaldatoriaDTO dto = new EmpresaDocumentacionRespaldatoriaDTO();
        dto.setEstatutoId(empresa.getEstatuto() != null ? empresa.getEstatuto().getId() : null);
        dto.setBalanceId(empresa.getBalance() != null ? empresa.getBalance().getId() : null);
        dto.setAccionistaId(empresa.getAccionista() != null ? empresa.getAccionista().getId() : null);
        dto.setPoderActaId(empresa.getPoderActa() != null ? empresa.getPoderActa().getId() : null);
        dto.setPoderId(empresa.getPoder() != null ? empresa.getPoder().getId() : null);
        dto.setDdjjGananciasId(empresa.getDdjjGanancias() != null ? empresa.getDdjjGanancias().getId() : null);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/documentacion-respaldatoria/{solicitudId}")
    @Operation(summary = "Actualizar Documentación Respaldatoria", description = "Actualiza la documentación respaldatoria de la empresa")
    public ResponseEntity<Void> actualizarDocumentacionRespaldatoria(
            @PathVariable Long solicitudId,
            @Parameter(description = "Documentación respaldatoria") @Valid @RequestBody EmpresaDocumentacionRespaldatoriaDTO dto) {

        log.info("Actualizando documentación respaldatoria de empresa para solicitud: {}", solicitudId);

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

        if (solicitud.getTitular() != null && solicitud.getTitular() instanceof Empresa empresa) {
            empresa.setEstatuto(archivoRepository.findById(dto.getEstatutoId()).orElse(null));
            empresa.setBalance(archivoRepository.findById(dto.getBalanceId()).orElse(null));
            empresa.setAccionista(archivoRepository.findById(dto.getAccionistaId()).orElse(null));
            empresa.setPoderActa(archivoRepository.findById(dto.getPoderActaId()).orElse(null));
            empresa.setPoder(archivoRepository.findById(dto.getPoderId()).orElse(null));
            empresa.setDdjjGanancias(archivoRepository.findById(dto.getDdjjGananciasId()).orElse(null));
        }

        solicitudService.actualizar(solicitud);

        return ResponseEntity.ok().build();
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
        if (solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO) {
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
        if (solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO) {
            return ResponseEntity.badRequest().build();
        }

        CuentasBancariasDTO dto = new CuentasBancariasDTO();
        dto.setDebeCompletarCuentasBancariasExterior(false);

        if (solicitud.getTitular() != null &&
                solicitud.getTitular() instanceof Empresa empresa &&
                empresa.getDomicilio().getPais() != null &&
                !"Argentina".equalsIgnoreCase(empresa.getDomicilio().getPais())) {
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
                solicitud.getTitular() instanceof Empresa empresa &&
                empresa.getPerfilInversor() != null &&
                empresa.getPerfilInversor().getRespuestas() != null) {

            respuestasDTO = empresa.getPerfilInversor().getRespuestas().stream().map(respuesta -> {
                PerfilInversorRespuestaDTO respuestaDTO = new PerfilInversorRespuestaDTO();
                respuestaDTO.setPreguntaId(respuesta.getPerfilInversorPregunta().getId());
                respuestaDTO.setOpcionId(respuesta.getPerfilInversorPreguntaOpcion().getId());
                return respuestaDTO;
            }).collect(Collectors.toList());

            if (empresa.getPerfilInversor().getTipo() != null) {
                dto.setTipoPerfil(empresa.getPerfilInversor().getTipo().getDescripcion());
            }
        }

        dto.setRespuestas(respuestasDTO);

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/perfil-inversor/{solicitudId}")
    @Operation(summary = "Actualizar Perfil del Inversor", description = "Actualiza el perfil de inversor del individuo")
    public ResponseEntity<Void> actualizarPerfilInversor(
            @PathVariable Long solicitudId,
            @Parameter(description = "Perfil de inversor") @Valid @RequestBody IndividuoPerfilInversorDTO dto) {

        log.info("Actualizando perfil de inversor de individuo para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Void>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();

        if (solicitud.getTitular() != null && solicitud.getTitular() instanceof Empresa empresa) {

            // Crear o obtener el perfil inversor de la persona
            ar.com.st.entity.PerfilInversor perfilInversor = empresa.getPerfilInversor();
            if (perfilInversor == null) {
                perfilInversor = new ar.com.st.entity.PerfilInversor();
                empresa.setPerfilInversor(perfilInversor);
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

        solicitudService.actualizar(solicitud);
        return ResponseEntity.ok().build();
    }

    // ========== TÉRMINOS Y CONDICIONES ==========

    @GetMapping("/terminos-condiciones/{solicitudId}")
    @Operation(summary = "Obtener Términos y Condiciones", description = "Obtiene los términos y condiciones de la solicitud")
    public ResponseEntity<TerminosCondicionesDTO> obtenerTerminosCondiciones(@PathVariable Long solicitudId) {
        log.info("Obteniendo términos y condiciones de empresa para solicitud: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<TerminosCondicionesDTO>) validacion;
        }

        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();

        if (solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO) {
            return ResponseEntity.badRequest().build();
        }

        TerminosCondicionesDTO dto = new TerminosCondicionesDTO();
        dto.setAceptaTerminosCondiciones(solicitud.isAceptaTerminosCondiciones());
        dto.setAceptaReglamentoGestionFondos(solicitud.isAceptaReglamentoGestionFondos());
        dto.setAceptaComisiones(solicitud.isAceptaComisiones());

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/terminos-condiciones/{solicitudId}")
    @Operation(summary = "Actualizar Términos y Condiciones", description = "Finaliza la solicitud con términos y condiciones")
    public ResponseEntity<Void> actualizarTerminosCondiciones(
            @PathVariable Long solicitudId,
            @Parameter(description = "Términos y condiciones") @Valid @RequestBody TerminosCondicionesDTO dto) {

        log.info("Finalizando solicitud de empresa con términos y condiciones: {}", solicitudId);

        ResponseEntity<?> validacion = validarAccesoCliente(solicitudId);
        if (validacion != null) {
            return (ResponseEntity<Void>) validacion;
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
        solicitudService.finalizarCarga(solicitudId);
        return ResponseEntity.ok().build();
    }

    // ========== MÉTODOS PRIVADOS PARA ACCIONISTAS ==========

    private ResponseEntity<AccionistaCreadoResponse> guardarAccionista(Long solicitudId, Long accionistaId, AccionistaDetalleDTO dto) {
        Optional<Solicitud> solicitudOpt = solicitudService.obtenerPorId(solicitudId);
        if (solicitudOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Solicitud solicitud = solicitudOpt.get();
        if (solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO || !(solicitud.getTitular() instanceof Empresa empresa)) {
            return ResponseEntity.badRequest().build();
        }

        // Validar que el tipo esté presente y sea válido
        if (dto.getTipo() == null || (!"PERSONA".equalsIgnoreCase(dto.getTipo()) && !"EMPRESA".equalsIgnoreCase(dto.getTipo()))) {
            return ResponseEntity.badRequest().build();
        }

        List<Organizacion> accionistas = empresa.getAccionistas();
        if (accionistas == null) {
            accionistas = new ArrayList<>();
            empresa.setAccionistas(accionistas);
        }

        Organizacion accionista = null;
        if (accionistaId != null) {
            accionista = accionistas.stream()
                    .filter(a -> Objects.equals(a.getId(), accionistaId))
                    .findFirst()
                    .orElse(null);
            if (accionista == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Validar que el tipo del DTO coincida con el tipo existente
            if ("PERSONA".equalsIgnoreCase(dto.getTipo()) && !(accionista instanceof Persona)) {
                return ResponseEntity.badRequest().build();
            }
            if ("EMPRESA".equalsIgnoreCase(dto.getTipo()) && !(accionista instanceof Empresa)) {
                return ResponseEntity.badRequest().build();
            }
        } else if (dto.getId() != null) {
            accionista = accionistas.stream()
                    .filter(a -> Objects.equals(a.getId(), dto.getId()))
                    .findFirst()
                    .orElse(null);
        }

        // Crear o actualizar según el tipo
        boolean esNuevo = accionista == null;
        if (accionista == null) {
            if ("PERSONA".equalsIgnoreCase(dto.getTipo())) {
                if (dto.getDatosPersona() == null) {
                    return ResponseEntity.badRequest().build();
                }
                Persona personaAccionista = crearPersonaBase();
                personaAccionista.setTipo(Persona.Tipo.ACCIONISTA);
                accionista = personaAccionista;
                accionistas.add(accionista);
                aplicarDatosPersona((Persona) accionista, dto.getDatosPersona());
            } else if ("EMPRESA".equalsIgnoreCase(dto.getTipo())) {
                Empresa empresaAccionista = crearEmpresaBase();
                accionista = empresaAccionista;
                accionistas.add(accionista);
                aplicarDatosEmpresa((Empresa) accionista, dto);
            }
        } else {
            // Actualizar existente
            if ("PERSONA".equalsIgnoreCase(dto.getTipo())) {
                if (dto.getDatosPersona() == null) {
                    return ResponseEntity.badRequest().build();
                }
                Persona personaAccionista = (Persona) accionista;
                if (personaAccionista.getTipo() == null) {
                    personaAccionista.setTipo(Persona.Tipo.ACCIONISTA);
                }
                aplicarDatosPersona(personaAccionista, dto.getDatosPersona());
            } else if ("EMPRESA".equalsIgnoreCase(dto.getTipo())) {
                aplicarDatosEmpresa((Empresa) accionista, dto);
            }
        }

        // Guardar la posición del accionista en la lista antes de guardar (si es nuevo)
        int indiceAccionista = -1;
        if (esNuevo) {
            indiceAccionista = accionistas.indexOf(accionista);
        }
        
        solicitudService.actualizar(solicitud);
        
        // Si es un nuevo accionista, devolver su ID. Si es actualización, devolver el ID existente.
        if (accionista == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Long idAccionista = accionista.getId();
        
        // Si es nuevo y el ID aún no está disponible, buscar el accionista en la solicitud refrescada
        if (esNuevo && idAccionista == null) {
            // Refrescar la solicitud desde el repositorio para obtener los IDs generados
            Optional<Solicitud> solicitudRefrescadaOpt = solicitudService.obtenerPorId(solicitudId);
            if (solicitudRefrescadaOpt.isPresent()) {
                Solicitud solicitudRefrescada = solicitudRefrescadaOpt.get();
                Empresa empresaRefrescada = (Empresa) solicitudRefrescada.getTitular();
                List<Organizacion> accionistasRefrescados = empresaRefrescada.getAccionistas();
                if (accionistasRefrescados != null && !accionistasRefrescados.isEmpty() && indiceAccionista >= 0 && indiceAccionista < accionistasRefrescados.size()) {
                    // Obtener el accionista por su posición en la lista
                    accionista = accionistasRefrescados.get(indiceAccionista);
                    idAccionista = accionista.getId();
                } else if (accionistasRefrescados != null && !accionistasRefrescados.isEmpty()) {
                    // Si no se pudo obtener por índice, tomar el último agregado
                    accionista = accionistasRefrescados.get(accionistasRefrescados.size() - 1);
                    idAccionista = accionista.getId();
                }
            }
        }
        
        AccionistaCreadoResponse response = new AccionistaCreadoResponse();
        response.setId(idAccionista);
        response.setTipo(dto.getTipo());
        response.setCreado(esNuevo);
        
        return ResponseEntity.ok(response);
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
        actualizarDatosPrincipalesPersona(persona, dto.getDatosPrincipales());
        actualizarDatosPersonalesPersona(persona, dto.getDatosPersonales());
        actualizarDeclaracionesPersona(persona, dto.getDeclaraciones());
        actualizarDatosFiscalesPersona(persona, dto.getDatosFiscales());
        actualizarDomicilioPersona(persona, dto.getDomicilio());
    }

    private void actualizarDatosPrincipalesPersona(Persona persona, IndividuoDatosPrincipalesDTO dto) {
        if (dto == null) {
            return;
        }
        persona.setNombres(dto.getNombres());
        persona.setApellidos(dto.getApellidos());
        persona.setCelular(dto.getCelular());
        persona.setCorreoElectronico(dto.getCorreoElectronico());
        persona.setPorcentaje(dto.getPorcentajeParticipacion());
    }

    private void actualizarDatosPersonalesPersona(Persona persona, IndividuoDatosPersonalesDTO dto) {
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

    private void actualizarDeclaracionesPersona(Persona persona, IndividuoDeclaracionesDTO dto) {
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

    private void actualizarDatosFiscalesPersona(Persona persona, DatosFiscalesDTO dto) {
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

    private void actualizarDomicilioPersona(Persona persona, DomicilioDTO dto) {
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

    private Empresa crearEmpresaBase() {
        Empresa empresa = new Empresa();
        empresa.setTipo(Empresa.Tipo.ACCIONISTA); // Por defecto es accionista cuando se crea desde aquí
        
        if (empresa.getDomicilio() == null) {
            empresa.setDomicilio(new Domicilio());
        }
        if (empresa.getDatosFiscales() == null) {
            empresa.setDatosFiscales(new DatosFiscales());
        }
        if (empresa.getDatosFiscalesExterior() == null) {
            empresa.setDatosFiscalesExterior(new DatosFiscales());
        }
        if (empresa.getPerfilInversor() == null) {
            PerfilInversor perfilInversor = new PerfilInversor();
            perfilInversor.setRespuestas(new ArrayList<>());
            empresa.setPerfilInversor(perfilInversor);
        } else if (empresa.getPerfilInversor().getRespuestas() == null) {
            empresa.getPerfilInversor().setRespuestas(new ArrayList<>());
        }
        return empresa;
    }

    private void aplicarDatosEmpresa(Empresa empresa, AccionistaDetalleDTO dto) {
        if (dto.getDatosEmpresa() == null) {
            return;
        }
        
        EmpresaAccionistaDetalleDTO datosEmpresa = dto.getDatosEmpresa();
        
        if (datosEmpresa.getDatosPrincipales() != null) {
            actualizarDatosPrincipalesEmpresa(empresa, datosEmpresa.getDatosPrincipales());
        }
        if (datosEmpresa.getDatosFiscales() != null) {
            actualizarDatosFiscalesComun(empresa, datosEmpresa.getDatosFiscales());
        }
        
        // Manejar accionistas anidados (si la empresa accionista tiene sus propios accionistas)
        List<Organizacion> accionistasEmpresa = empresa.getAccionistas();
        if (accionistasEmpresa == null) {
            accionistasEmpresa = new ArrayList<>();
            empresa.setAccionistas(accionistasEmpresa);
        }
        
        if (datosEmpresa.getAccionistas() != null && !datosEmpresa.getAccionistas().isEmpty()) {
            // Obtener los IDs de los accionistas en el DTO
            Set<Long> idsAccionistasDTO = datosEmpresa.getAccionistas().stream()
                    .map(AccionistaDetalleDTO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            
            // Eliminar los accionistas que ya no están en el DTO
            accionistasEmpresa.removeIf(a -> a.getId() != null && !idsAccionistasDTO.contains(a.getId()));
            
            // Procesar cada accionista anidado (agregar o actualizar)
            for (AccionistaDetalleDTO accionistaDTO : datosEmpresa.getAccionistas()) {
                guardarAccionistaAnidado(accionistasEmpresa, accionistaDTO);
            }
        } else {
            // Si no hay accionistas en el DTO, eliminar todos los existentes
            accionistasEmpresa.clear();
        }
    }
    
    private void guardarAccionistaAnidado(List<Organizacion> accionistas, AccionistaDetalleDTO dto) {
        if (dto.getTipo() == null || (!"PERSONA".equalsIgnoreCase(dto.getTipo()) && !"EMPRESA".equalsIgnoreCase(dto.getTipo()))) {
            return;
        }
        
        Organizacion accionista = null;
        if (dto.getId() != null) {
            accionista = accionistas.stream()
                    .filter(a -> Objects.equals(a.getId(), dto.getId()))
                    .findFirst()
                    .orElse(null);
        }
        
        if (accionista == null) {
            // Crear nuevo accionista
            if ("PERSONA".equalsIgnoreCase(dto.getTipo())) {
                if (dto.getDatosPersona() == null) {
                    return;
                }
                Persona personaAccionista = crearPersonaBase();
                personaAccionista.setTipo(Persona.Tipo.ACCIONISTA);
                accionista = personaAccionista;
                accionistas.add(accionista);
                aplicarDatosPersona(personaAccionista, dto.getDatosPersona());
            } else if ("EMPRESA".equalsIgnoreCase(dto.getTipo())) {
                Empresa empresaAccionista = crearEmpresaBase();
                accionista = empresaAccionista;
                accionistas.add(accionista);
                aplicarDatosEmpresa(empresaAccionista, dto);
            }
        } else {
            // Actualizar existente
            if ("PERSONA".equalsIgnoreCase(dto.getTipo()) && accionista instanceof Persona) {
                if (dto.getDatosPersona() == null) {
                    return;
                }
                Persona personaAccionista = (Persona) accionista;
                if (personaAccionista.getTipo() == null) {
                    personaAccionista.setTipo(Persona.Tipo.ACCIONISTA);
                }
                aplicarDatosPersona(personaAccionista, dto.getDatosPersona());
            } else if ("EMPRESA".equalsIgnoreCase(dto.getTipo()) && accionista instanceof Empresa) {
                aplicarDatosEmpresa((Empresa) accionista, dto);
            }
        }
    }

    private void actualizarDatosPrincipalesEmpresa(Empresa empresa, EmpresaDatosPrincipalesDTO dto) {
        if (dto == null) {
            return;
        }
        empresa.setDenominacion(dto.getDenominacion());
        empresa.setTipoEmpresa(dto.getTipoEmpresa());
        empresa.setTelefono(dto.getTelefono());
        empresa.setCelular(dto.getCelular());
        empresa.setCorreoElectronico(dto.getCorreoElectronico());
        empresa.setUsoFirma(dto.getUsoFirma());
        empresa.setActividad(dto.getActividad());
        empresa.setPorcentaje(dto.getPorcentajeParticipacion());
    }

    private void actualizarDatosFiscalesComun(Organizacion organizacion, DatosFiscalesDTO dto) {
        if (dto == null || organizacion == null) {
            return;
        }
        if (organizacion.getDatosFiscales() == null) {
            organizacion.setDatosFiscales(new DatosFiscales());
        }
        DatosFiscales datosFiscales = organizacion.getDatosFiscales();
        datosFiscales.setTipo(dto.getTipo());
        datosFiscales.setClaveFiscal(dto.getClaveFiscal());
        datosFiscales.setTipoIva(dto.getTipoIva());
        datosFiscales.setTipoGanancia(dto.getTipoGanancia());
        datosFiscales.setResidenciaFiscal(dto.getResidenciaFiscal());

        // Para Persona, manejar datos fiscales exterior si aplica
        if (organizacion instanceof Persona persona) {
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
    }

    // ========== MÉTODOS DE CONVERSIÓN A DTO ==========

    private AccionistaDetalleDTO convertirOrganizacionADTO(Organizacion organizacion) {
        if (organizacion == null) {
            return null;
        }

        AccionistaDetalleDTO dto = new AccionistaDetalleDTO();
        dto.setId(organizacion.getId());

        if (organizacion instanceof Persona persona) {
            dto.setTipo("PERSONA");
            dto.setDatosPersona(convertirPersonaADTO(persona));
        } else if (organizacion instanceof Empresa empresa) {
            dto.setTipo("EMPRESA");
            dto.setDatosEmpresa(convertirEmpresaADTO(empresa));
        } else {
            return null;
        }

        return dto;
    }

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
        datosPrincipales.setPorcentajeParticipacion(persona.getPorcentaje());
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

    private EmpresaAccionistaDetalleDTO convertirEmpresaADTO(Empresa empresa) {
        if (empresa == null) {
            return null;
        }

        EmpresaAccionistaDetalleDTO dto = new EmpresaAccionistaDetalleDTO();
        dto.setId(empresa.getId());

        // Datos principales
        EmpresaDatosPrincipalesDTO datosPrincipales = new EmpresaDatosPrincipalesDTO();
        datosPrincipales.setDenominacion(empresa.getDenominacion());
        datosPrincipales.setTipoEmpresa(empresa.getTipoEmpresa());
        datosPrincipales.setTelefono(empresa.getTelefono());
        datosPrincipales.setCelular(empresa.getCelular());
        datosPrincipales.setCorreoElectronico(empresa.getCorreoElectronico());
        datosPrincipales.setUsoFirma(empresa.getUsoFirma());
        datosPrincipales.setActividad(empresa.getActividad());
        datosPrincipales.setPorcentajeParticipacion(empresa.getPorcentaje());
        dto.setDatosPrincipales(datosPrincipales);

        // Datos fiscales
        if (empresa.getDatosFiscales() != null) {
            DatosFiscalesDTO datosFiscales = new DatosFiscalesDTO();
            datosFiscales.setTipo(empresa.getDatosFiscales().getTipo());
            datosFiscales.setClaveFiscal(empresa.getDatosFiscales().getClaveFiscal());
            datosFiscales.setTipoIva(empresa.getDatosFiscales().getTipoIva());
            datosFiscales.setTipoGanancia(empresa.getDatosFiscales().getTipoGanancia());
            datosFiscales.setResidenciaFiscal(empresa.getDatosFiscales().getResidenciaFiscal());
            dto.setDatosFiscales(datosFiscales);
        }

        // Accionistas anidados (recursivo)
        if (empresa.getAccionistas() != null && !empresa.getAccionistas().isEmpty()) {
            List<AccionistaDetalleDTO> accionistasDTO = empresa.getAccionistas().stream()
                    .map(this::convertirOrganizacionADTO)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            dto.setAccionistas(accionistasDTO);
        }

        return dto;
    }
}
