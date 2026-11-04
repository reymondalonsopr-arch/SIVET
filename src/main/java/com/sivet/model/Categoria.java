
package com.sivet.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que representa las categorías de productos de SIVET.
 * Cada producto podrá pertenecer a una categoría.
 */
@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor
public class Categoria {

    // Identificador único de la categoría.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre obligatorio y único de la categoría.
    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    // Descripción opcional de la categoría.
    @Size(max = 200, message = "La descripción no puede superar los 200 caracteres")
    @Column(length = 200)
    private String descripcion;

    // Indica si la categoría está habilitada.
    @Column(nullable = false)
    private boolean activo = true;
}
