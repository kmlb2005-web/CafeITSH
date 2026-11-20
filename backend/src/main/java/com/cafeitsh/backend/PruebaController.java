package com.cafeitsh.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PruebaController {

    @GetMapping("/api/prueba")
    public String prueba() {
        return "¡El backend de cafeITSH funciona correctamente!";
    }
}
