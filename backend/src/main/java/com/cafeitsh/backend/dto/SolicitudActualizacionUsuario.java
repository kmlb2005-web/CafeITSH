package com.cafeitsh.backend.dto;

import com.cafeitsh.backend.entity.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para actualizar un usuario desde administración.
 * La contraseña no se modifica desde aquí (operación no incluida en este sprint).
 */
public record SolicitudActualizacionUsuario(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 255, message = "El correo no puede exceder 255 caracteres")
        String correo,

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {
}
