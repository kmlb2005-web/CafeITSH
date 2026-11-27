package com.cafeitsh.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de CORS del Sprint 5: los orígenes salen de la propiedad
 * {@code cafeitsh.cors.origenes-permitidos} y nunca se acepta un origen ajeno.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties =
        "cafeitsh.cors.origenes-permitidos=https://cafe.ejemplo.test,http://localhost:3000")
class ConfiguracionCorsTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void origenConfiguradoRecibeCabeceraDeAcceso() throws Exception {
        mockMvc.perform(options("/api/pedidos")
                        .header("Origin", "https://cafe.ejemplo.test")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://cafe.ejemplo.test"));
    }

    @Test
    void origenNoConfiguradoEsRechazado() throws Exception {
        mockMvc.perform(options("/api/pedidos")
                        .header("Origin", "https://sitio-malicioso.test")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void metodoNoPermitidoEsRechazadoAunConOrigenValido() throws Exception {
        mockMvc.perform(options("/api/pedidos")
                        .header("Origin", "https://cafe.ejemplo.test")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isForbidden());
    }
}
