package ar.com.st.service;

import ar.com.st.entity.Solicitud;

/**
 * Servicio para generación de PDFs de solicitudes
 * 
 * @author Tomás Serra <tomas@serra.com.ar>
 */
public interface SolicitudPdfService {

    /**
     * Genera el PDF completo de una solicitud
     * 
     * @param solicitud La solicitud para la cual generar el PDF
     * @return Array de bytes con el contenido del PDF
     */
    byte[] generarPdf(Solicitud solicitud);
}

