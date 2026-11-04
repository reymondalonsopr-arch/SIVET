package com.sivet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO para recibir los datos necesarios para registrar un usuario.
 * La contraseña se recibe aquí, pero no se devuelve en la respuesta.
 */
public record UsuarioCrearRequest(

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 120, message = "El nombre completo no puede superar los 120 caracteres")
        String nombreCompleto,

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(max = 50, message = "El nombre de usuario no puede superar los 50 caracteres")
        String nombreUsuario,

        @NotBlank(message = "La contraseña es obligatoria")
        String password,

        @NotNull(message = "El rol es obligatorio")
        @Positive(message = "El identificador del rol debe ser positivo")
        Long rolId

) {
}
