
package com.sivet.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que representa a los clientes de SIVET.
 *
 * Almacena la información básica de las personas
 * o empresas que realizan compras en el negocio.
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    // Identificador único del cliente.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre completo o razón social del cliente.
    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nombre;

    // Número de documento de identidad.
    @Size(max = 20)
    @Column(name = "numero_documento", length = 20, unique = true)
    private String numeroDocumento;

    // Número telefónico de contacto.
    @Size(max = 20)
    @Column(length = 20)
    private String telefono;

    // Correo electrónico del cliente.
    @Email(message = "El correo electrónico no es válido")
    @Size(max = 120)
    @Column(length = 120)
    private String correo;

    // Dirección del cliente.
    @Size(max = 200)
    @Column(length = 200)
    private String direccion;

    // Indica si el cliente está habilitado.
    @Column(nullable = false)
    private boolean activo = true;
}
