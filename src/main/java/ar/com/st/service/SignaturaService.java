package ar.com.st.service;

import ar.com.st.dto.firmaDigital.EstadoDocumentoDTO;
import java.io.OutputStream;

/**
 * Servicio para comunicación con el servicio externo de firma digital (Signatura)
 * @author Tomás Serra <tomas@serra.com.ar>
 */
public interface SignaturaService {
    
    /**
     * Crea un documento para firma digital
     * 
     * @param titulo Título del documento
     * @param emails Lista de emails de los firmantes
     * @param contenidoPdfBase64 Contenido del PDF codificado en Base64
     * @return ID del documento creado, o null si hubo error
     */
    String crearDocumento(String titulo, java.util.List<String> emails, String contenidoPdfBase64);
    
    /**
     * Obtiene el estado de un documento
     * 
     * @param id ID del documento
     * @return Estado del documento o null si no se encuentra
     */
    EstadoDocumentoDTO obtenerEstadoDocumento(String id);
    
    /**
     * Cancela un documento
     * 
     * @param id ID del documento
     * @param razon Razón de cancelación
     * @return true si se canceló exitosamente, false en caso contrario
     */
    boolean cancelarDocumento(String id, String razon);
    
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
     * @param id ID del documento
     * @param out OutputStream donde se escribirá el PDF
     */
    void obtenerDocumentoPdfFirmado(String id, OutputStream out);
    
    /**
     * Obtiene el certificado de firma del documento
     * 
     * @param id ID del documento
     * @param out OutputStream donde se escribirá el certificado
     */
    void obtenerCertificadoFirma(String id, OutputStream out);
}

