package com.cafeitsh.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cafeitsh.backend.entity.Rol;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias del servicio JWT: generación, validación y rechazo
 * de tokens manipulados o expirados. No requieren base de datos.
 */
class ServicioJwtTests {

    private static final String SECRETO = "clave-de-prueba-suficientemente-larga-0123456789";

    private ServicioJwt servicio(long expiracionMinutos) {
        return new ServicioJwt(SECRETO, expiracionMinutos);
    }

    @Test
    void tokenValidoDevuelveElCorreo() {
        String token = servicio(480).generarToken("cliente@correo.mx", Rol.CLIENTE);

        assertThat(servicio(480).esValido(token)).isTrue();
        assertThat(servicio(480).obtenerCorreo(token)).isEqualTo("cliente@correo.mx");
    }

    @Test
    void tokenConOtroSecretoEsRechazado() {
        String token = servicio(480).generarToken("cliente@correo.mx", Rol.CLIENTE);
        ServicioJwt otroServicio = new ServicioJwt(
                "otro-secreto-completamente-distinto-9876543210", 480);

        assertThat(otroServicio.esValido(token)).isFalse();
        assertThatThrownBy(() -> otroServicio.obtenerCorreo(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenExpiradoEsRechazado() {
        // Expiración negativa: el token venció antes de generarse.
        String token = servicio(-1).generarToken("cliente@correo.mx", Rol.CLIENTE);

        assertThat(servicio(-1).esValido(token)).isFalse();
    }

    @Test
    void tokenManipuladoEsRechazado() {
        String token = servicio(480).generarToken("cliente@correo.mx", Rol.CLIENTE);
        int ultimaParte = token.lastIndexOf('.');
        String manipulado = token.substring(0, ultimaParte - 1)
                + (token.charAt(ultimaParte - 1) == 'A' ? 'B' : 'A')
                + token.substring(ultimaParte);

        assertThat(servicio(480).esValido(manipulado)).isFalse();
    }

    @Test
    void textoBasuraNoEsToken() {
        assertThat(servicio(480).esValido("no-es-un-jwt")).isFalse();
        assertThat(servicio(480).esValido("")).isFalse();
    }

    @Test
    void secretoCortoSeRechazaAlIniciar() {
        assertThatThrownBy(() -> new ServicioJwt("corto", 480))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
