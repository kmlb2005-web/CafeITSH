package com.cafeitsh.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/**
 * Respuesta de error uniforme para toda la API.
 * {@code campos} solo se llena en errores de validación; nunca se exponen
 * stack traces ni detalles internos de la base de datos.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaError(String mensaje, Instant timestamp, Map<String, String> campos) {

    public static RespuestaError simple(String mensaje) {
        return new RespuestaError(mensaje, Instant.now(), null);
    }

    public static RespuestaError conCampos(String mensaje, Map<String, String> campos) {
        return new RespuestaError(mensaje, Instant.now(), campos);
    }
}
