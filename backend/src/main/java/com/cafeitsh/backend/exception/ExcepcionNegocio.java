package com.cafeitsh.backend.exception;

import java.util.Map;

/**
 * Error de regla de negocio (correo duplicado, operación no permitida, etc.).
 * Se traduce a una respuesta HTTP apropiada por el manejador central.
 * Opcionalmente puede adjuntar detalle por campo (ej. categoriaId inválido).
 */
public class ExcepcionNegocio extends RuntimeException {

    private final int estadoHttp;
    private final Map<String, String> campos;

    public ExcepcionNegocio(String mensaje, int estadoHttp) {
        this(mensaje, estadoHttp, null);
    }

    public ExcepcionNegocio(String mensaje, int estadoHttp, Map<String, String> campos) {
        super(mensaje);
        this.estadoHttp = estadoHttp;
        this.campos = campos;
    }

    public int getEstadoHttp() {
        return estadoHttp;
    }

    public Map<String, String> getCampos() {
        return campos;
    }
}
