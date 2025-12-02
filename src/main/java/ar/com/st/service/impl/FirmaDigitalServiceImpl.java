package ar.com.st.service.impl;

import ar.com.st.dto.firmaDigital.EstadoDocumentoDTO;
import ar.com.st.entity.Empresa;
import ar.com.st.entity.Persona;
import ar.com.st.entity.Solicitud;
import ar.com.st.repository.SolicitudRepository;
import ar.com.st.service.FirmaDigitalService;
import ar.com.st.service.SignaturaService;
import ar.com.st.service.SolicitudPdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.util.Base64;
import java.util.List;

/**
 * Implementación del servicio para manejo de firma digital de solicitudes
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FirmaDigitalServiceImpl implements FirmaDigitalService {

    private final SolicitudPdfService solicitudPdfService;
    private final SignaturaService signaturaService;
    private final SolicitudRepository solicitudRepository;

    @Override
    @Transactional
    public boolean enviarFirmaDigital(Solicitud solicitud, List<String> emails) {
        try {
            if (emails == null || emails.isEmpty()) {
                log.warn("No se proporcionaron emails para enviar la firma digital");
                return false;
            }

            // Generar PDF de la solicitud
            byte[] pdfBytes = solicitudPdfService.generarPdf(solicitud);
            
            // Convertir PDF a Base64
            String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
            
            // Generar título según el tipo de solicitud
            String titulo;
            if (solicitud.getTipo() == Solicitud.Tipo.INDIVIDUO && solicitud.getTitular() instanceof Persona persona) {
                titulo = String.format("%s %s", persona.getNombres(), persona.getApellidos());
            } else if (solicitud.getTitular() instanceof Empresa empresa) {
                titulo = empresa.getDenominacion();
            } else {
                titulo = "Solicitud de Apertura de Cuenta - " + solicitud.getId();
            }

            // Crear documento en el servicio de firma digital
            String idFirmaDigital = signaturaService.crearDocumento(titulo, emails, pdfBase64);
            
            if (idFirmaDigital != null && !idFirmaDigital.isEmpty()) {
                // Actualizar la solicitud con el ID de firma digital
                solicitud.setIdFirmaDigital(idFirmaDigital);
                solicitud.setEstado(Solicitud.Estado.PENTIENTE_FIRMA);
                
                // Guardar la solicitud actualizada
                solicitudRepository.save(solicitud);
                
                log.info("Firma digital enviada exitosamente para la solicitud {}. ID de firma digital: {}", 
                        solicitud.getId(), idFirmaDigital);
                return true;
            } else {
                log.error("No se pudo obtener el ID de firma digital para la solicitud {}", solicitud.getId());
                return false;
            }

        } catch (Exception ex) {
            log.error("Error al enviar el PDF para firma digital de la solicitud {}: {}", 
                    solicitud.getId(), ex.getMessage(), ex);
            return false;
        }
    }

    @Override
    public EstadoDocumentoDTO obtenerEstadoDocumento(String idFirmaDigital) {
        return signaturaService.obtenerEstadoDocumento(idFirmaDigital);
    }

    @Override
    public boolean cancelarDocumento(String idFirmaDigital, String razon) {
        return signaturaService.cancelarDocumento(idFirmaDigital, razon);
    }

    @Override
    public boolean reenviarEmailFirmaDigital(String idFirma) {
        return signaturaService.reenviarEmailFirmaDigital(idFirma);
    }

    @Override
    public void obtenerDocumentoCertificado(String idFirmaDigital, OutputStream out) {
        signaturaService.obtenerDocumentoPdfFirmado(idFirmaDigital, out);
    }

    @Override
    public void obtenerCertificadoFirma(String idFirmaDigital, OutputStream out) {
        signaturaService.obtenerCertificadoFirma(idFirmaDigital, out);
    }
}

