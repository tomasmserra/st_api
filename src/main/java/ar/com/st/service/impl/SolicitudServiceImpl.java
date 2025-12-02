package ar.com.st.service.impl;

import ar.com.st.dto.aunesa.AltaCuentaResultado;
import ar.com.st.dto.firmaDigital.EstadoDocumentoDTO;
import ar.com.st.dto.firmaDigital.EstadoFirmaDTO;
import ar.com.st.dto.solicitud.SolicitudResumenDTO;
import ar.com.st.entity.*;
import ar.com.st.repository.*;
import ar.com.st.service.AunesaService;
import ar.com.st.service.AuditoriaService;
import ar.com.st.service.AuthService;
import ar.com.st.service.EmailService;
import ar.com.st.service.FirmaDigitalService;
import ar.com.st.service.SolicitudPdfService;
import ar.com.st.service.SolicitudService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación del servicio de solicitudes
 * 
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SolicitudServiceImpl implements SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudPdfService solicitudPdfService;
    private final EmailService emailService;
    private final AunesaService aunesaService;
    private final AuthService authService;
    private final FirmaDigitalService firmaDigitalService;
    private final AuditoriaService auditoriaService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Solicitud crearSolicitud(Solicitud.Tipo tipo, Long idUsuarioCargo) {
        log.info("Creando nueva solicitud de tipo: {} para usuario: {}", tipo, idUsuarioCargo);
        
        // Buscar el usuario por ID
        Usuario usuario = usuarioRepository.findById(idUsuarioCargo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuarioCargo));
        
        Solicitud solicitud = new Solicitud();
        solicitud.setEstado(Solicitud.Estado.INCOMPLETA);
        solicitud.setTipo(tipo);
        solicitud.setUsuarioCargo(usuario);
        solicitud.setFirmantes(new ArrayList<>());
        solicitud.setCuentasBancarias(new ArrayList<>());
        solicitud.setDdJjOrigenFondos(new ArrayList<>());
        solicitud.setFechaAlta(LocalDateTime.now());

        // Crear titular según el tipo
        if (tipo == Solicitud.Tipo.INDIVIDUO) {
            Persona titular = crearPersonaTitular();
            solicitud.setTitular(titular);
        } else if (tipo == Solicitud.Tipo.EMPRESA_CLASICA || tipo == Solicitud.Tipo.EMPRESA_SGR) {
            Empresa titular = crearEmpresaTitular();
            solicitud.setTitular(titular);
        }

        return solicitudRepository.save(solicitud);
    }

    @Override
    public Optional<Solicitud> obtenerPorId(Long id) {
        return solicitudRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Solicitud> obtenerTodas() {
        // Usar método que carga el titular con JOIN FETCH para evitar problemas con lazy loading
        List<Solicitud> solicitudes = solicitudRepository.findAllWithTitular();
        
        // Verificar el estado de firma digital para solicitudes pendientes de firma
        for (Solicitud solicitud : solicitudes) {
            if (solicitud.getEstado() == Solicitud.Estado.PENTIENTE_FIRMA 
                    && solicitud.getIdFirmaDigital() != null 
                    && !solicitud.getIdFirmaDigital().isEmpty()) {
                actualizarEstadoSegunFirmaDigital(solicitud.getId(), solicitud.getIdFirmaDigital());
            }
        }
        
        return solicitudes;
    }
    
    /**
     * Verifica el estado del documento de firma digital y actualiza el estado de la solicitud si es necesario
     * Este método usa su propia transacción para evitar conflictos con la transacción de solo lectura
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    private void actualizarEstadoSegunFirmaDigital(Long solicitudId, String idFirmaDigital) {
        try {
            // Re-obtener la solicitud en la nueva transacción
            Solicitud solicitud = solicitudRepository.findById(solicitudId)
                    .orElse(null);
            
            if (solicitud == null) {
                return;
            }
            
            EstadoDocumentoDTO estadoDocumento = firmaDigitalService.obtenerEstadoDocumento(idFirmaDigital);
            
            if (estadoDocumento == null) {
                log.warn("No se pudo obtener el estado del documento de firma digital {} para la solicitud {}", 
                        idFirmaDigital, solicitudId);
                return;
            }
            
            // Si el documento está cancelado, actualizar la solicitud
            if (estadoDocumento.getEstado() == EstadoFirmaDTO.Estado.CANCELADO) {
                if (solicitud.getEstado() != Solicitud.Estado.CANCELADA) {
                    solicitud.setEstado(Solicitud.Estado.CANCELADA);
                    solicitudRepository.save(solicitud);
                    log.info("Solicitud {} actualizada a CANCELADA debido a cancelación de firma digital", 
                            solicitudId);
                }
                return;
            }
            
            // Si el documento está completo (todas las firmas completadas)
            if (estadoDocumento.getEstado() == EstadoFirmaDTO.Estado.COMPLETO) {
                if (solicitud.getEstado() != Solicitud.Estado.FIRMADO) {
                    solicitud.setEstado(Solicitud.Estado.FIRMADO);
                    solicitudRepository.save(solicitud);
                    log.info("Solicitud {} actualizada a FIRMADO - todas las firmas completadas", 
                            solicitudId);
                }
                return;
            }
            
            // Verificar si todas las firmas individuales están completas
            if (estadoDocumento.getFirmas() != null && !estadoDocumento.getFirmas().isEmpty()) {
                boolean todasFirmasCompletas = estadoDocumento.getFirmas().stream()
                        .allMatch(firma -> firma.getEstado() == EstadoFirmaDTO.Estado.COMPLETO);
                
                if (todasFirmasCompletas && solicitud.getEstado() != Solicitud.Estado.FIRMADO) {
                    solicitud.setEstado(Solicitud.Estado.FIRMADO);
                    solicitudRepository.save(solicitud);
                    log.info("Solicitud {} actualizada a FIRMADO - todas las firmas individuales completadas", 
                            solicitudId);
                }
            }
            
        } catch (Exception ex) {
            log.error("Error al verificar el estado de firma digital para la solicitud {}: {}", 
                    solicitudId, ex.getMessage(), ex);
            // No lanzar excepción para no interrumpir la obtención de solicitudes
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Solicitud> obtenerTodas(Pageable pageable) {
        Page<Solicitud> solicitudes = solicitudRepository.findAll(pageable);
        
        // Verificar el estado de firma digital para solicitudes pendientes de firma
        solicitudes.getContent().forEach(solicitud -> {
            if (solicitud.getEstado() == Solicitud.Estado.PENTIENTE_FIRMA 
                    && solicitud.getIdFirmaDigital() != null 
                    && !solicitud.getIdFirmaDigital().isEmpty()) {
                actualizarEstadoSegunFirmaDigital(solicitud.getId(), solicitud.getIdFirmaDigital());
            }
        });
        
        return solicitudes;
    }

    @Override
    public List<Solicitud> buscar(String filtro, Collection<Solicitud.Tipo> tipos,
            Collection<Solicitud.Estado> estados) {
        return solicitudRepository.buscarConFiltros(filtro, tipos, estados);
    }

    @Override
    public List<Solicitud> obtenerPorEstado(Solicitud.Estado estado) {
        return solicitudRepository.findByEstado(estado);
    }

    @Override
    public List<Solicitud> obtenerPorTipo(Solicitud.Tipo tipo) {
        return solicitudRepository.findByTipo(tipo);
    }

    @Override
    public List<Solicitud> obtenerPorUsuario(Long idUsuario) {
        return solicitudRepository.findByUsuarioCargoId(idUsuario);
    }

    @Override
    public Optional<Solicitud> obtenerActivaPorUsuario(Long idUsuario) {
        return solicitudRepository.findActivaByUsuario(idUsuario);
    }

    @Override
    @Transactional
    public Solicitud guardar(Solicitud solicitud) {
        log.info("Guardando solicitud con ID: {}", solicitud.getId());
        return solicitudRepository.save(solicitud);
    }

    @Override
    @Transactional
    public Solicitud actualizar(Solicitud solicitud) {
        log.info("Actualizando solicitud con ID: {}", solicitud.getId());
        return solicitudRepository.save(solicitud);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando solicitud con ID: {}", id);
        solicitudRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Solicitud cancelarSolicitud(Long id) {
        log.info("Cancelando solicitud con ID: {}", id);
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + id));
        
        solicitud.setEstado(Solicitud.Estado.CANCELADA);
        return solicitudRepository.save(solicitud);
    }

    @Override
    @Transactional
    public Solicitud finalizarCarga(Long id) {
        log.info("Finalizando carga de solicitud con ID: {}", id);
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + id));
        
        solicitud.setEstado(Solicitud.Estado.PENDIENTE);
        return solicitudRepository.save(solicitud);
    }

    @Override
    @Transactional
    public Solicitud aprobarSolicitud(Long id, String motivo, Long idCuenta) {
        log.info("Aprobando solicitud con ID: {} con idCuenta: {}", id, idCuenta);
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + id));
        
        solicitud.setIdCuenta(idCuenta != null ? idCuenta.intValue() : null);        // Intentar dar de alta la cuenta en AUNESA
        AltaCuentaResultado resultado = aunesaService.altaCuentaComitente(solicitud);
        
        if (resultado.isExitoso()) {
            solicitud.setEstado(Solicitud.Estado.APROBADA);
            solicitud.setMotivoAprobadoSinFirma(motivo);
            solicitud.setFechaAprobo(LocalDateTime.now());
            
            // Asignar el usuario que aprobó
            try {
                org.springframework.security.core.Authentication authentication = 
                        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    Usuario usuario = authService.obtenerUsuarioPorUsername(authentication.getName())
                            .orElse(null);
                    if (usuario != null) {
                        solicitud.setUsuarioAprobo(usuario);
                    }
                }
            } catch (Exception e) {
                log.warn("No se pudo asignar el usuario que aprobó la solicitud: {}", e.getMessage());
            }
            
            // Registrar auditoría de aprobación exitosa
            auditoriaService.registrarAccionExitosa(
                    ar.com.st.entity.Auditoria.TipoAccion.APROBACION_CUENTA,
                    solicitud,
                    String.format("Cuenta aprobada exitosamente. ID Solicitud: %d, ID Cuenta: %d, Motivo: %s", 
                            solicitud.getId(), idCuenta != null ? idCuenta : "N/A", motivo != null ? motivo : "Sin motivo especificado")
            );
        } else {
            solicitud.setEstado(Solicitud.Estado.ERROR_DETECTADO);
            log.error("No fue posible dar de alta la cuenta en AUNESA: {}", resultado.getMensajeError());
            
            // Registrar auditoría de aprobación fallida
            String detallesError = resultado.getMensajeError() != null ? resultado.getMensajeError() : "Error desconocido al aprobar la cuenta";
            auditoriaService.registrarAccionFallida(
                    ar.com.st.entity.Auditoria.TipoAccion.APROBACION_CUENTA,
                    solicitud,
                    String.format("Error al aprobar cuenta. ID Solicitud: %d", solicitud.getId()),
                    detallesError
            );
        }

        return solicitudRepository.save(solicitud);
    }

    @Override
    @Transactional
    public Solicitud rechazarSolicitud(Long id, String motivo) {
        log.info("Rechazando solicitud con ID: {}", id);
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + id));
        
        solicitud.setEstado(Solicitud.Estado.RECHAZADA);
        solicitud.setMotivoAprobadoSinFirma(motivo);
        
        return solicitudRepository.save(solicitud);
    }

    @Override
    public Integer obtenerUltimoIdCuenta() {
        return solicitudRepository.obtenerUltimoIdCuenta();
    }

    @Override
    public byte[] generarPdf(Long id) {
        log.info("Generando PDF para solicitud con ID: {}", id);
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + id));

        return solicitudPdfService.generarPdf(solicitud);
    }

    @Override
    @Transactional(readOnly = false)
    public void enviarMailBienvenida(Long solicitudId) {
        try {
            Solicitud solicitud = solicitudRepository.findById(solicitudId)
                    .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + solicitudId));

            ClassPathResource contenido = new ClassPathResource("templates/mailBienvenida/mailBienvenida.html");
            String html = StreamUtils.copyToString(contenido.getInputStream(), StandardCharsets.UTF_8);
            
            boolean esEmpresa = solicitud.getTipo() == Solicitud.Tipo.EMPRESA_CLASICA || 
                               solicitud.getTipo() == Solicitud.Tipo.EMPRESA_SGR;
            boolean esFemenino = solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO && 
                                solicitud.getTitular() instanceof Persona persona && 
                                persona.getSexo() == Persona.Sexo.FEMENINO;
            
            Map<String, String> reemplazos = generarTexto(esEmpresa, esFemenino);

            for (Map.Entry<String, String> e : reemplazos.entrySet()) {
                html = html.replace(e.getKey(), e.getValue());
            }

            if (solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO && solicitud.getTitular() instanceof Persona persona) {
                html = html.replace("#{cliente}", String.format("%s %s", persona.getNombres(), persona.getApellidos()));
            } else if (solicitud.getTitular() instanceof Empresa empresa) {
                html = html.replace("#{cliente}", empresa.getDenominacion());
            }

            if (solicitud.getIdCuenta() != null) {
                html = html.replace("#{idNumero}", solicitud.getIdCuenta().toString());
            }

            ClassPathResource imgHeader = new ClassPathResource("templates/headerST.jpeg");
            byte[] bytes = imgHeader.getInputStream().readAllBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            html = html.replace("#{base64}", base64);

            ClassPathResource pdfInstrucciones = new ClassPathResource("templates/mailBienvenida/intruccionesGenerales.pdf");
            EmailService.AdjuntoEmail adjunto = new EmailService.AdjuntoEmail(
                    "instruccionesGenerales.pdf",
                    pdfInstrucciones
            );

            String emailUsuario = solicitud.getUsuarioCargo().getEmail();
            emailService.enviarEmailHtml(
                    Arrays.asList(emailUsuario),
                    "Bienvenido a ST Securities",
                    html,
                    Arrays.asList(adjunto)
            );

            log.info("Email de bienvenida enviado para la solicitud: {}", solicitudId);
            
            // Registrar auditoría de envío de correo exitoso
            auditoriaService.registrarAccionExitosa(
                    ar.com.st.entity.Auditoria.TipoAccion.ENVIO_CORREO,
                    solicitud,
                    String.format("Correo de bienvenida enviado exitosamente. ID Solicitud: %d, Destinatario: %s", 
                            solicitud.getId(), emailUsuario)
            );

        } catch (Exception ex) {
            log.error("No fue posible enviar el correo de bienvenida para la solicitud {}: {}", solicitudId, ex.getMessage(), ex);
            
            // Registrar auditoría de envío de correo fallido
            try {
                Solicitud solicitud = solicitudRepository.findById(solicitudId).orElse(null);
                if (solicitud != null) {
                    auditoriaService.registrarAccionFallida(
                            ar.com.st.entity.Auditoria.TipoAccion.ENVIO_CORREO,
                            solicitud,
                            String.format("Error al enviar correo de bienvenida. ID Solicitud: %d", solicitud.getId()),
                            ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()
                    );
                }
            } catch (Exception e) {
                log.warn("No se pudo registrar la auditoría del error: {}", e.getMessage());
            }
            
            throw new RuntimeException("Error enviando email de bienvenida", ex);
        }
    }

    @Override
    @Transactional(readOnly = false)
    public void enviarDatosAcceso(Long solicitudId, String usuario, String clave, String correosElectronicos) {
        try {
            Solicitud solicitud = solicitudRepository.findById(solicitudId)
                    .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con ID: " + solicitudId));

            ClassPathResource contenido = new ClassPathResource("templates/mailAccesoAunesa/mailAccesoAunesa.html");
            String html = StreamUtils.copyToString(contenido.getInputStream(), StandardCharsets.UTF_8);
            
            boolean esEmpresa = solicitud.getTipo() == Solicitud.Tipo.EMPRESA_CLASICA || 
                               solicitud.getTipo() == Solicitud.Tipo.EMPRESA_SGR;
            boolean esFemenino = solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO && 
                                solicitud.getTitular() instanceof Persona persona && 
                                persona.getSexo() == Persona.Sexo.FEMENINO;
            
            Map<String, String> reemplazos = generarTexto(esEmpresa, esFemenino);

            for (Map.Entry<String, String> e : reemplazos.entrySet()) {
                html = html.replace(e.getKey(), e.getValue());
            }

            if (solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO && solicitud.getTitular() instanceof Persona persona) {
                html = html.replace("#{cliente}", String.format("%s %s", persona.getNombres(), persona.getApellidos()));
            } else if (solicitud.getTitular() instanceof Empresa empresa) {
                html = html.replace("#{cliente}", empresa.getDenominacion());
            }

            html = html.replace("#{usuario}", usuario);
            html = html.replace("#{clave}", clave);

            ClassPathResource imgHeader = new ClassPathResource("templates/headerST.jpeg");
            byte[] bytes = imgHeader.getInputStream().readAllBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            html = html.replace("#{base64}", base64);

            List<String> emails = new ArrayList<>();
            emails.add(solicitud.getUsuarioCargo().getEmail());

            if (correosElectronicos != null && !correosElectronicos.trim().isEmpty()) {
                emails.addAll(Arrays.asList(correosElectronicos.split(",")));
            }

            emailService.enviarEmailHtml(
                    emails,
                    "Usuario en ST Securities",
                    html,
                    null
            );

            log.info("Email con datos de acceso enviado para la solicitud: {}", solicitudId);
            
            // Registrar auditoría de envío de correo exitoso
            auditoriaService.registrarAccionExitosa(
                    ar.com.st.entity.Auditoria.TipoAccion.ENVIO_CORREO,
                    solicitud,
                    String.format("Correo de datos de acceso enviado exitosamente. ID Solicitud: %d, Destinatarios: %s", 
                            solicitud.getId(), String.join(", ", emails))
            );

        } catch (Exception ex) {
            log.error("No fue posible enviar el correo con datos de acceso para la solicitud {}: {}", solicitudId, ex.getMessage(), ex);
            
            // Registrar auditoría de envío de correo fallido
            try {
                Solicitud solicitudError = solicitudRepository.findById(solicitudId).orElse(null);
                if (solicitudError != null) {
                    auditoriaService.registrarAccionFallida(
                            ar.com.st.entity.Auditoria.TipoAccion.ENVIO_CORREO,
                            solicitudError,
                            String.format("Error al enviar correo de datos de acceso. ID Solicitud: %d", solicitudError.getId()),
                            ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()
                    );
                }
            } catch (Exception e) {
                log.warn("No se pudo registrar la auditoría del error: {}", e.getMessage());
            }
            
            throw new RuntimeException("Error enviando email con datos de acceso", ex);
        }
    }

    private static Map<String, String> generarTexto(boolean plural, boolean femenino) {
        Map<String, String> t = new HashMap<>();

        // Saludo
        if (plural) {
            t.put("#{saludo}", femenino ? "Estimadas" : "Estimados");
            t.put("#{dar_bienvenida}", "darles la bienvenida");
            t.put("#{agradecer}", "agradecerles");
            t.put("#{orgulloso_forma}", femenino ? "orgullosas" : "orgullosos");
            t.put("#{posesivo_sus_tus}", "sus");
            t.put("#{asistir_pron}", femenino ? "las" : "los");
            t.put("#{posesivo_su_tu}", "su");
            t.put("#{imperativo_haga_clic}", "hagan clic");
            t.put("#{estar_recibiendo}", "estarán recibiendo");
            t.put("#{le_les}", "les");
        } else {
            t.put("#{saludo}", femenino ? "Estimada" : "Estimado");
            t.put("#{dar_bienvenida}", "darle la bienvenida");
            t.put("#{agradecer}", "agradecerle");
            t.put("#{orgulloso_forma}", femenino ? "orgullosa" : "orgulloso");
            t.put("#{posesivo_sus_tus}", "sus");
            t.put("#{asistir_pron}", femenino ? "la" : "lo");
            t.put("#{posesivo_su_tu}", "su");
            t.put("#{imperativo_haga_clic}", "haga clic");
            t.put("#{estar_recibiendo}", "estará recibiendo");
            t.put("#{le_les}", "le");
        }

        return t;
    }

    private Persona crearPersonaTitular() {
        Persona titular = new Persona();
        titular.setTipo(Persona.Tipo.TITULAR);
        
        // Crear domicilio
        Domicilio domicilio = new Domicilio();
        domicilio.setTipo(Domicilio.Tipo.LEGAL);
        titular.setDomicilio(domicilio);
        
        // Crear datos fiscales
        titular.setDatosFiscales(new DatosFiscales());
        
        // Crear perfil inversor
        PerfilInversor perfilInversor = new PerfilInversor();
        perfilInversor.setRespuestas(new ArrayList<>());
        titular.setPerfilInversor(perfilInversor);
        
        // Crear cónyuge
        titular.setConyuge(new Conyuge());
        
        return titular;
    }

    private Empresa crearEmpresaTitular() {
        Empresa titular = new Empresa();
        titular.setTipo(Empresa.Tipo.TITULAR);
        
        // Crear domicilio
        Domicilio domicilio = new Domicilio();
        domicilio.setTipo(Domicilio.Tipo.LEGAL);
        titular.setDomicilio(domicilio);
        
        // Crear datos fiscales
        titular.setDatosFiscales(new DatosFiscales());
        titular.setDatosFiscalesExterior(new DatosFiscales());
        
        // Crear perfil inversor
        PerfilInversor perfilInversor = new PerfilInversor();
        perfilInversor.setRespuestas(new ArrayList<>());
        titular.setPerfilInversor(perfilInversor);
        
        return titular;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudResumenDTO> obtenerTodasResumen() {
        List<Solicitud> solicitudes = obtenerTodas();
        return solicitudes.stream()
                .map(this::mapearSolicitudResumen)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudResumenDTO mapearSolicitudResumen(Solicitud solicitud) {
        SolicitudResumenDTO dto = new SolicitudResumenDTO();
        dto.setId(solicitud.getId());
        
        // Obtener ID y email del usuario que cargó la solicitud
        Long idUsuarioCargo = solicitud.getUsuarioCargo() != null
                ? solicitud.getUsuarioCargo().getId()
                : solicitud.getIdUsuarioCargo();
        dto.setIdUsuarioCargo(idUsuarioCargo);
        
        // Obtener email del usuario que cargó la solicitud
        String emailUsuarioCargo = null;
        if (idUsuarioCargo != null) {
            try {
                // Usar consulta JPQL directa para evitar problemas con lazy loading
                Query queryUsuario = entityManager.createQuery(
                    "SELECT u FROM Usuario u WHERE u.id = :id", Usuario.class);
                queryUsuario.setParameter("id", idUsuarioCargo);
                Usuario usuarioCargo = (Usuario) queryUsuario.getSingleResult();
                
                if (usuarioCargo != null) {
                    emailUsuarioCargo = usuarioCargo.getEmail();
                }
            } catch (jakarta.persistence.NoResultException e) {
                // El usuario no existe - simplemente no incluimos el email
                emailUsuarioCargo = null;
            } catch (Exception e) {
                // Cualquier otro error - loguear pero continuar
                log.warn("Error al acceder al usuario con ID {} para solicitud {}: {}", idUsuarioCargo, solicitud.getId(), e.getMessage());
                emailUsuarioCargo = null;
            }
        }
        dto.setEmailUsuarioCargo(emailUsuarioCargo);
        
        dto.setIdUsuarioAprobo(
                solicitud.getUsuarioAprobo() != null
                        ? solicitud.getUsuarioAprobo().getId()
                        : solicitud.getIdUsuarioAprobo()
        );
        dto.setFechaAprobo(solicitud.getFechaAprobo());
        dto.setFechaAlta(solicitud.getFechaAlta());
        dto.setFechaUltimaModificacion(solicitud.getFechaUltimaModificacion());
        dto.setEstado(solicitud.getEstado());
        dto.setIdCuenta(solicitud.getIdCuenta());
        dto.setTipo(solicitud.getTipo());
        dto.setIdFirmaDigital(solicitud.getIdFirmaDigital());
        
        // Manejar productor con validación de existencia
        // Usar el campo transiente primero para evitar inicializar el proxy lazy
        Long idProductor = solicitud.getIdProductor();
        dto.setIdProductor(idProductor);
        
        String nombreProductor = null;
        if (idProductor != null) {
            try {
                // Usar consulta JPQL directa para evitar problemas con JOINED inheritance y lazy loading
                Query query = entityManager.createQuery(
                    "SELECT p FROM Persona p WHERE p.id = :id", Persona.class);
                query.setParameter("id", idProductor);
                Persona productor = (Persona) query.getSingleResult();
                
                if (productor != null) {
                    String nombres = productor.getNombres() != null ? productor.getNombres() : "";
                    String apellidos = productor.getApellidos() != null ? productor.getApellidos() : "";
                    nombreProductor = (nombres + " " + apellidos).trim();
                    if (nombreProductor.isEmpty()) {
                        nombreProductor = null;
                    }
                }
            } catch (jakarta.persistence.NoResultException e) {
                // El productor no existe - simplemente no incluimos el nombre
                nombreProductor = null;
            } catch (Exception e) {
                // Cualquier otro error - loguear pero continuar
                log.warn("Error al acceder al productor con ID {} para solicitud {}: {}", idProductor, solicitud.getId(), e.getMessage());
                nombreProductor = null;
            }
        }
        dto.setProductor(nombreProductor);
        
        // Usar el método getNombre() de la entidad que maneja correctamente Persona y Empresa
        dto.setTitular(solicitud.getNombre());
        return dto;
    }
}
