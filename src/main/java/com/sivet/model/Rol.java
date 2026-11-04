package com.sivet.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que representa los roles del sistema.
 * Cada rol define el tipo de acceso que tendrá un usuario.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Rol {

    // Identificador único del rol, generado automáticamente.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre obligatorio y único del rol (ADMIN, VENDEDOR, etc.).
    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    // Descripción opcional de las funciones del rol.
    @Size(max = 150)
    @Column(length = 150)
    private String descripcion;
}