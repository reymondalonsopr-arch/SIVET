package com.sivet.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que representa a los usuarios del sistema SIVET.
 * Cada usuario tiene un rol que determina sus permisos.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    // Identificador único del usuario.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre y apellidos del usuario.
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 120)
    @Column(name = "nombre_completo", nullable = false, length = 120)
    private String nombreCompleto;

    // Nombre de usuario utilizado para iniciar sesión.
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 50)
    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 50)
    private String nombreUsuario;

    // Contraseña cifrada mediante BCrypt.
    // Nunca se debe almacenar la contraseña original.
    @NotBlank(message = "La contraseña es obligatoria")
    @Column(nullable = false)
    private String password;

    // Relación: muchos usuarios pueden compartir un mismo rol.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    // Indica si la cuenta del usuario está habilitada.
    @Column(nullable = false)
    private boolean activo = true;
}