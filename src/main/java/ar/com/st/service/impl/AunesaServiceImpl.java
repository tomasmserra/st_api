package ar.com.st.service.impl;

import ar.com.st.dto.aunesa.AltaCuentaResultado;
import ar.com.st.entity.*;
import ar.com.st.repository.SolicitudRepository;
import ar.com.st.service.AunesaService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio para comunicación con AUNESA
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AunesaServiceImpl implements AunesaService {

    private final RestTemplate restTemplate;
    private final SolicitudRepository solicitudRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${app.aunesa.login.url:}")
    private String loginUrl;
    
    @Value("${app.aunesa.login.client-id:}")
    private String clientId;
    
    @Value("${app.aunesa.login.username:}")
    private String username;
    
    @Value("${app.aunesa.login.password:}")
    private String password;
    
    @Value("${app.aunesa.cuenta.url:}")
    private String cuentaUrl;
    
    @Value("${app.aunesa.provincias.url:}")
    private String provinciasUrl;
    
    @Value("${app.aunesa.localidades.url:}")
    private String localidadesUrl;
    
    @Value("${app.aunesa.timeout:5000}")
    private int timeout;

    private String token;

    @Override
    public AltaCuentaResultado altaCuentaComitente(Solicitud solicitud) {
        AltaCuentaResultado resultado = new AltaCuentaResultado();
        resultado.setExitoso(false);

        Map<String, Object> body = null;
        try {
            body = construirJsonSolicitud(solicitud);
            Map<String, Object> respuesta = enviarSolicitud(body);
            
            if (respuesta != null && respuesta.containsKey("idCuenta")) {
                resultado.setExitoso(true);
                String idCuentaStr = String.valueOf(respuesta.get("idCuenta"));
                solicitud.setIdCuenta(Integer.parseInt(idCuentaStr));
                solicitudRepository.save(solicitud);
                log.info("Cuenta comitente creada exitosamente con ID: {}", idCuentaStr);
            } else {
                resultado.setMensajeError("No fue posible dar de alta la cuenta.");
            }
            
        } catch (HttpClientErrorException ex) {
            resultado = procesarErrorHttp(ex, solicitud, body);
        } catch (Exception ex) {
            log.error("Error al dar de alta la cuenta comitente en AUNESA: {}", ex.getMessage(), ex);
            if (body != null) {
                try {
                    String jsonBody = objectMapper.writeValueAsString(body);
                    log.error("JSON enviado a AUNESA (solicitud ID: {}): {}", solicitud.getId(), jsonBody);
                } catch (Exception e) {
                    log.error("Error al serializar JSON para logging: {}", e.getMessage());
                }
            }
            resultado.setMensajeError("No fue posible dar de alta la cuenta por un error inesperado: " + ex.getMessage());
        }

        return resultado;
    }

    private AltaCuentaResultado procesarErrorHttp(HttpClientErrorException ex, Solicitud solicitud, Map<String, Object> body) {
        AltaCuentaResultado resultado = new AltaCuentaResultado();
        resultado.setExitoso(false);

        int statusCode = ex.getStatusCode().value();
        
        // Loggear el JSON enviado cuando hay un error
        if (body != null) {
            try {
                String jsonBody = objectMapper.writeValueAsString(body);
                log.error("Error en alta de cuenta AUNESA (solicitud ID: {}). Status: {}. JSON enviado: {}", 
                        solicitud.getId(), statusCode, jsonBody);
            } catch (Exception e) {
                log.error("Error al serializar JSON para logging: {}", e.getMessage());
            }
        }
        
        if (statusCode == HttpStatus.CREATED.value() || statusCode == HttpStatus.OK.value()) {
            try {
                Map<String, Object> body201 = objectMapper.readValue(ex.getResponseBodyAsString(), 
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                Object idCuentaObj = body201.get("idCuenta");
                if (idCuentaObj != null) {
                    solicitud.setIdCuenta(Integer.parseInt(String.valueOf(idCuentaObj)));
                    solicitudRepository.save(solicitud);
                    resultado.setExitoso(true);
                    log.info("Cuenta comitente creada exitosamente con ID: {}", idCuentaObj);
                }
            } catch (Exception e) {
                log.error("Error al procesar respuesta exitosa: {}", e.getMessage(), e);
                resultado.setMensajeError("No fue posible obtener el ID de cuenta de la respuesta.");
            }
        } else if (statusCode == HttpStatus.BAD_REQUEST.value()) {
            try {
                Map<String, Object> body400 = objectMapper.readValue(ex.getResponseBodyAsString(), 
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> errores = (List<Map<String, Object>>) body400.get("errors");
                
                if (errores != null && !errores.isEmpty()) {
                    String mensajesError = errores.stream()
                            .map(error -> String.valueOf(error.get("detail")))
                            .collect(Collectors.joining(", "));
                    resultado.setMensajeError(mensajesError);
                } else {
                    resultado.setMensajeError("No fue posible dar de alta la cuenta.");
                }
            } catch (Exception e) {
                log.error("Error al procesar respuesta de error 400: {}", e.getMessage(), e);
                resultado.setMensajeError("No fue posible dar de alta la cuenta.");
            }
        } else {
            log.error("Error inesperado al dar de alta la cuenta. Código: {}", statusCode);
            resultado.setMensajeError("No fue posible dar de alta la cuenta, error inesperado.");
        }

        return resultado;
    }

    private synchronized String obtenerToken() {
        if (token == null) {
            try {
                Map<String, String> loginRequest = Map.of(
                        "clientId", clientId,
                        "username", username,
                        "password", password
                );

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, String>> entity = new HttpEntity<>(loginRequest, headers);

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        loginUrl,
                        HttpMethod.POST,
                        entity,
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );

                Map<String, Object> responseBody = response.getBody();
                if (response.getStatusCode() == HttpStatus.OK && responseBody != null) {
                    token = (String) responseBody.get("token");
                    if (token == null || token.isEmpty()) {
                        throw new RuntimeException("No se pudo iniciar sesión. El token regresó vacío.");
                    }
                } else {
                    throw new RuntimeException("No se pudo iniciar sesión en AUNESA.");
                }
            } catch (Exception e) {
                log.error("Error al obtener token de AUNESA: {}", e.getMessage(), e);
                throw new RuntimeException("Error al obtener token de sesión: " + e.getMessage(), e);
            }
        }
        return token;
    }

    private Map<String, Object> enviarSolicitud(Map<String, Object> body) {
        try {
            String ltoken = obtenerToken();
            if (ltoken == null || ltoken.isEmpty()) {
                throw new RuntimeException("AUNESA: Error al obtener el token de sesión");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + ltoken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    cuentaUrl,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

            return null;
        } catch (HttpClientErrorException ex) {
            int statusCode = ex.getStatusCode().value();
            // Loggear el JSON cuando hay un error HTTP
            try {
                String jsonBody = objectMapper.writeValueAsString(body);
                log.error("Error HTTP en AUNESA. Status: {}. Response: {}. JSON enviado: {}", 
                        statusCode, ex.getResponseBodyAsString(), jsonBody);
            } catch (Exception e) {
                log.error("Error al serializar JSON para logging: {}", e.getMessage());
            }
            
            if (statusCode == HttpStatus.FORBIDDEN.value() || statusCode == HttpStatus.UNAUTHORIZED.value()) {
                token = null;
                return enviarSolicitud(body); // Reintentar con nuevo token
            }
            throw ex;
        }
    }

    private Map<String, Object> construirJsonSolicitud(Solicitud solicitud) {
        Map<String, Object> res = new LinkedHashMap<>();
        
        res.put("idCuenta", solicitud.getIdCuenta());
        res.put("clase", null);
        res.put("codigoCartera", null);
        res.put("codigoCategoria", null);

        Map<String, Object> titular = new LinkedHashMap<>();
        titular.put("personaFisica", solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO);
        
        // Domicilio
        if (solicitud.getTitular() != null && solicitud.getTitular().getDomicilio() != null) {
            Domicilio domicilio = solicitud.getTitular().getDomicilio();
            Map<String, Object> domicilioMap = new LinkedHashMap<>();
            domicilioMap.put("uso", domicilio.getTipo() != null ? domicilio.getTipo().getValor() : null);
            domicilioMap.put("barrio", domicilio.getBarrio());
            domicilioMap.put("calle", domicilio.getCalle());
            domicilioMap.put("numero", parsearEnteroSeguro(domicilio.getNumero()));
            domicilioMap.put("piso", parsearEnteroSeguro(domicilio.getPiso()));
            domicilioMap.put("departamento", domicilio.getDepto());
            String lugar = String.format("%s, %s, %s", 
                    domicilio.getPais() != null ? domicilio.getPais() : "",
                    domicilio.getProvincia() != null ? domicilio.getProvincia() : "",
                    domicilio.getCiudad() != null ? domicilio.getCiudad() : "").trim();
            domicilioMap.put("lugar", lugar);
            domicilioMap.put("codigoPostal", domicilio.getCp());
            
            titular.put("domicilioUrbano", Arrays.asList(domicilioMap));
        }

        // Perfil inversor
        if (solicitud.getTitular() != null && solicitud.getTitular().getPerfilInversor() != null) {
            PerfilInversor perfil = solicitud.getTitular().getPerfilInversor();
            Map<String, Object> perfilMap = new LinkedHashMap<>();
            perfilMap.put("experiencia", "Ninguna");
            perfilMap.put("perfilPersonal", perfil.getTipo() != null ? perfil.getTipo().getDescripcion() : null);
            titular.put("perfilInversor", perfilMap);
        }

        // Declaraciones PF iniciales (comunes)
        if (solicitud.getTitular() instanceof Persona persona) {
            Map<String, Object> declaracionesPF = new LinkedHashMap<>();
            declaracionesPF.put("sujetoObligado", persona.getDeclaraUIF());
            declaracionesPF.put("sujetoObligadoUIF", persona.getMotivoUIF());
            declaracionesPF.put("personaEEUU", persona.getEsFATCA());
            declaracionesPF.put("observacionesFATCA", persona.getMotivoFatca());
            titular.put("declaracionesPF", declaracionesPF);
        } else if (solicitud.getTitular() instanceof Empresa empresa) {
            Map<String, Object> declaracionesPF = new LinkedHashMap<>();
            declaracionesPF.put("sujetoObligado", empresa.getDeclaraUIF());
            declaracionesPF.put("sujetoObligadoUIF", empresa.getMotivoUIF());
            declaracionesPF.put("personaEEUU", null);
            declaracionesPF.put("observacionesFATCA", null);
            titular.put("declaracionesPF", declaracionesPF);
        }

        // Medios de comunicación
        List<Map<String, Object>> mediosComunicacion = new ArrayList<>();
        if (solicitud.getTitular() != null) {
            String telefono = solicitud.getTitular() instanceof Persona persona ? persona.getTelefono() : 
                             solicitud.getTitular() instanceof Empresa empresa ? empresa.getTelefono() : null;
            String celular = solicitud.getTitular() instanceof Persona persona ? persona.getCelular() : 
                            solicitud.getTitular() instanceof Empresa empresa ? empresa.getCelular() : null;
            String email = solicitud.getTitular() instanceof Persona persona ? persona.getCorreoElectronico() : 
                          solicitud.getTitular() instanceof Empresa empresa ? empresa.getCorreoElectronico() : null;

            mediosComunicacion.add(crearMedioComunicacion("Teléfono", "Personal", telefono != null ? telefono : "0", false));
            mediosComunicacion.add(crearMedioComunicacion("Movil", "Personal", celular != null ? celular : "0", false));
            mediosComunicacion.add(crearMedioComunicacion("E-Mail", "Personal", email != null ? email : "N/A", false));
        }
        titular.put("mediocomunicacion", mediosComunicacion);

        // Cuentas bancarias nacionales
        if (solicitud.getCuentasBancarias() != null) {
            List<Map<String, Object>> cuentasNacionales = solicitud.getCuentasBancarias().stream()
                    .filter(cb -> cb.getTipo() != null && cb.getTipo().isEsNacional())
                    .map(cb -> {
                        Map<String, Object> cuentaMap = new LinkedHashMap<>();
                        cuentaMap.put("cbu", cb.getClaveBancaria());
                        cuentaMap.put("tipoID", cb.getTipoClaveBancaria() != null ? cb.getTipoClaveBancaria().name() : null);
                        cuentaMap.put("titular", solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO && solicitud.getTitular() instanceof Persona persona ? 
                                persona.getIdNumero() : null);
                        cuentaMap.put("moneda", cb.getMoneda() != null ? cb.getMoneda().getDescripcion() : null);
                        cuentaMap.put("tipo", cb.getTipo() != null ? cb.getTipo().getDescripcion() : null);
                        cuentaMap.put("uso", solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO ? "Personal" : "Comercial");
                        cuentaMap.put("vigenteDesde", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                        return cuentaMap;
                    })
                    .collect(Collectors.toList());
            titular.put("cuentaBancaria", cuentasNacionales);
        }

        // Datos fiscales
        if (solicitud.getTitular() != null && solicitud.getTitular().getDatosFiscales() != null) {
            DatosFiscales datosFiscales = solicitud.getTitular().getDatosFiscales();
            Map<String, Object> datosFiscalesMap = new LinkedHashMap<>();
            datosFiscalesMap.put("tipoCodigo", datosFiscales.getTipo() != null ? datosFiscales.getTipo().name() : null);
            datosFiscalesMap.put("cuit", datosFiscales.getClaveFiscal());
            datosFiscalesMap.put("tipoResponsableIVA", datosFiscales.getTipoIva() != null ? datosFiscales.getTipoIva().getDescripcion() : null);
            datosFiscalesMap.put("tipoResponsableGanancias", datosFiscales.getTipoGanancia() != null ? datosFiscales.getTipoGanancia().getDescripcion() : null);
            titular.put("datosFiscales", datosFiscalesMap);
        }

        // Residencia fiscal exterior
        if (solicitud.getTitular() != null && solicitud.getTitular().getDatosFiscalesExterior() != null) {
            DatosFiscales datosFiscalesExterior = solicitud.getTitular().getDatosFiscalesExterior();
            Map<String, Object> residenciaFiscalMap = new LinkedHashMap<>();
            residenciaFiscalMap.put("residenciaFiscalExterior", datosFiscalesExterior.getResidenciaFiscal());
            residenciaFiscalMap.put("tipoIdExterior", datosFiscalesExterior.getTipo() != null ? datosFiscalesExterior.getTipo().name() : null);
            residenciaFiscalMap.put("idFiscalExterior", datosFiscalesExterior.getClaveFiscal());
            titular.put("residenciaFiscalExterior", Arrays.asList(residenciaFiscalMap));
        }

        // Cuentas bancarias exterior
        if (solicitud.getCuentasBancarias() != null) {
            List<Map<String, Object>> cuentasExterior = solicitud.getCuentasBancarias().stream()
                    .filter(cb -> cb.getTipo() != null && !cb.getTipo().isEsNacional())
                    .map(cb -> {
                        Map<String, Object> cuentaMap = new LinkedHashMap<>();
                        cuentaMap.put("cuenta", cb.getClaveBancaria());
                        cuentaMap.put("moneda", cb.getMoneda() != null ? cb.getMoneda().getDescripcion() : null);
                        cuentaMap.put("banco", cb.getBanco());
                        
                        String direccion = "";
                        if (solicitud.getTitular() != null && solicitud.getTitular().getDomicilio() != null) {
                            Domicilio domicilio = solicitud.getTitular().getDomicilio();
                            direccion = String.format("%s %s, %s %s",
                                    domicilio.getCalle() != null ? domicilio.getCalle() : "",
                                    domicilio.getNumero() != null ? domicilio.getNumero() : "",
                                    domicilio.getProvincia() != null ? domicilio.getProvincia() : "",
                                    domicilio.getCiudad() != null ? domicilio.getCiudad() : "").trim();
                        }
                        cuentaMap.put("direccion", direccion);
                        cuentaMap.put("holderType", cb.getTipoCliente() != null ? cb.getTipoCliente().getDescripcion() : null);
                        cuentaMap.put("accountType", cb.getTipo() != null ? cb.getTipo().getDescripcion() : null);
                        cuentaMap.put("pais", cb.getPais());
                        cuentaMap.put("vigenteDesde", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                        cuentaMap.put("nroBancoABA", cb.getNumeroAba());
                        cuentaMap.put("idSWIFT", cb.getIdentificacionSwift());
                        return cuentaMap;
                    })
                    .collect(Collectors.toList());
            
            if (!cuentasExterior.isEmpty()) {
                titular.put("cuentaBancariaExterior", cuentasExterior);
            }
        }

        // Si es INDIVIDUO
        if (solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO && solicitud.getTitular() instanceof Persona persona) {
            // Datos personales
            Map<String, Object> datosPersonales = new LinkedHashMap<>();
            datosPersonales.put("fechaNacimiento", convertirFecha(persona.getFechaNacimiento()));
            datosPersonales.put("sexo", persona.getSexo() != null ? persona.getSexo().getDescripcion() : null);
            datosPersonales.put("estadoCivil", persona.getEstadoCivil() != null ? persona.getEstadoCivil().getValor() : null);
            datosPersonales.put("nacionalidad", persona.getNacionalidad());
            datosPersonales.put("paisResidencia", persona.getPaisResidencia());
            datosPersonales.put("paisOrigen", persona.getPaisOrigen());
            datosPersonales.put("lugarNacimiento", persona.getLugarNacimiento());
            datosPersonales.put("actividad", persona.getActividad());
            titular.put("datosPersonales", datosPersonales);

            // Datos principales físicas
            Map<String, Object> datosPrincipales = new LinkedHashMap<>();
            datosPrincipales.put("nombre", persona.getNombres());
            datosPrincipales.put("apellido", persona.getApellidos());
            datosPrincipales.put("tipoID", persona.getTipoID() != null ? persona.getTipoID().name() : null);
            datosPrincipales.put("id", persona.getIdNumero());
            titular.put("datosPrincipalesFisicas", datosPrincipales);

            // Declaraciones PF (individual)
            Map<String, Object> declaracionesPFInd = new LinkedHashMap<>();
            declaracionesPFInd.put("expuestaPoliticamente", persona.getEsPep());
            declaracionesPFInd.put("detalleExpPoliticamente", persona.getMotivoPep());
            declaracionesPFInd.put("sujetoObligado", persona.getDeclaraUIF());
            declaracionesPFInd.put("numeroInscripcion", 0);
            declaracionesPFInd.put("sujetoObligadoUIF", persona.getMotivoUIF());
            declaracionesPFInd.put("personaEEUU", persona.getEsFATCA());
            declaracionesPFInd.put("observacionesFATCA", persona.getMotivoFatca());
            titular.put("declaracionesPF", declaracionesPFInd);

            // Datos cónyuge si está casado
            if (persona.getEstadoCivil() == Persona.EstadoCivil.CASADO && persona.getConyuge() != null) {
                Conyuge conyuge = persona.getConyuge();
                Map<String, Object> datosConyugeMap = new LinkedHashMap<>();
                datosConyugeMap.put("nombre", conyuge.getNombres());
                datosConyugeMap.put("apellido", conyuge.getApellidos());
                datosConyugeMap.put("tipoID", conyuge.getTipoID() != null ? conyuge.getTipoID().name() : null);
                datosConyugeMap.put("id", conyuge.getIdNumero());
                datosConyugeMap.put("tipoFiscal", conyuge.getTipoClaveFiscal() != null ? conyuge.getTipoClaveFiscal().name() : null);
                datosConyugeMap.put("claveFiscal", conyuge.getClaveFiscal());
                titular.put("datosConyuge", Arrays.asList(datosConyugeMap));
            }

            // Actividad persona
            Map<String, Object> actividadPersonaMap = new LinkedHashMap<>();
            actividadPersonaMap.put("actividad", persona.getActividad());
            titular.put("actividadPersona", Arrays.asList(actividadPersonaMap));

            // Personas relacionadas (firmantes)
            if (solicitud.getFirmantes() != null && !solicitud.getFirmantes().isEmpty()) {
                List<Map<String, Object>> personasRelacionadas = new ArrayList<>();
                for (Persona firmante : solicitud.getFirmantes()) {
                    Map<String, Object> personaRelacionada = construirPersonaRelacionada(firmante, solicitud);
                    personaRelacionada.put("orden", 0);
                    personaRelacionada.put("tipo", "Condómino");
                    personasRelacionadas.add(personaRelacionada);
                }
                res.put("personaRelacionada", personasRelacionadas);
            }

        } else if (solicitud.getTitular() instanceof Empresa empresa) {
            // EMPRESA
            
            // Datos organización
            Map<String, Object> datosOrganizacion = new LinkedHashMap<>();
            datosOrganizacion.put("fechaConstitucion", convertirFecha(empresa.getFechaConstitucion()));
            datosOrganizacion.put("paisOrigen", empresa.getPaisOrigen());
            datosOrganizacion.put("paisResidencia", empresa.getPaisResidencia());
            datosOrganizacion.put("cierreBalance", convertirFecha(empresa.getFechaCierreBalance()));
            titular.put("datosOrganizacion", datosOrganizacion);

            // Datos principales ideal
            Map<String, Object> datosPrincipalesIdeal = new LinkedHashMap<>();
            datosPrincipalesIdeal.put("denominacion", empresa.getDenominacion());
            datosPrincipalesIdeal.put("tipoDeOrganizacion", empresa.getTipoEmpresa() != null ? empresa.getTipoEmpresa().getDescripcion() : null);
            datosPrincipalesIdeal.put("tipoID", empresa.getDatosFiscales() != null && empresa.getDatosFiscales().getTipo() != null ? 
                    empresa.getDatosFiscales().getTipo().name() : null);
            datosPrincipalesIdeal.put("id", empresa.getDatosFiscales() != null ? empresa.getDatosFiscales().getClaveFiscal() : null);
            titular.put("datosPrincipalesIdeal", datosPrincipalesIdeal);

            // Registro
            Map<String, Object> registroMap = new LinkedHashMap<>();
            registroMap.put("tipo", empresa.getLugarInscripcionRegistro() != null ? empresa.getLugarInscripcionRegistro().getDescripcion() : null);
            registroMap.put("numero", empresa.getNumeroRegistro());
            String lugarRegistro = String.format("%s, %s, %s",
                    empresa.getPaisRegistro() != null ? empresa.getPaisRegistro() : "",
                    empresa.getProvinciaRegistro() != null ? empresa.getProvinciaRegistro() : "",
                    empresa.getLugarRegistro() != null ? empresa.getLugarRegistro() : "").trim();
            registroMap.put("lugar", lugarRegistro);
            registroMap.put("fecha", convertirFecha(empresa.getFechaRegistro()));
            registroMap.put("folio", empresa.getFolio());
            registroMap.put("libro", empresa.getLibro());
            registroMap.put("tomo", empresa.getTomo());
            titular.put("registro", Arrays.asList(registroMap));

            // Uso firma
            Map<String, Object> usoFirmaMap = new LinkedHashMap<>();
            usoFirmaMap.put("tipo", empresa.getUsoFirma() != null ? empresa.getUsoFirma().getDescripcion() : null);
            res.put("usoFirma", Arrays.asList(usoFirmaMap));

            // Autoridad (firmantes)
            if (solicitud.getFirmantes() != null && !solicitud.getFirmantes().isEmpty()) {
                List<Map<String, Object>> autoridades = new ArrayList<>();
                for (Persona firmante : solicitud.getFirmantes()) {
                    Map<String, Object> autoridad = construirAutoridad(firmante, solicitud);
                    autoridad.put("orden", 0);
                    autoridad.put("cargo", firmante.getTipo() != null ? firmante.getTipo().getDescripcion() : null);
                    autoridad.put("realizarSeguimiento", false);
                    autoridades.add(autoridad);
                }
                titular.put("autoridad", autoridades);
            }

            // Accionistas
            if (empresa.getAccionistas() != null && !empresa.getAccionistas().isEmpty()) {
                List<Map<String, Object>> accionistasList = new ArrayList<>();
                for (Organizacion accionistaOrg : empresa.getAccionistas()) {
                    if (accionistaOrg instanceof Persona personaAccionista) {
                        Map<String, Object> accionistaMap = construirPersonaAccionista(personaAccionista);
                        Map<String, Object> accionistaWrapper = new LinkedHashMap<>();
                        accionistaWrapper.put("accionista", accionistaMap);
                        accionistaWrapper.put("porcentaje", personaAccionista.getPorcentaje());
                        accionistasList.add(accionistaWrapper);
                    }
                    // Nota: Por ahora solo se manejan Persona como accionistas
                    // Las Empresas accionistas requerirían una estructura similar pero adaptada
                }
                if (!accionistasList.isEmpty()) {
                    titular.put("accionista", accionistasList);
                }
            }

            // Actividad organización
            Map<String, Object> actividadOrganizacionMap = new LinkedHashMap<>();
            actividadOrganizacionMap.put("actividad", empresa.getActividad());
            titular.put("actividadOrganizacion", Arrays.asList(actividadOrganizacionMap));
        }

        res.put("titular", titular);

        // Disposiciones generales - debe ser un objeto, no un array
        Map<String, Object> disposicionesGenerales = new LinkedHashMap<>();
        disposicionesGenerales.put("tipoCliente", solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO ? "Persona" : "Empresa");
        disposicionesGenerales.put("vigenciaDesde", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        res.put("disposicionesGenerales", disposicionesGenerales);

        return res;
    }


    private Map<String, Object> construirPersonaRelacionada(Persona firmante, Solicitud solicitud) {
        Map<String, Object> persona = new LinkedHashMap<>();
        persona.put("personaFisica", true);

        // Datos fiscales
        if (firmante.getDatosFiscales() != null) {
            DatosFiscales datosFiscales = firmante.getDatosFiscales();
            Map<String, Object> datosFiscalesMap = new LinkedHashMap<>();
            datosFiscalesMap.put("tipoCodigo", datosFiscales.getTipo() != null ? datosFiscales.getTipo().name() : null);
            datosFiscalesMap.put("cuit", datosFiscales.getClaveFiscal());
            datosFiscalesMap.put("tipoResponsableIVA", datosFiscales.getTipoIva() != null ? datosFiscales.getTipoIva().getDescripcion() : null);
            datosFiscalesMap.put("tipoResponsableGanancias", datosFiscales.getTipoGanancia() != null ? datosFiscales.getTipoGanancia().getDescripcion() : null);
            persona.put("datosFiscales", datosFiscalesMap);
        }

        // Medios de comunicación
        List<Map<String, Object>> medios = new ArrayList<>();
        medios.add(crearMedioComunicacion("Teléfono", "Personal", firmante.getTelefono() != null ? firmante.getTelefono() : "0", false));
        medios.add(crearMedioComunicacion("Movil", "Personal", firmante.getCelular() != null ? firmante.getCelular() : "0", true));
        medios.add(crearMedioComunicacion("E-Mail", "Personal", firmante.getCorreoElectronico() != null ? firmante.getCorreoElectronico() : "0", false));
        persona.put("mediocomunicacion", medios);

        // Domicilio
        if (firmante.getDomicilio() != null) {
            Domicilio domicilio = firmante.getDomicilio();
            Map<String, Object> domicilioMap = new LinkedHashMap<>();
            domicilioMap.put("uso", "Legal");
            domicilioMap.put("barrio", domicilio.getBarrio());
            domicilioMap.put("calle", domicilio.getCalle());
            domicilioMap.put("numero", parsearEnteroSeguro(domicilio.getNumero()));
            domicilioMap.put("piso", parsearEnteroSeguro(domicilio.getPiso()));
            domicilioMap.put("departamento", domicilio.getDepto());
            String lugar = String.format("%s, %s, %s",
                    domicilio.getPais() != null ? domicilio.getPais() : "",
                    domicilio.getProvincia() != null ? domicilio.getProvincia() : "",
                    domicilio.getCiudad() != null ? domicilio.getCiudad() : "").trim();
            domicilioMap.put("lugar", lugar);
            domicilioMap.put("codigoPostal", domicilio.getCp());
            persona.put("domicilioUrbano", Arrays.asList(domicilioMap));
        }

        // Datos personales
        Map<String, Object> datosPersonales = new LinkedHashMap<>();
        datosPersonales.put("fechaNacimiento", convertirFecha(firmante.getFechaNacimiento()));
        datosPersonales.put("sexo", firmante.getSexo() != null ? firmante.getSexo().getDescripcion() : null);
        datosPersonales.put("estadoCivil", firmante.getEstadoCivil() != null ? firmante.getEstadoCivil().getValor() : null);
        datosPersonales.put("nacionalidad", firmante.getNacionalidad());
        datosPersonales.put("paisResidencia", firmante.getPaisResidencia());
        datosPersonales.put("paisOrigen", firmante.getPaisOrigen());
        datosPersonales.put("lugarNacimiento", firmante.getLugarNacimiento());
        datosPersonales.put("actividad", firmante.getActividad());
        persona.put("datosPersonales", datosPersonales);

        // Datos principales físicas
        Map<String, Object> datosPrincipales = new LinkedHashMap<>();
        datosPrincipales.put("nombre", firmante.getNombres());
        datosPrincipales.put("apellido", firmante.getApellidos());
        datosPrincipales.put("tipoID", firmante.getTipoID() != null ? firmante.getTipoID().name() : null);
        datosPrincipales.put("id", firmante.getIdNumero());
        persona.put("datosPrincipalesFisicas", datosPrincipales);

        // Declaraciones PF
        Map<String, Object> declaracionesPF = new LinkedHashMap<>();
        declaracionesPF.put("expuestaPoliticamente", firmante.getEsPep());
        declaracionesPF.put("detalleExpPoliticamente", firmante.getMotivoPep());
        declaracionesPF.put("sujetoObligado", firmante.getDeclaraUIF());
        declaracionesPF.put("numeroInscripcion", 0);
        declaracionesPF.put("sujetoObligadoUIF", firmante.getMotivoUIF());
        declaracionesPF.put("personaEEUU", firmante.getEsFATCA());
        declaracionesPF.put("observacionesFATCA", firmante.getMotivoFatca());
        persona.put("declaracionesPF", declaracionesPF);

        // Cónyuge si está casado
        if (firmante.getEstadoCivil() == Persona.EstadoCivil.CASADO && firmante.getConyuge() != null) {
            Conyuge conyuge = firmante.getConyuge();
            Map<String, Object> datosConyugeMap = new LinkedHashMap<>();
            datosConyugeMap.put("nombre", conyuge.getNombres());
            datosConyugeMap.put("apellido", conyuge.getApellidos());
            datosConyugeMap.put("tipoID", conyuge.getTipoID() != null ? conyuge.getTipoID().name() : null);
            datosConyugeMap.put("id", conyuge.getIdNumero());
            datosConyugeMap.put("tipoFiscal", conyuge.getTipoClaveFiscal() != null ? conyuge.getTipoClaveFiscal().name() : null);
            datosConyugeMap.put("claveFiscal", conyuge.getClaveFiscal());
            persona.put("datosConyuge", Arrays.asList(datosConyugeMap));
        }

        // Residencia fiscal exterior
        if (firmante.getDatosFiscalesExterior() != null) {
            DatosFiscales datosFiscalesExterior = firmante.getDatosFiscalesExterior();
            Map<String, Object> residenciaFiscalMap = new LinkedHashMap<>();
            residenciaFiscalMap.put("residenciaFiscalExterior", datosFiscalesExterior.getResidenciaFiscal());
            residenciaFiscalMap.put("tipoIdExterior", datosFiscalesExterior.getTipo() != null ? datosFiscalesExterior.getTipo().name() : null);
            residenciaFiscalMap.put("idFiscalExterior", datosFiscalesExterior.getClaveFiscal());
            persona.put("residenciaFiscalExterior", Arrays.asList(residenciaFiscalMap));
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("persona", persona);
        return res;
    }

    private Map<String, Object> construirAutoridad(Persona firmante, Solicitud solicitud) {
        Map<String, Object> autoridad = new LinkedHashMap<>();
        autoridad.put("personaFisica", true);

        // Datos fiscales - siempre CUIL para firmantes de empresa
        if (firmante.getDatosFiscales() != null) {
            Map<String, Object> datosFiscalesMap = new LinkedHashMap<>();
            datosFiscalesMap.put("tipoCodigo", "CUIL");
            datosFiscalesMap.put("cuit", firmante.getDatosFiscales().getClaveFiscal());
            autoridad.put("datosFiscales", datosFiscalesMap);
        }

        // Medios de comunicación
        List<Map<String, Object>> medios = new ArrayList<>();
        medios.add(crearMedioComunicacion("Teléfono", "Personal", firmante.getTelefono() != null ? firmante.getTelefono() : "0", false));
        medios.add(crearMedioComunicacion("Movil", "Personal", firmante.getCelular() != null ? firmante.getCelular() : "0", true));
        medios.add(crearMedioComunicacion("E-Mail", "Personal", firmante.getCorreoElectronico() != null ? firmante.getCorreoElectronico() : "0", false));
        autoridad.put("mediocomunicacion", medios);

        // Domicilio
        if (firmante.getDomicilio() != null) {
            Domicilio domicilio = firmante.getDomicilio();
            Map<String, Object> domicilioMap = new LinkedHashMap<>();
            domicilioMap.put("uso", "Legal");
            domicilioMap.put("barrio", domicilio.getBarrio());
            domicilioMap.put("calle", domicilio.getCalle());
            domicilioMap.put("numero", parsearEnteroSeguro(domicilio.getNumero()));
            domicilioMap.put("piso", parsearEnteroSeguro(domicilio.getPiso()));
            domicilioMap.put("departamento", domicilio.getDepto());
            String lugar = String.format("%s, %s, %s",
                    domicilio.getPais() != null ? domicilio.getPais() : "",
                    domicilio.getProvincia() != null ? domicilio.getProvincia() : "",
                    domicilio.getCiudad() != null ? domicilio.getCiudad() : "").trim();
            domicilioMap.put("lugar", lugar);
            domicilioMap.put("codigoPostal", domicilio.getCp());
            autoridad.put("domicilioUrbano", Arrays.asList(domicilioMap));
        }

        // Datos personales
        Map<String, Object> datosPersonales = new LinkedHashMap<>();
        datosPersonales.put("fechaNacimiento", convertirFecha(firmante.getFechaNacimiento()));
        datosPersonales.put("sexo", firmante.getSexo() != null ? firmante.getSexo().getDescripcion() : null);
        datosPersonales.put("estadoCivil", firmante.getEstadoCivil() != null ? firmante.getEstadoCivil().getValor() : null);
        datosPersonales.put("nacionalidad", firmante.getNacionalidad());
        datosPersonales.put("paisResidencia", firmante.getPaisResidencia());
        datosPersonales.put("paisOrigen", firmante.getPaisOrigen());
        datosPersonales.put("lugarNacimiento", firmante.getLugarNacimiento());
        datosPersonales.put("actividad", firmante.getActividad());
        autoridad.put("datosPersonales", datosPersonales);

        // Datos principales físicas
        Map<String, Object> datosPrincipales = new LinkedHashMap<>();
        datosPrincipales.put("nombre", firmante.getNombres());
        datosPrincipales.put("apellido", firmante.getApellidos());
        datosPrincipales.put("tipoID", firmante.getTipoID() != null ? firmante.getTipoID().name() : null);
        datosPrincipales.put("id", firmante.getIdNumero());
        autoridad.put("datosPrincipalesFisicas", datosPrincipales);

        // Declaraciones PF
        Map<String, Object> declaracionesPF = new LinkedHashMap<>();
        declaracionesPF.put("expuestaPoliticamente", firmante.getEsPep());
        declaracionesPF.put("detalleExpPoliticamente", firmante.getMotivoPep());
        declaracionesPF.put("sujetoObligado", firmante.getDeclaraUIF());
        declaracionesPF.put("numeroInscripcion", 0);
        declaracionesPF.put("sujetoObligadoUIF", firmante.getMotivoUIF());
        declaracionesPF.put("personaEEUU", firmante.getEsFATCA());
        declaracionesPF.put("observacionesFATCA", firmante.getMotivoFatca());
        autoridad.put("declaracionesPF", declaracionesPF);

        // Actividad persona
        Map<String, Object> actividadPersonaMap = new LinkedHashMap<>();
        actividadPersonaMap.put("actividad", firmante.getActividad());
        actividadPersonaMap.put("puesto", firmante.getTipo() != null ? firmante.getTipo().getDescripcion() : null);
        autoridad.put("actividadPersona", Arrays.asList(actividadPersonaMap));

        // Cónyuge si está casado
        if (firmante.getEstadoCivil() == Persona.EstadoCivil.CASADO && firmante.getConyuge() != null) {
            Conyuge conyuge = firmante.getConyuge();
            Map<String, Object> datosConyugeMap = new LinkedHashMap<>();
            datosConyugeMap.put("nombre", conyuge.getNombres());
            datosConyugeMap.put("apellido", conyuge.getApellidos());
            datosConyugeMap.put("tipoID", conyuge.getTipoID() != null ? conyuge.getTipoID().name() : null);
            datosConyugeMap.put("id", conyuge.getIdNumero());
            datosConyugeMap.put("tipoFiscal", conyuge.getTipoClaveFiscal() != null ? conyuge.getTipoClaveFiscal().name() : null);
            datosConyugeMap.put("claveFiscal", conyuge.getClaveFiscal());
            autoridad.put("datosConyuge", Arrays.asList(datosConyugeMap));
        }

        // Residencia fiscal exterior
        if (firmante.getDatosFiscalesExterior() != null) {
            DatosFiscales datosFiscalesExterior = firmante.getDatosFiscalesExterior();
            Map<String, Object> residenciaFiscalMap = new LinkedHashMap<>();
            residenciaFiscalMap.put("residenciaFiscalExterior", datosFiscalesExterior.getResidenciaFiscal());
            residenciaFiscalMap.put("tipoIdExterior", datosFiscalesExterior.getTipo() != null ? datosFiscalesExterior.getTipo().name() : null);
            residenciaFiscalMap.put("idFiscalExterior", datosFiscalesExterior.getClaveFiscal());
            autoridad.put("residenciaFiscalExterior", Arrays.asList(residenciaFiscalMap));
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("autoridad", autoridad);
        return res;
    }

    private Map<String, Object> construirPersonaAccionista(Persona personaAccionista) {
        Map<String, Object> accionista = new LinkedHashMap<>();
        accionista.put("personaFisica", true);

        // Datos fiscales - siempre CUIL para accionistas
        if (personaAccionista.getDatosFiscales() != null) {
            Map<String, Object> datosFiscalesMap = new LinkedHashMap<>();
            datosFiscalesMap.put("tipoCodigo", "CUIL");
            datosFiscalesMap.put("cuit", personaAccionista.getDatosFiscales().getClaveFiscal());
            accionista.put("datosFiscales", datosFiscalesMap);
        }

        // Medios de comunicación
        List<Map<String, Object>> medios = new ArrayList<>();
        medios.add(crearMedioComunicacion("Teléfono", "Personal", personaAccionista.getTelefono() != null ? personaAccionista.getTelefono() : "0", false));
        medios.add(crearMedioComunicacion("Movil", "Personal", personaAccionista.getCelular() != null ? personaAccionista.getCelular() : "0", true));
        medios.add(crearMedioComunicacion("E-Mail", "Personal", personaAccionista.getCorreoElectronico() != null ? personaAccionista.getCorreoElectronico() : "0", false));
        accionista.put("mediocomunicacion", medios);

        // Domicilio
        if (personaAccionista.getDomicilio() != null) {
            Domicilio domicilio = personaAccionista.getDomicilio();
            Map<String, Object> domicilioMap = new LinkedHashMap<>();
            domicilioMap.put("uso", "Legal");
            domicilioMap.put("barrio", domicilio.getBarrio());
            domicilioMap.put("calle", domicilio.getCalle());
            domicilioMap.put("numero", parsearEnteroSeguro(domicilio.getNumero()));
            domicilioMap.put("piso", parsearEnteroSeguro(domicilio.getPiso()));
            domicilioMap.put("departamento", domicilio.getDepto());
            String lugar = String.format("%s, %s, %s",
                    domicilio.getPais() != null ? domicilio.getPais() : "",
                    domicilio.getProvincia() != null ? domicilio.getProvincia() : "",
                    domicilio.getCiudad() != null ? domicilio.getCiudad() : "").trim();
            domicilioMap.put("lugar", lugar);
            domicilioMap.put("codigoPostal", domicilio.getCp());
            accionista.put("domicilioUrbano", Arrays.asList(domicilioMap));
        }

        // Datos personales
        Map<String, Object> datosPersonales = new LinkedHashMap<>();
        datosPersonales.put("fechaNacimiento", convertirFecha(personaAccionista.getFechaNacimiento()));
        datosPersonales.put("sexo", personaAccionista.getSexo() != null ? personaAccionista.getSexo().getDescripcion() : null);
        datosPersonales.put("estadoCivil", personaAccionista.getEstadoCivil() != null ? personaAccionista.getEstadoCivil().getValor() : null);
        datosPersonales.put("nacionalidad", personaAccionista.getNacionalidad());
        datosPersonales.put("paisResidencia", personaAccionista.getPaisResidencia());
        datosPersonales.put("paisOrigen", personaAccionista.getPaisOrigen());
        datosPersonales.put("lugarNacimiento", personaAccionista.getLugarNacimiento());
        datosPersonales.put("actividad", personaAccionista.getActividad());
        accionista.put("datosPersonales", datosPersonales);

        // Datos principales físicas
        Map<String, Object> datosPrincipales = new LinkedHashMap<>();
        datosPrincipales.put("nombre", personaAccionista.getNombres());
        datosPrincipales.put("apellido", personaAccionista.getApellidos());
        datosPrincipales.put("tipoID", personaAccionista.getTipoID() != null ? personaAccionista.getTipoID().name() : null);
        datosPrincipales.put("id", personaAccionista.getIdNumero());
        accionista.put("datosPrincipalesFisicas", datosPrincipales);

        // Declaraciones PF
        Map<String, Object> declaracionesPF = new LinkedHashMap<>();
        declaracionesPF.put("expuestaPoliticamente", personaAccionista.getEsPep());
        declaracionesPF.put("detalleExpPoliticamente", personaAccionista.getMotivoPep());
        declaracionesPF.put("sujetoObligado", personaAccionista.getDeclaraUIF());
        declaracionesPF.put("numeroInscripcion", 0);
        declaracionesPF.put("sujetoObligadoUIF", personaAccionista.getMotivoUIF());
        declaracionesPF.put("personaEEUU", personaAccionista.getEsFATCA());
        declaracionesPF.put("observacionesFATCA", personaAccionista.getMotivoFatca());
        accionista.put("declaracionesPF", declaracionesPF);

        // Actividad persona
        Map<String, Object> actividadPersonaMap = new LinkedHashMap<>();
        actividadPersonaMap.put("actividad", personaAccionista.getActividad());
        actividadPersonaMap.put("puesto", personaAccionista.getTipo() != null ? personaAccionista.getTipo().getDescripcion() : null);
        accionista.put("actividadPersona", Arrays.asList(actividadPersonaMap));

        // Cónyuge si está casado
        if (personaAccionista.getEstadoCivil() == Persona.EstadoCivil.CASADO && personaAccionista.getConyuge() != null) {
            Conyuge conyuge = personaAccionista.getConyuge();
            Map<String, Object> datosConyugeMap = new LinkedHashMap<>();
            datosConyugeMap.put("nombre", conyuge.getNombres());
            datosConyugeMap.put("apellido", conyuge.getApellidos());
            datosConyugeMap.put("tipoID", conyuge.getTipoID() != null ? conyuge.getTipoID().name() : null);
            datosConyugeMap.put("id", conyuge.getIdNumero());
            datosConyugeMap.put("tipoFiscal", conyuge.getTipoClaveFiscal() != null ? conyuge.getTipoClaveFiscal().name() : null);
            datosConyugeMap.put("claveFiscal", conyuge.getClaveFiscal());
            accionista.put("datosConyuge", Arrays.asList(datosConyugeMap));
        }

        return accionista;
    }

    private Map<String, Object> crearMedioComunicacion(String tipo, String uso, String medio, boolean principal) {
        Map<String, Object> medioMap = new LinkedHashMap<>();
        medioMap.put("tipo", tipo);
        medioMap.put("uso", uso);
        medioMap.put("principal", principal);
        medioMap.put("medio", medio);
        return medioMap;
    }

    private String convertirFecha(LocalDate fecha) {
        if (fecha == null) {
            return null;
        }
        try {
            return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            log.warn("Error al convertir fecha {}: {}", fecha, e.getMessage());
            return null;
        }
    }

    /**
     * Parsea un String a Integer de forma segura, manejando null y cadenas vacías.
     * @param valor String a parsear
     * @return Integer parseado o null si el valor es null, vacío o no es un número válido
     */
    private Integer parsearEnteroSeguro(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            log.warn("Error al parsear entero '{}': {}", valor, e.getMessage());
            return null;
        }
    }

    @Override
    public List<Provincia> obtenerProvincias() {
        try {
            String ltoken = obtenerToken();
            if (ltoken == null || ltoken.isEmpty()) {
                throw new RuntimeException("AUNESA: Error al obtener el token de sesión");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + ltoken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    provinciasUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Provincia> provincias = objectMapper.readValue(response.getBody(), 
                        new TypeReference<List<Provincia>>() {});
                log.info("Se obtuvieron {} provincias de AUNESA", provincias != null ? provincias.size() : 0);
                return provincias != null ? provincias : new ArrayList<>();
            }

            log.warn("No se pudieron obtener provincias de AUNESA. Código: {}", response.getStatusCode());
            return new ArrayList<>();

        } catch (HttpClientErrorException ex) {
            int statusCode = ex.getStatusCode().value();
            if (statusCode == HttpStatus.FORBIDDEN.value() || statusCode == HttpStatus.UNAUTHORIZED.value()) {
                token = null;
                return obtenerProvincias(); // Reintentar con nuevo token
            }
            log.error("Error al obtener provincias de AUNESA: {}", ex.getMessage(), ex);
            throw new RuntimeException("Error al obtener provincias de AUNESA: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Error al obtener provincias de AUNESA: {}", ex.getMessage(), ex);
            throw new RuntimeException("Error al obtener provincias de AUNESA: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Localidad> obtenerLocalidades() {
        return obtenerLocalidades(null);
    }

    @Override
    public List<Localidad> obtenerLocalidades(String codigoProvincia) {
        try {
            String ltoken = obtenerToken();
            if (ltoken == null || ltoken.isEmpty()) {
                throw new RuntimeException("AUNESA: Error al obtener el token de sesión");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + ltoken);

            String url = localidadesUrl;
            if (codigoProvincia != null && !codigoProvincia.isEmpty()) {
                url += "?codigoProvincia=" + codigoProvincia;
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Localidad> localidades = objectMapper.readValue(response.getBody(), 
                        new TypeReference<List<Localidad>>() {});
                log.info("Se obtuvieron {} localidades de AUNESA{}", 
                        localidades != null ? localidades.size() : 0,
                        codigoProvincia != null ? " para provincia " + codigoProvincia : "");
                return localidades != null ? localidades : new ArrayList<>();
            }

            log.warn("No se pudieron obtener localidades de AUNESA. Código: {}", response.getStatusCode());
            return new ArrayList<>();

        } catch (HttpClientErrorException ex) {
            int statusCode = ex.getStatusCode().value();
            if (statusCode == HttpStatus.FORBIDDEN.value() || statusCode == HttpStatus.UNAUTHORIZED.value()) {
                token = null;
                return obtenerLocalidades(codigoProvincia); // Reintentar con nuevo token
            }
            log.error("Error al obtener localidades de AUNESA: {}", ex.getMessage(), ex);
            throw new RuntimeException("Error al obtener localidades de AUNESA: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Error al obtener localidades de AUNESA: {}", ex.getMessage(), ex);
            throw new RuntimeException("Error al obtener localidades de AUNESA: " + ex.getMessage(), ex);
        }
    }
}

