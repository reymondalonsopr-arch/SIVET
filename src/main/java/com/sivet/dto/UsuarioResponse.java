package com.sivet.dto;

/**
 * DTO para devolver información pública de un usuario desde la API.
 */
public record UsuarioResponse(
        Long id,
        String nombreCompleto,
        String nombreUsuario,
        String nombreRol,
        boolean activo
) {
}
