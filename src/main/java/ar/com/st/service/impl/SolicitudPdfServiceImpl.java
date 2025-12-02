package ar.com.st.service.impl;

import ar.com.st.entity.*;
import ar.com.st.service.SolicitudPdfService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del servicio de generación de PDFs de solicitudes
 * 
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SolicitudPdfServiceImpl implements SolicitudPdfService {

    @Override
    public byte[] generarPdf(Solicitud solicitud) {
        log.info("Generando PDF para solicitud con ID: {}", solicitud.getId());

        if (solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO) {
            return generarPdfAperturaPersonaFisica(solicitud);
        } else if (solicitud.getTipo() == Solicitud.Tipo.EMPRESA_CLASICA) {
            return generarPdfAperturaPersonaJuridica(solicitud);
        }

        // Para otros tipos se puede agregar lógica similar con otras plantillas
        throw new UnsupportedOperationException(
                "Generación de PDF no implementada para el tipo de solicitud: " + solicitud.getTipo());
    }

    /**
     * Genera el PDF de apertura de cuenta para una solicitud de tipo INDIVIDUO
     * utilizando la plantilla HTML 'pdf/invidiuo/titular.html' ubicada en resources.
     * Si hay firmantes, también genera sus PDFs y los combina.
     * Luego agrega los PDFs estáticos de datosPersonales y perfilRiesgo.
     */
    private byte[] generarPdfAperturaPersonaFisica(Solicitud solicitud) {
        if (!(solicitud.getTitular() instanceof Persona persona)) {
            throw new IllegalStateException("La solicitud " + solicitud.getId()
                    + " es de tipo INDIVIDUO pero su titular no es una Persona");
        }

        try {
            List<byte[]> pdfs = new ArrayList<>();

            // Generar PDF del titular
            byte[] pdfTitular = generarPdfTitular(solicitud, persona);
            pdfs.add(pdfTitular);

            // Generar PDF de cada firmante
            if (solicitud.getFirmantes() != null && !solicitud.getFirmantes().isEmpty()) {
                for (Persona firmante : solicitud.getFirmantes()) {
                    byte[] pdfFirmante = generarPdfFirmante(solicitud, firmante);
                    pdfs.add(pdfFirmante);
                }
            }

            // Agregar PDF estático de datos personales
            byte[] pdfDatosPersonales = cargarPdfEstatico("pdf/datosPersonales.pdf");
            if (pdfDatosPersonales != null) {
                pdfs.add(pdfDatosPersonales);
            }

            // Agregar PDF estático de perfil de riesgo
            byte[] pdfPerfilRiesgo = cargarPdfEstatico("pdf/perfilRiesgo.pdf");
            if (pdfPerfilRiesgo != null) {
                pdfs.add(pdfPerfilRiesgo);
            }

            // Generar PDF dinámico de perfil de inversor
            if (persona.getPerfilInversor() != null) {
                byte[] pdfPerfilInversor = generarPdfPerfilInversor(solicitud, persona);
                if (pdfPerfilInversor != null) {
                    pdfs.add(pdfPerfilInversor);
                }
            }

            // Generar PDF dinámico de cuentas bancarias
            if (solicitud.getCuentasBancarias() != null && !solicitud.getCuentasBancarias().isEmpty()) {
                byte[] pdfCuentasBancarias = generarPdfCuentasBancarias(solicitud, persona);
                if (pdfCuentasBancarias != null) {
                    pdfs.add(pdfCuentasBancarias);
                }
            }

            // Agregar PDFs estáticos adicionales
            byte[] pdfComisiones = cargarPdfEstatico("pdf/comisiones.pdf");
            if (pdfComisiones != null) {
                pdfs.add(pdfComisiones);
            }

            byte[] pdfPorcentajeDerecho = cargarPdfEstatico("pdf/porcentajeDerecho.pdf");
            if (pdfPorcentajeDerecho != null) {
                pdfs.add(pdfPorcentajeDerecho);
            }

            byte[] pdfDeclaracionJurada = cargarPdfEstatico("pdf/declaracionJurada.pdf");
            if (pdfDeclaracionJurada != null) {
                pdfs.add(pdfDeclaracionJurada);
            }

            byte[] pdfConstanciaEntregaReglamento = cargarPdfEstatico("pdf/constanciaEntregaReglamento.pdf");
            if (pdfConstanciaEntregaReglamento != null) {
                pdfs.add(pdfConstanciaEntregaReglamento);
            }

            byte[] pdfSolicitudApertura = cargarPdfEstatico("pdf/solicitudApertura.pdf");
            if (pdfSolicitudApertura != null) {
                pdfs.add(pdfSolicitudApertura);
            }

            byte[] pdfDocumentacion = cargarPdfEstatico("pdf/documentacion.pdf");
            if (pdfDocumentacion != null) {
                pdfs.add(pdfDocumentacion);
            }

            byte[] pdfTerminosCondiciones = cargarPdfEstatico("pdf/terminosCondiciones.pdf");
            if (pdfTerminosCondiciones != null) {
                pdfs.add(pdfTerminosCondiciones);
            }


            // Si hay más de un PDF, combinarlos
            if (pdfs.size() > 1) {
                return combinarPdfs(pdfs);
            }

            return pdfs.get(0);

        } catch (IOException e) {
            log.error("Error al generar el PDF de la solicitud {}: {}", solicitud.getId(), e.getMessage(), e);
            throw new RuntimeException("No se pudo generar el PDF de la solicitud " + solicitud.getId(), e);
        }
    }

    /**
     * Genera el PDF de apertura de cuenta para una solicitud de tipo EMPRESA_CLASICA
     * utilizando la plantilla HTML 'pdf/empresa/razonSocial.html' ubicada en resources.
     */
    private byte[] generarPdfAperturaPersonaJuridica(Solicitud solicitud) {
        if (!(solicitud.getTitular() instanceof Empresa empresa)) {
            throw new IllegalStateException("La solicitud " + solicitud.getId()
                    + " es de tipo EMPRESA_CLASICA pero su titular no es una Empresa");
        }

        try {
            List<byte[]> pdfs = new ArrayList<>();

            // Generar PDF de razón social
            byte[] pdfRazonSocial = generarPdfRazonSocial(solicitud, empresa);
            pdfs.add(pdfRazonSocial);

            // Generar PDF de cada firmante
            if (solicitud.getFirmantes() != null && !solicitud.getFirmantes().isEmpty()) {
                for (Persona firmante : solicitud.getFirmantes()) {
                    byte[] pdfFirmante = generarPdfFirmanteEmpresa(solicitud, firmante);
                    pdfs.add(pdfFirmante);
                }
            }

            // Agregar PDFs estáticos adicionales
            byte[] pdfDatosPersonalesEmpresa = cargarPdfEstatico("pdf/datosPersonales.pdf");
            if (pdfDatosPersonalesEmpresa != null) {
                pdfs.add(pdfDatosPersonalesEmpresa);
            }

            byte[] pdfTerminosCondiciones = cargarPdfEstatico("pdf/terminosCondiciones.pdf");
            if (pdfTerminosCondiciones != null) {
                pdfs.add(pdfTerminosCondiciones);
            }

            byte[] pdfPerfilRiesgo = cargarPdfEstatico("pdf/perfilRiesgo.pdf");
            if (pdfPerfilRiesgo != null) {
                pdfs.add(pdfPerfilRiesgo);
            }

            // Generar PDF dinámico de perfil de inversor
            if (empresa.getPerfilInversor() != null) {
                byte[] pdfPerfilInversor = generarPdfPerfilInversorEmpresa(solicitud, empresa);
                if (pdfPerfilInversor != null) {
                    pdfs.add(pdfPerfilInversor);
                }
            }

            // Agregar PDFs estáticos adicionales
            byte[] pdfComisiones = cargarPdfEstatico("pdf/comisiones.pdf");
            if (pdfComisiones != null) {
                pdfs.add(pdfComisiones);
            }

            byte[] pdfConstanciaEntregaReglamento = cargarPdfEstatico("pdf/constanciaEntregaReglamento.pdf");
            if (pdfConstanciaEntregaReglamento != null) {
                pdfs.add(pdfConstanciaEntregaReglamento);
            }

            byte[] pdfDeclaracionJurada = cargarPdfEstatico("pdf/declaracionJurada.pdf");
            if (pdfDeclaracionJurada != null) {
                pdfs.add(pdfDeclaracionJurada);
            }

            // Si hay más de un PDF, combinarlos
            if (pdfs.size() > 1) {
                return combinarPdfs(pdfs);
            }

            return pdfs.get(0);

        } catch (IOException e) {
            log.error("Error al generar el PDF de la solicitud {}: {}", solicitud.getId(), e.getMessage(), e);
            throw new RuntimeException("No se pudo generar el PDF de la solicitud " + solicitud.getId(), e);
        }
    }

    /**
     * Carga un PDF estático desde los resources
     */
    private byte[] cargarPdfEstatico(String ruta) {
        try {
            ClassPathResource resource = new ClassPathResource(ruta);
            if (!resource.exists()) {
                log.warn("No se encontró el PDF estático: {}", ruta);
                return null;
            }
            try (InputStream is = resource.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (IOException e) {
            log.error("Error al cargar el PDF estático {}: {}", ruta, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Genera el PDF del titular usando la plantilla titular.html
     */
    private byte[] generarPdfTitular(Solicitud solicitud, Persona persona) throws IOException {
        ClassPathResource template = new ClassPathResource("pdf/invidiuo/titular.html");
        String html;
        try (InputStream is = template.getInputStream()) {
            html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Reemplazar los valores en el HTML
        html = reemplazarValoresHtml(html, solicitud, persona);

        // Convertir HTML a PDF
        return convertirHtmlAPdf(html);
    }

    /**
     * Genera el PDF de razón social usando la plantilla razonSocial.html
     */
    private byte[] generarPdfRazonSocial(Solicitud solicitud, Empresa empresa) throws IOException {
        ClassPathResource template = new ClassPathResource("pdf/empresa/razonSocial.html");
        String html;
        try (InputStream is = template.getInputStream()) {
            html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Reemplazar los valores en el HTML
        html = reemplazarValoresHtmlRazonSocial(html, solicitud, empresa);

        // Convertir HTML a PDF
        return convertirHtmlAPdf(html);
    }

    /**
     * Genera el PDF de un firmante de empresa usando la plantilla firmante.html
     */
    private byte[] generarPdfFirmanteEmpresa(Solicitud solicitud, Persona firmante) throws IOException {
        ClassPathResource template = new ClassPathResource("pdf/empresa/firmante.html");
        String html;
        try (InputStream is = template.getInputStream()) {
            html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Reemplazar los valores en el HTML (usa el mismo método que para firmantes de individuo)
        html = reemplazarValoresHtmlFirmante(html, solicitud, firmante);

        // Convertir HTML a PDF
        return convertirHtmlAPdf(html);
    }

    /**
     * Genera el PDF de un firmante usando la plantilla firmante.html
     */
    private byte[] generarPdfFirmante(Solicitud solicitud, Persona firmante) throws IOException {
        ClassPathResource template = new ClassPathResource("pdf/invidiuo/firmante.html");
        String html;
        try (InputStream is = template.getInputStream()) {
            html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Reemplazar los valores en el HTML
        html = reemplazarValoresHtmlFirmante(html, solicitud, firmante);

        // Convertir HTML a PDF
        return convertirHtmlAPdf(html);
    }

    /**
     * Genera el PDF de cuentas bancarias usando la plantilla cuentasBancarias.html
     */
    private byte[] generarPdfCuentasBancarias(Solicitud solicitud, Persona persona) {
        try {
            if (solicitud.getCuentasBancarias() == null || solicitud.getCuentasBancarias().isEmpty()) {
                log.warn("No hay cuentas bancarias para la solicitud {}", solicitud.getId());
                return null;
            }

            ClassPathResource template = new ClassPathResource("pdf/invidiuo/cuentasBancarias.html");
            String html;
            try (InputStream is = template.getInputStream()) {
                html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            // Reemplazar los valores en el HTML
            html = reemplazarValoresHtmlCuentasBancarias(html, solicitud, persona);

            // Convertir HTML a PDF
            return convertirHtmlAPdf(html);

        } catch (IOException e) {
            log.error("Error al generar el PDF de cuentas bancarias para la solicitud {}: {}", 
                    solicitud.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Genera el PDF del perfil de inversor usando la plantilla perfilInversor.html
     */
    private byte[] generarPdfPerfilInversor(Solicitud solicitud, Persona persona) {
        try {
            PerfilInversor perfilInversor = persona.getPerfilInversor();
            if (perfilInversor == null || perfilInversor.getRespuestas() == null || perfilInversor.getRespuestas().isEmpty()) {
                log.warn("No hay respuestas del perfil de inversor para la solicitud {}", solicitud.getId());
                return null;
            }

            ClassPathResource template = new ClassPathResource("pdf/perfilInversor.html");
            String html;
            try (InputStream is = template.getInputStream()) {
                html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            // Reemplazar los valores en el HTML
            html = reemplazarValoresHtmlPerfilInversor(html, solicitud, persona, perfilInversor);

            // Convertir HTML a PDF
            return convertirHtmlAPdf(html);

        } catch (IOException e) {
            log.error("Error al generar el PDF del perfil de inversor para la solicitud {}: {}", 
                    solicitud.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Genera el PDF del perfil de inversor para empresa usando la plantilla perfilInversor.html
     */
    private byte[] generarPdfPerfilInversorEmpresa(Solicitud solicitud, Empresa empresa) {
        try {
            PerfilInversor perfilInversor = empresa.getPerfilInversor();
            if (perfilInversor == null || perfilInversor.getRespuestas() == null || perfilInversor.getRespuestas().isEmpty()) {
                log.warn("No hay respuestas del perfil de inversor para la solicitud {}", solicitud.getId());
                return null;
            }

            ClassPathResource template = new ClassPathResource("pdf/perfilInversor.html");
            String html;
            try (InputStream is = template.getInputStream()) {
                html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            // Reemplazar los valores en el HTML (usa el mismo método pero no requiere persona ya que no se usa)
            html = reemplazarValoresHtmlPerfilInversor(html, solicitud, null, perfilInversor);

            // Convertir HTML a PDF
            return convertirHtmlAPdf(html);

        } catch (IOException e) {
            log.error("Error al generar el PDF del perfil de inversor para la solicitud {}: {}", 
                    solicitud.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Reemplaza los valores en el HTML con los datos de la solicitud y persona
     */
    private String reemplazarValoresHtml(String html, Solicitud solicitud, Persona persona) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", 
                java.util.Locale.forLanguageTag("es-AR"));

        // Datos de la cuenta comitente
        html = html.replace("{denominacionCuenta}", 
                obtenerValor(buildNombreCompleto(persona)));
        html = html.replace("{domicilioCuenta}", 
                obtenerValor(obtenerDomicilioCompleto(persona.getDomicilio())));
        html = html.replace("{numeroCuenta}", 
                obtenerValor(solicitud.getIdCuenta() != null ? solicitud.getIdCuenta().toString() : ""));
        html = html.replace("{fechaApertura}", 
                obtenerValor(solicitud.getFechaAlta() != null ? 
                        solicitud.getFechaAlta().format(dateFormatter) : ""));
        html = html.replace("{mesAño}", 
                LocalDate.now().format(monthYearFormatter));

        // Datos personales del titular
        html = html.replace("{t1_nombre}", 
                obtenerValor(buildNombreCompleto(persona)));
        html = html.replace("{t1_dni}", 
                obtenerValor(buildDocumento(persona)));
        html = html.replace("{t1_cuil}", 
                obtenerValor(persona.getDatosFiscales() != null ? 
                        persona.getDatosFiscales().getClaveFiscal() : ""));
        html = html.replace("{t1_estadoCivil}", 
                obtenerValor(persona.getEstadoCivil() != null ? 
                        persona.getEstadoCivil().getValor() : ""));
        html = html.replace("{t1_nacionalidad}", 
                obtenerValor(persona.getNacionalidad()));
        html = html.replace("{t1_fechaNac}", 
                obtenerValor(persona.getFechaNacimiento() != null ? 
                        persona.getFechaNacimiento().format(dateFormatter) : ""));
        html = html.replace("{t1_lugarNac}", 
                obtenerValor(persona.getLugarNacimiento()));
        html = html.replace("{t1_celular}", 
                obtenerValor(persona.getCelular()));
        html = html.replace("{t1_email}", 
                obtenerValor(persona.getCorreoElectronico()));

        // Domicilio
        if (persona.getDomicilio() != null) {
            Domicilio domicilio = persona.getDomicilio();
            html = html.replace("{t1_calle}", obtenerValor(domicilio.getCalle()));
            html = html.replace("{t1_nro}", obtenerValor(domicilio.getNumero()));
            html = html.replace("{t1_piso}", obtenerValor(domicilio.getPiso()));
            html = html.replace("{t1_dpto}", obtenerValor(domicilio.getDepto()));
            html = html.replace("{t1_pais}", obtenerValor(domicilio.getPais()));
            html = html.replace("{t1_provincia}", obtenerValor(domicilio.getProvincia()));
            html = html.replace("{t1_localidad}", obtenerValor(domicilio.getCiudad()));
            html = html.replace("{t1_cp}", obtenerValor(domicilio.getCp()));
        } else {
            html = html.replace("{t1_calle}", "");
            html = html.replace("{t1_nro}", "");
            html = html.replace("{t1_piso}", "");
            html = html.replace("{t1_dpto}", "");
            html = html.replace("{t1_pais}", "");
            html = html.replace("{t1_provincia}", "");
            html = html.replace("{t1_localidad}", "");
            html = html.replace("{t1_cp}", "");
        }

        // Declaraciones PEP - Por defecto NO si es null
        boolean esPep = persona.getEsPep() != null && persona.getEsPep();
        html = html.replace("{t1_pep_respuesta}", esPep ? "SI" : "NO");
        html = html.replace("{t1_pep_motivo}", 
                obtenerValor(persona.getMotivoPep()));

        // Declaraciones US Person - Por defecto NO si es null
        boolean esFatca = persona.getEsFATCA() != null && persona.getEsFATCA();
        html = html.replace("{t1_usperson_respuesta}", esFatca ? "SI" : "NO");
        html = html.replace("{t1_us_motivo}", 
                obtenerValor(persona.getMotivoFatca()));

        // Declaraciones Sujeto Obligado - Por defecto NO si es null
        boolean declaraUIF = persona.getDeclaraUIF() != null && persona.getDeclaraUIF();
        html = html.replace("{t1_sujetoObligado_respuesta}", declaraUIF ? "SI" : "NO");
        html = html.replace("{t1_sujeto_motivo}", 
                obtenerValor(persona.getMotivoUIF()));

        return html;
    }

    /**
     * Reemplaza los valores en el HTML con los datos del firmante
     * Similar a reemplazarValoresHtml pero sin los campos de cuenta comitente
     */
    private String reemplazarValoresHtmlFirmante(String html, Solicitud solicitud, Persona firmante) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", 
                java.util.Locale.forLanguageTag("es-AR"));

        // Mes y año (solo para el header)
        html = html.replace("{mesAño}", 
                LocalDate.now().format(monthYearFormatter));

        // Datos personales del firmante
        html = html.replace("{t1_nombre}", 
                obtenerValor(buildNombreCompleto(firmante)));
        html = html.replace("{t1_dni}", 
                obtenerValor(buildDocumento(firmante)));
        html = html.replace("{t1_cuil}", 
                obtenerValor(firmante.getDatosFiscales() != null ? 
                        firmante.getDatosFiscales().getClaveFiscal() : ""));
        html = html.replace("{t1_estadoCivil}", 
                obtenerValor(firmante.getEstadoCivil() != null ? 
                        firmante.getEstadoCivil().getValor() : ""));
        html = html.replace("{t1_nacionalidad}", 
                obtenerValor(firmante.getNacionalidad()));
        html = html.replace("{t1_fechaNac}", 
                obtenerValor(firmante.getFechaNacimiento() != null ? 
                        firmante.getFechaNacimiento().format(dateFormatter) : ""));
        html = html.replace("{t1_lugarNac}", 
                obtenerValor(firmante.getLugarNacimiento()));
        html = html.replace("{t1_celular}", 
                obtenerValor(firmante.getCelular()));
        html = html.replace("{t1_email}", 
                obtenerValor(firmante.getCorreoElectronico()));

        // Domicilio
        if (firmante.getDomicilio() != null) {
            Domicilio domicilio = firmante.getDomicilio();
            html = html.replace("{t1_calle}", obtenerValor(domicilio.getCalle()));
            html = html.replace("{t1_nro}", obtenerValor(domicilio.getNumero()));
            html = html.replace("{t1_piso}", obtenerValor(domicilio.getPiso()));
            html = html.replace("{t1_dpto}", obtenerValor(domicilio.getDepto()));
            html = html.replace("{t1_pais}", obtenerValor(domicilio.getPais()));
            html = html.replace("{t1_provincia}", obtenerValor(domicilio.getProvincia()));
            html = html.replace("{t1_localidad}", obtenerValor(domicilio.getCiudad()));
            html = html.replace("{t1_cp}", obtenerValor(domicilio.getCp()));
        } else {
            html = html.replace("{t1_calle}", "");
            html = html.replace("{t1_nro}", "");
            html = html.replace("{t1_piso}", "");
            html = html.replace("{t1_dpto}", "");
            html = html.replace("{t1_pais}", "");
            html = html.replace("{t1_provincia}", "");
            html = html.replace("{t1_localidad}", "");
            html = html.replace("{t1_cp}", "");
        }

        // Declaraciones PEP - Por defecto NO si es null
        boolean esPep = firmante.getEsPep() != null && firmante.getEsPep();
        html = html.replace("{t1_pep_respuesta}", esPep ? "SI" : "NO");
        html = html.replace("{t1_pep_motivo}", 
                obtenerValor(firmante.getMotivoPep()));

        // Declaraciones US Person - Por defecto NO si es null
        boolean esFatca = firmante.getEsFATCA() != null && firmante.getEsFATCA();
        html = html.replace("{t1_usperson_respuesta}", esFatca ? "SI" : "NO");
        html = html.replace("{t1_us_motivo}", 
                obtenerValor(firmante.getMotivoFatca()));

        // Declaraciones Sujeto Obligado - Por defecto NO si es null
        boolean declaraUIF = firmante.getDeclaraUIF() != null && firmante.getDeclaraUIF();
        html = html.replace("{t1_sujetoObligado_respuesta}", declaraUIF ? "SI" : "NO");
        html = html.replace("{t1_sujeto_motivo}", 
                obtenerValor(firmante.getMotivoUIF()));

        return html;
    }

    /**
     * Combina múltiples PDFs en un solo PDF
     */
    private byte[] combinarPdfs(List<byte[]> pdfs) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Agregar todos los PDFs como fuentes
        for (byte[] pdfBytes : pdfs) {
            merger.addSource(new ByteArrayInputStream(pdfBytes));
        }

        // Establecer el destino
        merger.setDestinationStream(baos);

        // Combinar los PDFs
        merger.mergeDocuments(null);

        return baos.toByteArray();
    }

    /**
     * Convierte un HTML a PDF usando OpenHTMLToPDF
     */
    private byte[] convertirHtmlAPdf(String html) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            
            // Configurar el contenido HTML con base URI para recursos
            ClassPathResource resourceDir = new ClassPathResource("pdf/");
            String baseUri = resourceDir.getURI().toString();
            
            // Cargar el HTML con la base URI
            builder.withHtmlContent(html, baseUri);
            
            // Configurar tamaño de página A4
            builder.useDefaultPageSize(210, 297, PdfRendererBuilder.PageSizeUnits.MM);
            
            // Configurar el stream de salida
            builder.toStream(baos);
            
            // Renderizar el PDF
            builder.run();

            return baos.toByteArray();
        }
    }

    private String buildNombreCompleto(Persona persona) {
        String nombres = persona.getNombres() != null ? persona.getNombres() : "";
        String apellidos = persona.getApellidos() != null ? persona.getApellidos() : "";
        return (nombres + " " + apellidos).trim();
    }

    private String buildDocumento(Persona persona) {
        String tipo = persona.getTipoID() != null ? persona.getTipoID().name() : "";
        String numero = persona.getIdNumero() != null ? persona.getIdNumero() : "";
        if (tipo.isEmpty() && numero.isEmpty()) {
            return "";
        }
        return tipo + (numero.isEmpty() ? "" : " " + numero);
    }

    private String obtenerDomicilioCompleto(Domicilio domicilio) {
        if (domicilio == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (domicilio.getCalle() != null && !domicilio.getCalle().isEmpty()) {
            sb.append(domicilio.getCalle());
        }
        if (domicilio.getNumero() != null && !domicilio.getNumero().isEmpty()) {
            sb.append(" ").append(domicilio.getNumero());
        }
        return sb.toString().trim();
    }

    /**
     * Reemplaza los valores en el HTML con los datos de las cuentas bancarias
     */
    private String reemplazarValoresHtmlCuentasBancarias(String html, Solicitud solicitud, Persona persona) {
        DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", 
                java.util.Locale.forLanguageTag("es-AR"));

        // Mes y año (solo para el header)
        html = html.replace("{mesAño}", 
                LocalDate.now().format(monthYearFormatter));

        // Número de cuenta comitente
        String numeroCuenta = solicitud.getIdCuenta() != null ? solicitud.getIdCuenta().toString() : "";
        html = html.replace("{numeroCuenta}", obtenerValor(numeroCuenta));

        // CUIL/CUIT del titular
        String cuilCuit = "";
        if (persona.getDatosFiscales() != null) {
            cuilCuit = obtenerValor(persona.getDatosFiscales().getClaveFiscal());
        }

        // Nombre del titular
        String nombreTitular = buildNombreCompleto(persona);

        // Construir el HTML de cuentas bancarias
        StringBuilder cuentasHtml = new StringBuilder();
        
        if (solicitud.getCuentasBancarias() != null && !solicitud.getCuentasBancarias().isEmpty()) {
            int numeroCuentaBancaria = 1;
            for (CuentaBancaria cuenta : solicitud.getCuentasBancarias()) {
                cuentasHtml.append("<div class=\"cuenta-seccion\">");
                cuentasHtml.append("<div class=\"seccion-titulo\">");
                cuentasHtml.append("<span>Cuenta bancaria ").append(numeroCuentaBancaria).append("</span>");
                cuentasHtml.append("</div>");
                
                cuentasHtml.append("<div class=\"seccion-body\">");
                cuentasHtml.append("<div class=\"instruccion-texto\">");
                cuentasHtml.append("Presentar Extracto Bancario (Consulta de CBU), que aparezca Titular, Nro de Cuenta, CUIT/L y CBU");
                cuentasHtml.append("</div>");
                
                // Primera fila: Banco, Tipo de Cuenta, Número de cuenta, Moneda
                cuentasHtml.append("<div class=\"row cols-4\">");
                cuentasHtml.append("<div class=\"field\">");
                cuentasHtml.append("<label>Banco</label>");
                cuentasHtml.append("<div class=\"input-value\">").append(obtenerValor(cuenta.getBanco())).append("</div>");
                cuentasHtml.append("</div>");
                
                cuentasHtml.append("<div class=\"field\">");
                cuentasHtml.append("<label>Tipo de Cuenta</label>");
                cuentasHtml.append("<div class=\"input-value\">").append(cuenta.getTipo() != null ? cuenta.getTipo().getDescripcion() : "").append("</div>");
                cuentasHtml.append("</div>");
                
                cuentasHtml.append("<div class=\"field\">");
                cuentasHtml.append("<label>Número de cuenta</label>");
                cuentasHtml.append("<div class=\"input-value\"></div>"); // No existe este campo en la entidad
                cuentasHtml.append("</div>");
                
                cuentasHtml.append("<div class=\"field\">");
                cuentasHtml.append("<label>Moneda</label>");
                cuentasHtml.append("<div class=\"input-value\">").append(cuenta.getMoneda() != null ? cuenta.getMoneda().getDescripcion() : "").append("</div>");
                cuentasHtml.append("</div>");
                cuentasHtml.append("</div>");
                
                // Segunda fila: CUIL/CUIT, Titular/es, CBU, Alias
                cuentasHtml.append("<div class=\"row cols-4\">");
                cuentasHtml.append("<div class=\"field\">");
                cuentasHtml.append("<label>CUIL / CUIT</label>");
                cuentasHtml.append("<div class=\"input-value\">").append(cuilCuit).append("</div>");
                cuentasHtml.append("</div>");
                
                cuentasHtml.append("<div class=\"field\">");
                cuentasHtml.append("<label>Titular/es</label>");
                cuentasHtml.append("<div class=\"input-value\">").append(nombreTitular).append("</div>");
                cuentasHtml.append("</div>");
                
                cuentasHtml.append("<div class=\"field\">");
                cuentasHtml.append("<label>CBU</label>");
                String cbu = "";
                if (cuenta.getTipoClaveBancaria() == CuentaBancaria.TipoClaveBancaria.CBU && cuenta.getClaveBancaria() != null) {
                    cbu = cuenta.getClaveBancaria();
                }
                cuentasHtml.append("<div class=\"input-value\">").append(cbu).append("</div>");
                cuentasHtml.append("</div>");
                
                cuentasHtml.append("<div class=\"field\">");
                cuentasHtml.append("<label>Alias</label>");
                cuentasHtml.append("<div class=\"input-value\"></div>"); // No existe este campo en la entidad
                cuentasHtml.append("</div>");
                cuentasHtml.append("</div>");
                
                cuentasHtml.append("</div>"); // cierra seccion-body
                cuentasHtml.append("</div>"); // cierra cuenta-seccion
                
                numeroCuentaBancaria++;
            }
        }

        html = html.replace("{cuentas_bancarias}", cuentasHtml.toString());

        return html;
    }

    /**
     * Reemplaza los valores en el HTML con los datos de la empresa (razón social)
     */
    private String reemplazarValoresHtmlRazonSocial(String html, Solicitud solicitud, Empresa empresa) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", 
                java.util.Locale.forLanguageTag("es-AR"));

        // Datos de la cuenta comitente
        html = html.replace("{denominacionCuenta}", 
                obtenerValor(empresa.getDenominacion()));
        html = html.replace("{domicilioCuenta}", 
                obtenerValor(obtenerDomicilioCompleto(empresa.getDomicilio())));
        html = html.replace("{numeroCuenta}", 
                obtenerValor(solicitud.getIdCuenta() != null ? solicitud.getIdCuenta().toString() : ""));
        html = html.replace("{fechaApertura}", 
                obtenerValor(solicitud.getFechaAlta() != null ? 
                        solicitud.getFechaAlta().format(dateFormatter) : ""));
        html = html.replace("{mesAño}", 
                LocalDate.now().format(monthYearFormatter));

        // Datos de la empresa
        html = html.replace("{denominacion}", 
                obtenerValor(empresa.getDenominacion()));

        // Domicilio - campos individuales
        if (empresa.getDomicilio() != null) {
            Domicilio domicilio = empresa.getDomicilio();
            html = html.replace("{calle}", obtenerValor(domicilio.getCalle()));
            html = html.replace("{nro}", obtenerValor(domicilio.getNumero()));
            html = html.replace("{piso}", obtenerValor(domicilio.getPiso()));
            html = html.replace("{dpto}", obtenerValor(domicilio.getDepto()));
            html = html.replace("{pais}", obtenerValor(domicilio.getPais()));
            html = html.replace("{provincia}", obtenerValor(domicilio.getProvincia()));
            html = html.replace("{localidad}", obtenerValor(domicilio.getCiudad()));
            html = html.replace("{cp}", obtenerValor(domicilio.getCp()));
        } else {
            html = html.replace("{calle}", "");
            html = html.replace("{nro}", "");
            html = html.replace("{piso}", "");
            html = html.replace("{dpto}", "");
            html = html.replace("{pais}", "");
            html = html.replace("{provincia}", "");
            html = html.replace("{localidad}", "");
            html = html.replace("{cp}", "");
        }

        // Contacto
        html = html.replace("{telefono}", 
                obtenerValor(empresa.getTelefono()));
        html = html.replace("{celular}", 
                obtenerValor(empresa.getCelular()));
        html = html.replace("{email}", 
                obtenerValor(empresa.getCorreoElectronico()));

        // Constitución
        html = html.replace("{fechaConstitucion}", 
                obtenerValor(empresa.getFechaConstitucion() != null ? 
                        empresa.getFechaConstitucion().format(dateFormatter) : ""));
        html = html.replace("{numeroActa}", 
                obtenerValor(empresa.getNumeroActa()));

        // Tipo y N° de Inscripción
        String tipoNumeroInscripcion = "";
        if (empresa.getLugarInscripcionRegistro() != null) {
            tipoNumeroInscripcion = empresa.getLugarInscripcionRegistro().getDescripcion();
        }
        if (empresa.getNumeroRegistro() != null && !empresa.getNumeroRegistro().isEmpty()) {
            if (!tipoNumeroInscripcion.isEmpty()) {
                tipoNumeroInscripcion += " ";
            }
            tipoNumeroInscripcion += empresa.getNumeroRegistro();
        }
        html = html.replace("{tipoNumeroInscripcion}", 
                obtenerValor(tipoNumeroInscripcion));

        html = html.replace("{lugarRegistro}", 
                obtenerValor(empresa.getLugarRegistro()));

        // Registro
        html = html.replace("{folio}", 
                obtenerValor(empresa.getFolio()));
        html = html.replace("{libro}", 
                obtenerValor(empresa.getLibro()));
        html = html.replace("{tomo}", 
                obtenerValor(empresa.getTomo()));
        html = html.replace("{usoFirma}", 
                obtenerValor(empresa.getUsoFirma() != null ? empresa.getUsoFirma().getDescripcion() : ""));

        // CUIT
        String cuit = "";
        if (empresa.getDatosFiscales() != null) {
            cuit = empresa.getDatosFiscales().getClaveFiscal();
        }
        html = html.replace("{cuit}", 
                obtenerValor(cuit));

        // Aclaraciones s/uso de firma (por ahora vacío, ya que no existe ese campo en la entidad)
        html = html.replace("{aclaracionesFirma}", "");

        // Declaraciones UIF - Por defecto NO si es null
        boolean declaraUIF = empresa.getDeclaraUIF() != null && empresa.getDeclaraUIF();
        html = html.replace("{uif_respuesta}", declaraUIF ? "SI" : "NO");
        html = html.replace("{uif_motivo}", 
                obtenerValor(empresa.getMotivoUIF()));

        // Declaraciones FATCA - Por defecto NO si es null
        boolean esFatca = empresa.getEsFATCA() != null && empresa.getEsFATCA();
        html = html.replace("{fatca_respuesta}", esFatca ? "SI" : "NO");
        html = html.replace("{fatca_motivo}", 
                obtenerValor(empresa.getMotivoFatca()));

        return html;
    }

    /**
     * Reemplaza los valores en el HTML con los datos del perfil de inversor
     */
    private String reemplazarValoresHtmlPerfilInversor(String html, Solicitud solicitud, 
            Persona persona, PerfilInversor perfilInversor) {
        DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", 
                java.util.Locale.forLanguageTag("es-AR"));

        // Mes y año (solo para el header)
        html = html.replace("{mesAño}", 
                LocalDate.now().format(monthYearFormatter));

        // Construir el HTML de preguntas y respuestas
        StringBuilder preguntasRespuestasHtml = new StringBuilder();
        
        if (perfilInversor.getRespuestas() != null && !perfilInversor.getRespuestas().isEmpty()) {
            // Ordenar las respuestas por el ID de la pregunta para mantener consistencia
            List<PerfilInversorRespuesta> respuestasOrdenadas = new ArrayList<>(perfilInversor.getRespuestas());
            respuestasOrdenadas.sort((r1, r2) -> {
                Long id1 = r1.getPerfilInversorPregunta() != null ? r1.getPerfilInversorPregunta().getId() : 0L;
                Long id2 = r2.getPerfilInversorPregunta() != null ? r2.getPerfilInversorPregunta().getId() : 0L;
                return id1.compareTo(id2);
            });

            for (PerfilInversorRespuesta respuesta : respuestasOrdenadas) {
                if (respuesta.getPerfilInversorPregunta() != null && 
                    respuesta.getPerfilInversorPreguntaOpcion() != null) {
                    
                    String pregunta = obtenerValor(respuesta.getPerfilInversorPregunta().getPregunta());
                    String respuestaTexto = obtenerValor(respuesta.getPerfilInversorPreguntaOpcion().getValor());
                    
                    preguntasRespuestasHtml.append("<div class=\"field\">");
                    preguntasRespuestasHtml.append("<label>").append(pregunta).append("</label>");
                    preguntasRespuestasHtml.append("<div class=\"respuesta-value\">").append(respuestaTexto).append("</div>");
                    preguntasRespuestasHtml.append("</div>");
                }
            }
        }

        html = html.replace("{preguntas_respuestas}", preguntasRespuestasHtml.toString());

        // Tipo de perfil
        String tipoPerfilTexto = "";
        if (perfilInversor.getTipo() != null) {
            tipoPerfilTexto = perfilInversor.getTipo().getDescripcion();
        }
        html = html.replace("{tipoPerfil}", obtenerValor(tipoPerfilTexto));

        return html;
    }

    private String obtenerValor(String valor) {
        return valor != null ? valor : "";
    }
}

