package ar.com.st.service;

import ar.com.st.dto.firmaDigital.EstadoDocumentoDTO;
import ar.com.st.entity.Solicitud;

import java.io.OutputStream;
import java.util.List;

/**
 * Servicio para manejo de firma digital de solicitudes
 * @author Tomás Serra <tomas@serra.com.ar>
 */
public interface FirmaDigitalService {
    
    /**
     * Envía el PDF de una solicitud para firma digital
     * 
     * @param solicitud La solicitud cuyo PDF se enviará para firma
     * @param emails Lista de emails de los firmantes
     * @return true si se envió exitosamente, false en caso contrario
     */
    boolean enviarFirmaDigital(Solicitud solicitud, List<String> emails);
    
    /**
     * Obtiene el estado de un documento de firma digital
     * 
     * @param idFirmaDigital ID del documento de firma digital
     * @return Estado del documento o null si no se encuentra
     */
    EstadoDocumentoDTO obtenerEstadoDocumento(String idFirmaDigital);
    
    /**
     * Cancela un documento de firma digital
     * 
     * @param idFirmaDigital ID del documento de firma digital
     * @param razon Razón de cancelación
     * @return true si se canceló exitosamente, false en caso contrario
     */
    boolean cancelarDocumento(String idFirmaDigital, String razon);
    
    /**
     * Reenvía el email de invitación para firmar
     * 
     * @param idFirma ID de la firma
     * @return true si se reenvió exitosamente, false en caso contrario
     */
    boolean reenviarEmailFirmaDigital(String idFirma);
    
    /**
     * Obtiene el PDF firmado del documento
     * 
     * @param idFirmaDigital ID del documento de firma digital
     * @param out OutputStream donde se escribirá el PDF
     */
    void obtenerDocumentoCertificado(String idFirmaDigital, OutputStream out);
    
    /**
     * Obtiene el certificado de firma del documento
     * 
     * @param idFirmaDigital ID del documento de firma digital
     * @param out OutputStream donde se escribirá el certificado
     */
    void obtenerCertificadoFirma(String idFirmaDigital, OutputStream out);
}

