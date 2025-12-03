package ar.com.st.service.impl;

import ar.com.st.dto.firmaDigital.EstadoDocumentoDTO;
import ar.com.st.dto.firmaDigital.EstadoFirmaDTO;
import ar.com.st.service.SignaturaService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Implementación del servicio para comunicación con el servicio externo de firma digital (Signatura)
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Service
@Slf4j
public class SignaturaServiceImpl implements SignaturaService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${app.signatura.crear-documento-url:}")
    private String crearDocumentoUrl;
    
    @Value("${app.signatura.obtener-documento-url:}")
    private String obtenerDocumentoUrl;
    
    @Value("${app.signatura.cancelar-documento-url:}")
    private String cancelarDocumentoUrl;
    
    @Value("${app.signatura.reenviar-firma-url:}")
    private String reenviarFirmaUrl;
    
    @Value("${app.signatura.obtener-pdf-firmado-url:}")
    private String obtenerPdfFirmadoUrl;
    
    @Value("${app.signatura.certificado-firma-url:}")
    private String certificadoFirmaUrl;
    
    @Value("${app.signatura.authorization-token:}")
    private String authorizationToken;
    
    @Value("${app.signatura.timeout:5000}")
    private int timeout;

    public SignaturaServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String crearDocumento(String titulo, List<String> emails, String contenidoPdfBase64) {
        if (crearDocumentoUrl == null || crearDocumentoUrl.isEmpty()) {
            log.warn("URL de crear documento no configurada. Saltando creación de documento.");
            return null;
        }
        
        if (authorizationToken == null || authorizationToken.isEmpty()) {
            log.warn("Token de autorización no configurado. Saltando creación de documento.");
            return null;
        }

        // Validar parámetros requeridos
        if (titulo == null || titulo.trim().isEmpty()) {
            log.error("El título del documento es requerido y no puede estar vacío");
            return null;
        }
        
        if (contenidoPdfBase64 == null || contenidoPdfBase64.trim().isEmpty()) {
            log.error("El contenido del PDF en Base64 es requerido y no puede estar vacío");
            return null;
        }
        
        if (emails == null || emails.isEmpty()) {
            log.error("La lista de emails es requerida y no puede estar vacía");
            return null;
        }

        try {
            // Construir el JSON manualmente como Map (similar a AunesaServiceImpl)
            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("title", titulo.trim());
            requestBody.put("fashion", "SE");
            requestBody.put("selected_emails", emails.stream().distinct().toList());
            requestBody.put("validations", Arrays.asList("EM", "AF"));
            requestBody.put("file_content", contenidoPdfBase64.trim());
            
            log.debug("Creando documento con título: '{}', emails: {}, tamaño Base64: {} caracteres", 
                    titulo, emails.size(), contenidoPdfBase64.length());
            
            // Serializar a JSON String manualmente
            String requestJson;
            if (objectMapper != null) {
                try {
                    requestJson = objectMapper.writeValueAsString(requestBody);
                    log.debug("Request JSON a enviar (primeros 500 chars): {}", 
                            requestJson.length() > 500 ? requestJson.substring(0, 500) + "..." : requestJson);
                } catch (Exception e) {
                    log.error("Error al serializar el request a JSON: {}", e.getMessage());
                    return null;
                }
            } else {
                log.error("ObjectMapper no está disponible para serializar el request");
                return null;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.set("Authorization", "Bearer " + authorizationToken);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<CrearDocumentoResponse> response = restTemplate.exchange(
                    crearDocumentoUrl,
                    HttpMethod.POST,
                    entity,
                    CrearDocumentoResponse.class
            );
            
            log.debug("Respuesta recibida - Status: {}, Body: {}", response.getStatusCode(), response.getBody());

            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                CrearDocumentoResponse responseBody = response.getBody();
                if (responseBody != null && responseBody.getId() != null) {
                    log.info("Documento de firma digital creado exitosamente con ID: {}", responseBody.getId());
                    return responseBody.getId();
                }
            }

            log.error("Error al crear documento de firma digital. Código: {}, Mensaje: {}", 
                    response.getStatusCode(), response.getBody());
            return null;

        } catch (Exception ex) {
            // Loggear detalles del error para debugging
            if (ex instanceof org.springframework.web.client.HttpClientErrorException httpEx) {
                log.error("Error HTTP al crear documento de firma digital. Status: {}, Response: {}", 
                        httpEx.getStatusCode(), httpEx.getResponseBodyAsString());
                try {
                    log.debug("Request que se intentó enviar - Título: '{}', Emails: {}, Tamaño Base64: {} caracteres", 
                            titulo, emails, contenidoPdfBase64 != null ? contenidoPdfBase64.length() : 0);
                } catch (Exception logEx) {
                    // Ignorar errores de logging
                }
            } else {
                log.error("Error al crear documento de firma digital: {}", ex.getMessage(), ex);
            }
            return null;
        }
    }

    @Data
    private static class CrearDocumentoRequest {
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("fashion")
        private String fashion;
        
        @JsonProperty("selected_emails")
        private List<String> selected_emails;
        
        @JsonProperty("validations")
        private List<String> validations;
        
        @JsonProperty("file_content")
        private String file_content;
    }

    @Data
    private static class CrearDocumentoResponse {
        private String id;
        private String title;
        private String status;
        private String tiny_url;
    }

    @Override
    public EstadoDocumentoDTO obtenerEstadoDocumento(String id) {
        if (obtenerDocumentoUrl == null || obtenerDocumentoUrl.isEmpty() || 
            authorizationToken == null || authorizationToken.isEmpty()) {
            log.warn("URL o token no configurado. No se puede obtener el estado del documento.");
            return null;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + authorizationToken);

            String url = obtenerDocumentoUrl.replace("{id}", id);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<DocumentoResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    DocumentoResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return mapearDocumentoAEstado(response.getBody());
            }

            log.error("Error al obtener estado del documento {}. Código: {}", id, response.getStatusCode());
            return null;

        } catch (Exception ex) {
            log.error("Error al obtener estado del documento {}: {}", id, ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public boolean cancelarDocumento(String id, String razon) {
        if (cancelarDocumentoUrl == null || cancelarDocumentoUrl.isEmpty() || 
            authorizationToken == null || authorizationToken.isEmpty()) {
            log.warn("URL o token no configurado. No se puede cancelar el documento.");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + authorizationToken);

            String url = cancelarDocumentoUrl.replace("{id}", id);
            CancelarDocumentoRequest request = new CancelarDocumentoRequest();
            request.setCancel_reason(razon);

            HttpEntity<CancelarDocumentoRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    entity,
                    Void.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Documento {} cancelado exitosamente", id);
                return true;
            }

            log.error("Error al cancelar documento {}. Código: {}", id, response.getStatusCode());
            return false;

        } catch (Exception ex) {
            log.error("Error al cancelar documento {}: {}", id, ex.getMessage(), ex);
            return false;
        }
    }

    @Override
    public boolean reenviarEmailFirmaDigital(String idFirma) {
        if (reenviarFirmaUrl == null || reenviarFirmaUrl.isEmpty() || 
            authorizationToken == null || authorizationToken.isEmpty()) {
            log.warn("URL o token no configurado. No se puede reenviar el email.");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + authorizationToken);

            String url = reenviarFirmaUrl.replace("{id}", idFirma);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.info("Email de firma digital reenviado exitosamente para firma {}", idFirma);
                return true;
            }

            log.error("Error al reenviar email de firma digital {}. Código: {}", idFirma, response.getStatusCode());
            return false;

        } catch (Exception ex) {
            log.error("Error al reenviar email de firma digital {}: {}", idFirma, ex.getMessage(), ex);
            return false;
        }
    }

    @Override
    public void obtenerDocumentoPdfFirmado(String id, OutputStream out) {
        if (obtenerPdfFirmadoUrl == null || obtenerPdfFirmadoUrl.isEmpty() || 
            authorizationToken == null || authorizationToken.isEmpty()) {
            throw new RuntimeException("URL o token no configurado. No se puede obtener el PDF firmado.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + authorizationToken);

            String url = obtenerPdfFirmadoUrl.replace("{id}", id);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                out.write(response.getBody());
                log.info("PDF firmado obtenido exitosamente para documento {}", id);
            } else {
                throw new RuntimeException("Error al obtener PDF firmado. Código: " + response.getStatusCode());
            }

        } catch (Exception ex) {
            log.error("Error al obtener PDF firmado del documento {}: {}", id, ex.getMessage(), ex);
            throw new RuntimeException("Error al obtener PDF firmado del documento " + id, ex);
        }
    }

    @Override
    public void obtenerCertificadoFirma(String id, OutputStream out) {
        if (certificadoFirmaUrl == null || certificadoFirmaUrl.isEmpty() || 
            authorizationToken == null || authorizationToken.isEmpty()) {
            throw new RuntimeException("URL o token no configurado. No se puede obtener el certificado de firma.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + authorizationToken);

            String url = certificadoFirmaUrl.replace("{id}", id);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                out.write(response.getBody());
                log.info("Certificado de firma obtenido exitosamente para documento {}", id);
            } else {
                throw new RuntimeException("Error al obtener certificado de firma. Código: " + response.getStatusCode());
            }

        } catch (Exception ex) {
            log.error("Error al obtener certificado de firma del documento {}: {}", id, ex.getMessage(), ex);
            throw new RuntimeException("Error al obtener certificado de firma del documento " + id, ex);
        }
    }

    private EstadoDocumentoDTO mapearDocumentoAEstado(DocumentoResponse documento) {
        EstadoDocumentoDTO estado = new EstadoDocumentoDTO();
        estado.setId(documento.getId());
        estado.setEstado(convertirEstado(documento.getStatus()));
        estado.setFecha(convertirFecha(documento.getCreation_date()));
        estado.setMotivoCancelacion(documento.getCancel_reason());
        estado.setTitulo(documento.getTitle());
        estado.setUrl(documento.getTiny_url());

        if (documento.getSignatures() != null) {
            List<EstadoFirmaDTO> firmas = new ArrayList<>();
            for (SignatureResponse signature : documento.getSignatures()) {
                EstadoFirmaDTO firma = new EstadoFirmaDTO();
                firma.setId(signature.getId());
                firma.setUrl(signature.getUrl());
                firma.setFecha(convertirFecha(signature.getCreated_date()));
                firma.setEstado(convertirEstado(signature.getStatus()));
                
                // Extraer email de validations
                if (signature.getValidations() != null) {
                    Object emValidation = signature.getValidations().get("EM");
                    if (emValidation instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> emMap = (Map<String, Object>) emValidation;
                        Object value = emMap.get("value");
                        if (value != null) {
                            firma.setEmail(value.toString());
                        }
                    }
                }
                
                firmas.add(firma);
            }
            estado.setFirmas(firmas);
        }

        return estado;
    }

    private EstadoFirmaDTO.Estado convertirEstado(String status) {
        if (status == null) {
            return EstadoFirmaDTO.Estado.INCOMPLETO;
        }
        return switch (status.toUpperCase()) {
            case "IN" -> EstadoFirmaDTO.Estado.INCOMPLETO;
            case "CA" -> EstadoFirmaDTO.Estado.CANCELADO;
            case "PE" -> EstadoFirmaDTO.Estado.PENDIENTE;
            default -> EstadoFirmaDTO.Estado.COMPLETO;
        };
    }

    private LocalDateTime convertirFecha(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    @Data
    private static class CancelarDocumentoRequest {
        @JsonProperty("cancel_reason")
        private String cancel_reason;
    }

    @Data
    private static class DocumentoResponse {
        private String id;
        private String title;
        private String status;
        private String tiny_url;
        @JsonProperty("creation_date")
        private Date creation_date;
        @JsonProperty("cancel_reason")
        private String cancel_reason;
        private List<SignatureResponse> signatures;
    }

    @Data
    private static class SignatureResponse {
        private String id;
        private String url;
        @JsonProperty("created_date")
        private Date created_date;
        private String status;
        private Map<String, Object> validations;
    }
}

