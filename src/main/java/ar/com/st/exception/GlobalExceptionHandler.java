package ar.com.st.exception;

import ar.com.st.dto.auth.MessageResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.PropertyValueException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.persistence.EntityNotFoundException;

import java.util.stream.Collectors;

/**
 * Manejador global de excepciones
 * @author Tomás Serra <tomas@serra.com.ar>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("Error de validación: {}", ex.getMessage());
        
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        return ResponseEntity.badRequest()
                .body(new MessageResponseDTO("Error de validación: " + errorMessage));
    }

    @ExceptionHandler(PropertyValueException.class)
    public ResponseEntity<MessageResponseDTO> handlePropertyValueException(PropertyValueException ex) {
        log.error("Error de propiedad de entidad: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponseDTO("Error interno del servidor. Por favor, contacte al administrador."));
    }

    @ExceptionHandler(ClassCastException.class)
    public ResponseEntity<MessageResponseDTO> handleClassCastException(ClassCastException ex) {
        log.error("Error de casting de tipo: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponseDTO("Error interno del servidor. Por favor, contacte al administrador."));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<MessageResponseDTO> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.error("Entidad no encontrada: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponseDTO("Recurso no encontrado."));
    }

    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<MessageResponseDTO> handleHttpMessageNotWritableException(HttpMessageNotWritableException ex) {
        log.error("Error al escribir respuesta JSON: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponseDTO("Error interno del servidor. Por favor, contacte al administrador."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<MessageResponseDTO> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("Error de tipo de argumento: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponseDTO("Parámetro inválido: " + (ex.getName() != null ? ex.getName() : "desconocido")));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<MessageResponseDTO> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.warn("Método HTTP no soportado: {} para {}", ex.getMethod(), ex.getSupportedHttpMethods());
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new MessageResponseDTO("Método HTTP no permitido: " + ex.getMethod()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponseDTO> handleGenericException(Exception ex) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponseDTO("Error interno del servidor. Por favor, contacte al administrador."));
    }
}
