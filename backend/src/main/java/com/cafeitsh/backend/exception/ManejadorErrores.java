package com.cafeitsh.backend.exception;

import com.cafeitsh.backend.dto.RespuestaError;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejo centralizado de errores: todas las respuestas de error salen en
 * español, con el estado HTTP adecuado y sin exponer stack traces ni
 * detalles internos de la base de datos.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger bitacora = LoggerFactory.getLogger(ManejadorErrores.class);

    /** Errores de validación de DTOs (Bean Validation). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> validar(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return responder(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos", campos);
    }

    /** Parámetro de ruta o query con tipo inválido (?categoria=abc). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespuestaError> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos",
                Map.of(ex.getName(), "Valor inválido"));
    }

    /** Reglas de negocio definidas en las especificaciones. */
    @ExceptionHandler(ExcepcionNegocio.class)
    public ResponseEntity<RespuestaError> negocio(ExcepcionNegocio ex) {
        return responder(HttpStatus.valueOf(ex.getEstadoHttp()), ex.getMessage(),
                ex.getCampos());
    }

    /** Credenciales incorrectas en el inicio de sesión. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<RespuestaError> credenciales(BadCredentialsException ex) {
        return responder(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos", null);
    }

    /** Cuenta desactivada: mismo mensaje genérico para no filtrar información. */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<RespuestaError> deshabilitado(DisabledException ex) {
        return responder(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos", null);
    }

    /** Token ausente o inválido cuando se exige autenticación. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespuestaError> denegado(AccessDeniedException ex) {
        return responder(HttpStatus.FORBIDDEN,
                "No tienes permiso para realizar esta operación", null);
    }

    /** Ruta inexistente: sin este manejador, el genérico la devolvería como 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<RespuestaError> recursoInexistente(NoResourceFoundException ex) {
        return responder(HttpStatus.NOT_FOUND, "Recurso no encontrado", null);
    }

    /** Última línea: cualquier error no previsto. El detalle solo va al log. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> inesperado(Exception ex) {
        bitacora.error("Error interno no controlado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor", null);
    }

    private ResponseEntity<RespuestaError> responder(
            HttpStatus estado, String mensaje, Map<String, String> campos) {
        RespuestaError cuerpo = campos == null
                ? RespuestaError.simple(mensaje)
                : RespuestaError.conCampos(mensaje, campos);
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
